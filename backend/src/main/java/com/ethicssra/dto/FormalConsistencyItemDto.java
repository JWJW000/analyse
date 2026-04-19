package com.ethicssra.dto;

/**
 * 单条逻辑一致性判定：带可读的逻辑式说明（蕴含/合取），便于教学与追溯。
 */
public record FormalConsistencyItemDto(
        String id,
        /** 形式化规则简述，如 "(E≠∅) ⇒ ethicsDiscuss" */
        String formalRule,
        String label,
        boolean satisfied,
        String hint
) {
}
