package com.ethicssra.dto;

import java.util.List;
import java.util.Map;

public record StudentProfileDto(
    Long studentId,
    String studentName,
    Double avgScore,
    Double ethicsScore,
    Double ethicsQualityScore,
    List<CommonMistakeDto> commonMistakes,
    Map<String, Double> abilities,
    Integer totalSubmissions,
    Integer submittedCount
) {}