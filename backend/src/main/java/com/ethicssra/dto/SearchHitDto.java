package com.ethicssra.dto;

public record SearchHitDto(
        String type,
        Long id,
        String title,
        String snippet
) {
}
