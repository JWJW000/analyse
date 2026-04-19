package com.ethicssra.dto;

import java.util.List;

public record AiGradingDto(
    Long submissionId,
    Double suggestedScore,
    String intelligentComment,
    List<String> improvementSuggestions,
    List<String> strengths,
    Double ethicsIntegrationScore,
    Double completenessScore,
    Double innovationScore,
    String overallFeedback
) {}
