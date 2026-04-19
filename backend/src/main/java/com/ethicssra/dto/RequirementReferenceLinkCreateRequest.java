package com.ethicssra.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RequirementReferenceLinkCreateRequest(
        @NotNull Long referenceId,
        @NotBlank String evidenceNote,
        @Min(0) @Max(1) Double confidence
) {
}
