package com.ethicssra.dto;

import java.util.List;

public record DraftSpecDto(
        List<String> keywords,
        String background,
        String goals,
        String ethics
) {
}
