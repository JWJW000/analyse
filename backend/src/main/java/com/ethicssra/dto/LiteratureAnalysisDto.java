package com.ethicssra.dto;

import java.util.List;

public record LiteratureAnalysisDto(
    String title,
    String author,
    String abstractText,
    List<String> keywords,
    String researchDirection,
    String summary
) {}