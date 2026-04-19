package com.ethicssra.dto;

import jakarta.validation.constraints.NotNull;

public record EmbedFeedbackRequestDto(
        @NotNull Long requirementId
) {
}
