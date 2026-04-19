package com.ethicssra.dto;

public record DocumentAnalysisDto(
        LanguageAnalysisDto language,
        LogicalConsistencyDto logicalConsistency
) {
}
