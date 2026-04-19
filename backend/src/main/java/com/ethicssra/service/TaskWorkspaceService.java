package com.ethicssra.service;

import com.ethicssra.domain.Assignment;
import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.Role;
import com.ethicssra.dto.TaskRequirementProgressDto;
import com.ethicssra.dto.TaskWorkspaceDto;
import com.ethicssra.repository.AssignmentRepository;
import com.ethicssra.repository.EnrollmentRepository;
import com.ethicssra.repository.RequirementReferenceLinkRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class TaskWorkspaceService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private final AssignmentRepository assignmentRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementReferenceLinkRepository referenceLinkRepository;
    private final EnrollmentRepository enrollmentRepository;

    public TaskWorkspaceService(
            AssignmentRepository assignmentRepository,
            RequirementRepository requirementRepository,
            RequirementReferenceLinkRepository referenceLinkRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.requirementRepository = requirementRepository;
        this.referenceLinkRepository = referenceLinkRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public TaskWorkspaceDto getWorkspace(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("课程任务不存在"));
        assertTaskAccessible(assignment);

        List<Requirement> scopedRequirements = scopedRequirements(assignmentId);
        List<TaskRequirementProgressDto> progressRows = new ArrayList<>();
        List<RequirementSnapshot> snapshots = new ArrayList<>();

        for (Requirement requirement : scopedRequirements) {
            int refCount = referenceLinkRepository.countByRequirementId(requirement.getId());
            int ethicsCount = countEmbeddedModules(requirement.getEmbeddedModules());
            progressRows.add(new TaskRequirementProgressDto(
                    requirement.getId(),
                    requirement.getTitle(),
                    requirement.getStatus() != null ? requirement.getStatus().name() : null,
                    refCount,
                    ethicsCount
            ));
            snapshots.add(new RequirementSnapshot(
                    requirement.getId(),
                    requirement.getTextContent(),
                    refCount,
                    ethicsCount
            ));
        }

        List<String> blockingIssues = collectBlockingIssues(snapshots);
        int totalReferenceLinks = referenceLinkRepository.countByRequirementIds(
                scopedRequirements.stream().map(Requirement::getId).collect(java.util.stream.Collectors.toSet())
        );
        int totalEthicsLinks = snapshots.stream().mapToInt(RequirementSnapshot::ethicsCount).sum();

        return new TaskWorkspaceDto(
                assignment.getId(),
                assignment.getCourseId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt() != null ? FMT.format(assignment.getDueAt()) : null,
                scopedRequirements.size(),
                totalReferenceLinks,
                totalEthicsLinks,
                !scopedRequirements.isEmpty() && blockingIssues.isEmpty(),
                blockingIssues,
                progressRows
        );
    }

    private List<Requirement> scopedRequirements(Long assignmentId) {
        Role role = SecurityUtils.currentUser().role();
        if (role == Role.STUDENT) {
            return requirementRepository.findByAssignmentIdAndUserIdOrderByUpdatedAtDesc(assignmentId, SecurityUtils.currentUserId());
        }
        return requirementRepository.findByAssignmentId(assignmentId);
    }

    private void assertTaskAccessible(Assignment assignment) {
        Role role = SecurityUtils.currentUser().role();
        if (role == Role.ADMIN) {
            return;
        }
        if (role == Role.TEACHER && assignment.getCreatedBy().equals(SecurityUtils.currentUserId())) {
            return;
        }
        if (role == Role.STUDENT && enrollmentRepository
                .findByCourseIdAndStudentId(assignment.getCourseId(), SecurityUtils.currentUserId())
                .isPresent()) {
            return;
        }
        throw new IllegalArgumentException("无权访问该课程任务");
    }

    static int countEmbeddedModules(String embeddedModules) {
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

    static List<String> collectBlockingIssues(List<RequirementSnapshot> snapshots) {
        List<String> out = new ArrayList<>();
        for (RequirementSnapshot snapshot : snapshots) {
            if (snapshot.textContent == null || snapshot.textContent.isBlank()) {
                out.add("需求 #" + snapshot.requirementId + " 缺少正文内容");
            }
            if (snapshot.referenceCount <= 0) {
                out.add("需求 #" + snapshot.requirementId + " 缺少文献证据映射");
            }
            if (snapshot.ethicsCount <= 0) {
                out.add("需求 #" + snapshot.requirementId + " 缺少伦理模块映射");
            }
        }
        return out;
    }

    static final class RequirementSnapshot {
        private final Long requirementId;
        private final String textContent;
        private final int referenceCount;
        private final int ethicsCount;

        RequirementSnapshot(Long requirementId, String textContent, int referenceCount, int ethicsCount) {
            this.requirementId = requirementId;
            this.textContent = textContent;
            this.referenceCount = referenceCount;
            this.ethicsCount = ethicsCount;
        }

        int ethicsCount() {
            return ethicsCount;
        }
    }
}
