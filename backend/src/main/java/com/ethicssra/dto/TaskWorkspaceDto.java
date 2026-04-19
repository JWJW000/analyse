package com.ethicssra.dto;

import java.util.List;

public record TaskWorkspaceDto(
        Long assignmentId,
        Long courseId,
        String assignmentTitle,
        String assignmentDescription,
        String dueAt,
        int requirementCount,
        int referenceLinkCount,
        int ethicsLinkCount,
        boolean readyForSubmission,
        List<String> blockingIssues,
        List<TaskRequirementProgressDto> requirements
) {
}
