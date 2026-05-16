package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;

public record EthicsModuleUpsertRequest(
        @NotBlank String title,
        String category,
        String keywords,
        String description,
        String caseText,
        String reference,
        String applicableScenario,
        String teachingObjective,
        String valuePoint,
        String discussionQuestions,
        String riskPoints,
        String integrationSuggestion,
        String applicableMajor,
        String difficultyLevel
) {
}
