package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;

public record EthicsModuleUpsertRequest(
        @NotBlank String title,
        String category,
        String keywords,
        String description,
        String caseText,
        String reference
) {
}
