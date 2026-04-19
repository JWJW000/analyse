package com.ethicssra.dto;

public record AssignmentDto(
        Long id,
        Long courseId,
        String title,
        String description,
        String dueAt,
        Long createdBy,
        String createdAt
) {
}
