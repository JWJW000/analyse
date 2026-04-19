package com.ethicssra.dto;

public record EthicsModuleDto(
        Long id,
        String title,
        String category,
        String keywords,
        String description,
        String caseText,
        String reference,
        Integer currentVersion
) {
}
