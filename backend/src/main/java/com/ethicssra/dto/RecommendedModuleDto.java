package com.ethicssra.dto;

public record RecommendedModuleDto(
        Long moduleId,
        String title,
        String snippet,
        /** CONTENT_SIMILARITY 基于最近需求正文；POPULAR 基于全站匹配记录频次 */
        String reason,
        Double score
) {
}
