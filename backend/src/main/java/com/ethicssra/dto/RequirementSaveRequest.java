package com.ethicssra.dto;

public record RequirementSaveRequest(
        String title,
        String textContent,
        String embeddedModules,
        Double matchingScore,
        String diagramJson,
        String specWizardJson,
        Long courseId,
        Long assignmentId
) {
}
