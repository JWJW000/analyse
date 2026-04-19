package com.ethicssra.service;

import com.ethicssra.domain.Course;
import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.RequirementStatus;
import com.ethicssra.domain.Role;
import com.ethicssra.domain.Submission;
import com.ethicssra.domain.SubmissionStatus;
import com.ethicssra.dto.RequirementDto;
import com.ethicssra.dto.RequirementSaveRequest;
import com.ethicssra.repository.CourseRepository;
import com.ethicssra.repository.RequirementReferenceLinkRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.repository.SubmissionRepository;
import com.ethicssra.security.SecurityUserDetails;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RequirementService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private final RequirementRepository requirementRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final RequirementReferenceLinkRepository requirementReferenceLinkRepository;
    private final AuditService auditService;

    public RequirementService(
            RequirementRepository requirementRepository,
            SubmissionRepository submissionRepository,
            CourseRepository courseRepository,
            RequirementReferenceLinkRepository requirementReferenceLinkRepository,
            AuditService auditService
    ) {
        this.requirementRepository = requirementRepository;
        this.submissionRepository = submissionRepository;
        this.courseRepository = courseRepository;
        this.requirementReferenceLinkRepository = requirementReferenceLinkRepository;
        this.auditService = auditService;
    }

    public List<RequirementDto> myList() {
        Long uid = SecurityUtils.currentUserId();
        return requirementRepository.findByUserIdOrderByUpdatedAtDesc(uid).stream()
                .map(this::toDto)
                .toList();
    }

    public RequirementDto get(Long id) {
        Requirement r = requirementRepository.findById(id).orElseThrow();
        assertCanAccess(r);
        return toDto(r);
    }

    @Transactional
    public RequirementDto save(RequirementSaveRequest req) {
        Long uid = SecurityUtils.currentUserId();
        Requirement r = new Requirement();
        r.setUserId(uid);
        r.setTitle(req.title());
        r.setTextContent(req.textContent());
        r.setEmbeddedModules(req.embeddedModules());
        r.setMatchingScore(req.matchingScore());
        r.setDiagramJson(req.diagramJson());
        r.setSpecWizardJson(req.specWizardJson());
        r.setCourseId(req.courseId());
        r.setAssignmentId(req.assignmentId());
        r.setStatus(RequirementStatus.DRAFT);
        r = requirementRepository.save(r);
        auditService.log(uid, "REQUIREMENT_SAVE", "Requirement", r.getId(), null);
        if (req.assignmentId() != null) {
            upsertSubmissionDraft(req.assignmentId(), uid, r.getId());
        }
        return toDto(r);
    }

    @Transactional
    public RequirementDto update(Long id, RequirementSaveRequest req) {
        Requirement r = requirementRepository.findById(id).orElseThrow();
        if (!r.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new IllegalArgumentException("无权修改");
        }
        r.setTitle(req.title());
        r.setTextContent(req.textContent());
        r.setEmbeddedModules(req.embeddedModules());
        r.setMatchingScore(req.matchingScore());
        r.setDiagramJson(req.diagramJson());
        r.setSpecWizardJson(req.specWizardJson());
        r.setCourseId(req.courseId());
        r.setAssignmentId(req.assignmentId());
        r = requirementRepository.save(r);
        if (req.assignmentId() != null) {
            upsertSubmissionDraft(req.assignmentId(), r.getUserId(), r.getId());
        }
        auditService.log(r.getUserId(), "REQUIREMENT_UPDATE", "Requirement", r.getId(), null);
        return toDto(r);
    }

    @Transactional
    public RequirementDto review(Long id, RequirementStatus status, String comment) {
        SecurityUserDetails u = SecurityUtils.currentUser();
        if (u.role() != Role.TEACHER && u.role() != Role.ADMIN) {
            throw new IllegalArgumentException("无权审核");
        }
        Requirement r = requirementRepository.findById(id).orElseThrow();
        if (u.role() == Role.TEACHER) {
            assertTeacherOwnsRequirementCourse(r);
        }
        r.setStatus(status);
        r.setTeacherComment(comment);
        r = requirementRepository.save(r);
        submissionRepository.findFirstByRequirementId(r.getId()).ifPresent(sub -> {
            sub.setStatus(mapSubmissionStatus(status));
            sub.setTeacherComment(comment);
            sub.setReviewedAt(Instant.now());
            submissionRepository.save(sub);
        });
        auditService.log(u.id(), "REQUIREMENT_REVIEW", "Requirement", r.getId(), status.name());
        return toDto(r);
    }

    @Transactional
    public List<RequirementDto> batchReview(
            List<Long> ids,
            RequirementStatus status,
            String globalComment,
            Map<String, String> commentsByRequirementId
    ) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("请选择至少一条需求");
        }
        Map<Long, String> perId = parsePerRequirementComments(commentsByRequirementId);
        List<RequirementDto> out = new ArrayList<>();
        for (Long id : ids) {
            String c = resolveBatchComment(id, globalComment, perId);
            out.add(review(id, status, c));
        }
        return out;
    }

    private static Map<Long, String> parsePerRequirementComments(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> m = new HashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            try {
                m.put(Long.parseLong(e.getKey().trim()), e.getValue());
            } catch (NumberFormatException ignored) {
            }
        }
        return m;
    }

    /** 若存在逐条批注且非空白，则用之；否则用全局批注（可为 null）。 */
    private static String resolveBatchComment(Long requirementId, String globalComment, Map<Long, String> perId) {
        if (perId.isEmpty()) {
            return globalComment;
        }
        String specific = perId.get(requirementId);
        if (specific == null) {
            return globalComment;
        }
        String t = specific.trim();
        return t.isEmpty() ? globalComment : t;
    }

    private void assertTeacherOwnsRequirementCourse(Requirement r) {
        if (r.getCourseId() == null) {
            throw new IllegalArgumentException("需求未关联课程，无法审核");
        }
        Course c = courseRepository.findById(r.getCourseId()).orElseThrow();
        if (!c.getTeacherId().equals(SecurityUtils.currentUserId())) {
            throw new IllegalArgumentException("无权审核该需求");
        }
    }

    private void upsertSubmissionDraft(Long assignmentId, Long studentId, Long requirementId) {
        var opt = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
        Submission s;
        if (opt.isPresent()) {
            s = opt.get();
            s.setRequirementId(requirementId);
            s.setStatus(SubmissionStatus.DRAFT);
        } else {
            s = new Submission();
            s.setAssignmentId(assignmentId);
            s.setStudentId(studentId);
            s.setRequirementId(requirementId);
            s.setStatus(SubmissionStatus.DRAFT);
        }
        submissionRepository.save(s);
    }

    private SubmissionStatus mapSubmissionStatus(RequirementStatus st) {
        return switch (st) {
            case APPROVED -> SubmissionStatus.APPROVED;
            case REJECTED -> SubmissionStatus.REJECTED;
            case SUBMITTED -> SubmissionStatus.SUBMITTED;
            default -> SubmissionStatus.DRAFT;
        };
    }

    @Transactional
    public void submit(Long id) {
        Requirement r = requirementRepository.findById(id).orElseThrow();
        if (!r.getUserId().equals(SecurityUtils.currentUserId())) {
            throw new IllegalArgumentException("无权提交");
        }
        if (r.getAssignmentId() == null) {
            throw new IllegalArgumentException("请先关联课程任务后再提交");
        }
        if (r.getTextContent() == null || r.getTextContent().isBlank()) {
            throw new IllegalArgumentException("需求正文为空，无法提交");
        }
        int referenceCount = requirementReferenceLinkRepository.countByRequirementId(r.getId());
        if (referenceCount <= 0) {
            throw new IllegalArgumentException("请至少关联 1 条文献证据后再提交");
        }
        if (countEmbeddedModules(r.getEmbeddedModules()) <= 0) {
            throw new IllegalArgumentException("请至少关联 1 个伦理思政模块后再提交");
        }
        r.setStatus(RequirementStatus.SUBMITTED);
        requirementRepository.save(r);
        submissionRepository.findByAssignmentIdAndStudentId(r.getAssignmentId(), r.getUserId())
                .ifPresent(sub -> {
                    sub.setStatus(SubmissionStatus.SUBMITTED);
                    sub.setSubmittedAt(Instant.now());
                    submissionRepository.save(sub);
                });
        auditService.log(r.getUserId(), "REQUIREMENT_SUBMIT", "Requirement", r.getId(), null);
    }

    private static int countEmbeddedModules(String embeddedModules) {
        if (embeddedModules == null || embeddedModules.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String part : embeddedModules.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                Long.parseLong(s);
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        return count;
    }

    private void assertCanAccess(Requirement r) {
        SecurityUserDetails u = SecurityUtils.currentUser();
        if (u.role() == Role.ADMIN) {
            return;
        }
        if (r.getUserId().equals(u.id())) {
            return;
        }
        if (u.role() == Role.TEACHER && r.getCourseId() != null) {
            var course = courseRepository.findById(r.getCourseId()).orElseThrow();
            if (course.getTeacherId().equals(u.id())) {
                return;
            }
        }
        throw new IllegalArgumentException("无权访问");
    }

    private RequirementDto toDto(Requirement r) {
        return new RequirementDto(
                r.getId(),
                r.getUserId(),
                r.getTitle(),
                r.getTextContent(),
                r.getEmbeddedModules(),
                r.getMatchingScore(),
                r.getDiagramJson(),
                r.getSpecWizardJson(),
                r.getCourseId(),
                r.getAssignmentId(),
                r.getStatus(),
                r.getTeacherComment(),
                r.getCreatedAt() != null ? FMT.format(r.getCreatedAt()) : null,
                r.getUpdatedAt() != null ? FMT.format(r.getUpdatedAt()) : null
        );
    }
}
