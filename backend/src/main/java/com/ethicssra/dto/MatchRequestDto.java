package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;

public record MatchRequestDto(
        @NotBlank String requirementText,
        Long requirementId,
        Integer topK
) {
}
