package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDiscussionPostRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20000) String content,
        String category
) {
}
