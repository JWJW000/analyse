package com.ethicssra.dto;

public record ModuleMatchDto(
        Long moduleId,
        double score,
        String title,
        String snippet
) {
}
