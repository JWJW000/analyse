package com.ethicssra.dto;

public record RequirementReferenceLinkDto(
        Long id,
        Long requirementId,
        Long referenceId,
        String evidenceNote,
        Double confidence,
        String createdAt
) {
}
