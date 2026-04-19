package com.ethicssra.service;

import com.ethicssra.domain.Course;
import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.Role;
import com.ethicssra.dto.RequirementReferenceLinkCreateRequest;
import com.ethicssra.dto.RequirementReferenceLinkDto;
import com.ethicssra.repository.CourseRepository;
import com.ethicssra.repository.LiteratureRepository;
import com.ethicssra.repository.RequirementReferenceLinkRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequirementReferenceLinkService {

    private final RequirementRepository requirementRepository;
    private final LiteratureRepository literatureRepository;
    private final RequirementReferenceLinkRepository linkRepository;
    private final CourseRepository courseRepository;
    private final AuditService auditService;

    public RequirementReferenceLinkService(
            RequirementRepository requirementRepository,
            LiteratureRepository literatureRepository,
            RequirementReferenceLinkRepository linkRepository,
            CourseRepository courseRepository,
            AuditService auditService
    ) {
        this.requirementRepository = requirementRepository;
        this.literatureRepository = literatureRepository;
        this.linkRepository = linkRepository;
        this.courseRepository = courseRepository;
        this.auditService = auditService;
    }

    public List<RequirementReferenceLinkDto> list(Long requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new IllegalArgumentException("需求不存在"));
        assertCanView(requirement);
        return linkRepository.findByRequirementId(requirementId);
    }

    @Transactional
    public RequirementReferenceLinkDto create(Long requirementId, RequirementReferenceLinkCreateRequest req) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new IllegalArgumentException("需求不存在"));
        assertCanEdit(requirement);
        literatureRepository.findById(req.referenceId())
                .orElseThrow(() -> new IllegalArgumentException("文献不存在"));
        RequirementReferenceLinkDto dto = linkRepository.create(
                requirementId,
                req.referenceId(),
                req.evidenceNote().trim(),
                req.confidence()
        );
        auditService.log(SecurityUtils.currentUserId(), "REQUIREMENT_REFERENCE_LINK_CREATE", "Requirement", requirementId,
                java.util.Map.of("referenceId", req.referenceId()));
        return dto;
    }

    @Transactional
    public void delete(Long linkId) {
        RequirementReferenceLinkDto link = linkRepository.getById(linkId);
        Requirement requirement = requirementRepository.findById(link.requirementId())
                .orElseThrow(() -> new IllegalArgumentException("需求不存在"));
        assertCanEdit(requirement);
        linkRepository.deleteById(linkId);
        auditService.log(SecurityUtils.currentUserId(), "REQUIREMENT_REFERENCE_LINK_DELETE", "Requirement", link.requirementId(),
                java.util.Map.of("linkId", linkId));
    }

    private void assertCanView(Requirement requirement) {
        Role role = SecurityUtils.currentUser().role();
        if (role == Role.ADMIN) {
            return;
        }
        if (requirement.getUserId().equals(SecurityUtils.currentUserId())) {
            return;
        }
        if (role == Role.TEACHER && requirement.getCourseId() != null) {
            Course course = courseRepository.findById(requirement.getCourseId())
                    .orElseThrow(() -> new IllegalArgumentException("课程不存在"));
            if (course.getTeacherId().equals(SecurityUtils.currentUserId())) {
                return;
            }
        }
        throw new IllegalArgumentException("无权访问该需求");
    }

    private void assertCanEdit(Requirement requirement) {
        Role role = SecurityUtils.currentUser().role();
        if (role == Role.ADMIN) {
            return;
        }
        if (requirement.getUserId().equals(SecurityUtils.currentUserId())) {
            return;
        }
        throw new IllegalArgumentException("无权编辑该需求");
    }
}
