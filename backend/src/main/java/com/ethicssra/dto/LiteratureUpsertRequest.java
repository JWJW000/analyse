package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;

public record LiteratureUpsertRequest(
        @NotBlank String title,
        String author,
        String source,
        String abstractText,
        String keywords
) {
}
