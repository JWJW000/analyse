package com.ethicssra.dto;

import java.util.List;

public record SearchResponseDto(
        String query,
        List<SearchHitDto> hits
) {
}
