package com.ethicssra.dto;

public record TaskRequirementProgressDto(
        Long requirementId,
        String title,
        String status,
        int referenceCount,
        int ethicsCount
) {
}
