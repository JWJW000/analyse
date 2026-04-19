package com.ethicssra.dto;

public record CourseDto(
        Long id,
        String name,
        String code,
        Long teacherId,
        String createdAt
) {
}
