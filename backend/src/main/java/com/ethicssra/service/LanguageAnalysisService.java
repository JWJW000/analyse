package com.ethicssra.service;

import com.ethicssra.dto.LanguageAnalysisDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于字符脚本占比的多语言分析（中文/英文/混合），用于需求文档场景。
 */
@Service
public class LanguageAnalysisService {

    public LanguageAnalysisDto analyze(String title, String text) {
        String combined = (title != null ? title : "") + "\n" + (text != null ? text : "");
        int cjk = 0;
        int latin = 0;
        for (char c : combined.toCharArray()) {
            if (isCjk(c)) {
                cjk++;
            } else if (isLatinLetter(c)) {
                latin++;
            }
        }
        int scriptTotal = cjk + latin;
        if (scriptTotal < 8) {
            List<String> hints = new ArrayList<>();
            hints.add("有效字符过少，无法可靠判断语言；请补充标题与正文。");
            return new LanguageAnalysisDto("UNKNOWN", 0, 0, hints);
        }
        double zhR = cjk / (double) scriptTotal;
        double enR = latin / (double) scriptTotal;

        String primary;
        if (zhR >= 0.52 && enR <= 0.28) {
            primary = "ZH";
        } else if (enR >= 0.52 && zhR <= 0.28) {
            primary = "EN";
        } else if (zhR >= 0.2 && enR >= 0.2) {
            primary = "MIXED";
        } else if (cjk >= latin) {
            primary = "ZH";
        } else {
            primary = "EN";
        }

        List<String> hints = new ArrayList<>();
        switch (primary) {
            case "ZH" -> hints.add("主语言判定为中文：用户故事可使用「作为…我希望…」等表述。");
            case "EN" -> hints.add("Primary language appears English: consider explicit user stories (As a / I want / so that).");
            case "MIXED" -> hints.add("中英混排：若课程要求单语正文，建议统一主要语言或为各节标注语言。");
            default -> hints.add("语言特征不明显，可补充更多正文后再分析。");
        }
        if ("MIXED".equals(primary)) {
            hints.add("混排场景下，规格向导三节建议与正文主语言一致，便于评审。");
        }
        return new LanguageAnalysisDto(primary, round3(zhR), round3(enR), hints);
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static boolean isCjk(char c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3400 && c <= 0x4DBF)
                || (c >= 0x20000 && c <= 0x2A6DF);
    }

    private static boolean isLatinLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
