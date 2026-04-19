package com.ethicssra.dto;

/**
 * 当前项目阶段进入「下一阶段」前需满足的检查项（用于前端步骤条与门禁提示）。
 */
public record ProjectPhaseChecklistItemDto(
        String key,
        String label,
        boolean satisfied,
        String hint
) {}
