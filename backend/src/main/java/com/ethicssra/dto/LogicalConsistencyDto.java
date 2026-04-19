package com.ethicssra.dto;

import java.util.List;

public record LogicalConsistencyDto(
        boolean allSatisfied,
        String summary,
        List<FormalConsistencyItemDto> items
) {
}
