package com.ethicssra.dto;

import java.util.List;

public record EmbedFeedbackDto(
        List<EmbedFeedbackItemDto> items,
        List<ModuleMatchDto> suggestedAlternatives,
        String summary
) {
}
