package com.ethicssra.dto;

import java.util.List;

/**
 * 文档主语言与多语言分析结果（启发式，非语料库级 NLP）。
 */
public record LanguageAnalysisDto(
        /** ZH | EN | MIXED | UNKNOWN */
        String primaryLanguage,
        double zhScriptRatio,
        double latinScriptRatio,
        List<String> hints
) {
}
