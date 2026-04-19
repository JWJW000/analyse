package com.ethicssra.dto;

public record AiGeneratedContentDto(
    String type,
    String title,
    String description,
    String priority
) {}