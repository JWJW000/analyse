package com.ethicssra.dto;

import java.util.List;

public record IntegrityCheckDto(
        int score,
        String summary,
        List<CheckItem> items
) {
    public record CheckItem(String id, boolean ok, String label, String hint) {}
}
