package com.ethicssra.dto;

public record EmbedFeedbackItemDto(
        Long moduleId,
        double score,
        boolean weak,
        String title,
        String hint
) {
}
