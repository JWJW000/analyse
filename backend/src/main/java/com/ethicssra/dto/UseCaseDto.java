package com.ethicssra.dto;

public record UseCaseDto(
    String name,
    String actor,
    String preconditions,
    String mainFlow,
    String postconditions
) {}