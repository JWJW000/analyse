package com.ethicssra.dto;

import com.ethicssra.domain.RequirementStatus;

public record RequirementDto(
        Long id,
        Long userId,
        String title,
        String textContent,
        String embeddedModules,
        Double matchingScore,
        String diagramJson,
        String specWizardJson,
        Long courseId,
        Long assignmentId,
        RequirementStatus status,
        String teacherComment,
        String createdAt,
        String updatedAt
) {
}
