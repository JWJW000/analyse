package com.ethicssra.dto;

public record CommonMistakeDto(
    String type,
    String description,
    String suggestion,
    Integer count
) {}