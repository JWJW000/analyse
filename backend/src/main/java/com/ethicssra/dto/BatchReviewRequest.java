package com.ethicssra.dto;

import java.util.List;
import java.util.Map;

/**
 * @param commentsByRequirementId 可选；键为需求 ID 字符串。某条若填写则优先于全局 {@code comment}，否则沿用全局批注。
 */
public record BatchReviewRequest(
        List<Long> requirementIds,
        String status,
        String comment,
        Map<String, String> commentsByRequirementId
) {}
