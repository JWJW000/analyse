package com.ethicssra.dto;

public record EthicsModuleDto(
        Long id,
        String title,
        String category,
        String keywords,
        String description,
        String caseText,
        String reference,
        Integer currentVersion,
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
