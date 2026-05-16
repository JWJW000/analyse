package com.ethicssra.dto;

public record LiteratureDto(
        Long id,
        String title,
        String author,
        String source,
        String abstractText,
        String keywords,
        String filePath,
        Long createdBy,
        Integer publicationYear,
        String doi,
        String url,
        String literatureType,
        String researchMethod,
        String applicableTopic,
        String keyFindings,
        String evidenceValue
) {
}
