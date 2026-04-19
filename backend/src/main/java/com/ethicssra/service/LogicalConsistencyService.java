package com.ethicssra.service;

import com.ethicssra.dto.FormalConsistencyItemDto;
import com.ethicssra.dto.LanguageAnalysisDto;
import com.ethicssra.dto.LogicalConsistencyDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 基于显式命题与蕴含规则的一致性检查（教学可解释），非通用定理证明。
 * 规则以 Horn 风格可读字符串回传，便于与课程「形式化需求」衔接。
 */
@Service
public class LogicalConsistencyService {

    private static final Pattern SPLIT = Pattern.compile("[\\s,，;；.。!！?？、]+");
    private static final Pattern ETHICS_KW = Pattern.compile(
            "伦理|思政|工程伦理|职业道德|合规|责任|安全|隐私|公平公正|可持续|ethical|ethics|compliance|privacy|responsibility|fairness",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;

    public LogicalConsistencyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LogicalConsistencyDto check(
            String title,
            String textContent,
            String specWizardJson,
            String embeddedModules,
            LanguageAnalysisDto language
    ) {
        List<FormalConsistencyItemDto> items = new ArrayList<>();
        String text = (textContent != null ? textContent : "").toLowerCase(Locale.ROOT);
        String tit = title != null ? title : "";
        JsonNode spec = readSpec(specWizardJson);
        String bg = textOf(spec, "background");
        String goals = textOf(spec, "goals");
        String ethics = textOf(spec, "ethics");
        boolean hasEmbed = embeddedModules != null && !embeddedModules.trim().isEmpty();

        // L1: (embed ≠ ∅) ⇒ ethicsDiscuss(spec ∨ text)
        boolean ethicsDiscuss = ETHICS_KW.matcher(ethics).find() || ETHICS_KW.matcher(text).find();
        boolean l1 = !hasEmbed || ethicsDiscuss;
        items.add(new FormalConsistencyItemDto(
                "L1_EMBED_IMPLIES_ETHICS",
                "(∃嵌入模块) ⇒ (伦理话语出现于规格·伦理节 ∨ 正文)",
                "嵌入与伦理表述一致",
                l1,
                l1 ? "已嵌入模块且伦理相关表述可检索。"
                        : "已选思政嵌入，但规格「伦理与思政」与正文中未明显出现伦理/合规类关键词，建议补充对应论述。"
        ));

        // L2: (goals ≠ ∅) ⇒ 关键词应在正文中有体现
        Set<String> gTokens = tokenize(goals, 12);
        if (gTokens.isEmpty()) {
            items.add(new FormalConsistencyItemDto(
                    "L2_GOALS_COVERED",
                    "(目标节≠∅) ⇒ (目标词项 ∩ 正文 ≠ ∅)",
                    "目标与正文关键词覆盖",
                    true,
                    "目标节为空或无法分词，跳过覆盖检查。"
            ));
        } else {
            int hit = 0;
            for (String t : gTokens) {
                if (t.length() < 2) {
                    continue;
                }
                if (text.contains(t.toLowerCase(Locale.ROOT))) {
                    hit++;
                }
            }
            double ratio = hit / (double) gTokens.size();
            boolean l2Ok = gTokens.size() <= 2 ? hit >= 1 : ratio >= 0.25;
            items.add(new FormalConsistencyItemDto(
                    "L2_GOALS_COVERED",
                    "(目标节≠∅) ⇒ (目标词项在正文中可检索)",
                    "目标与正文关键词覆盖",
                    l2Ok,
                    l2Ok ? "目标节关键词在正文中有一定覆盖。"
                            : "目标节中的部分关键词未在正文出现，可能存在目标—正文脱节，请对齐表述。"
            ));
        }

        // L3: (背景≠∅ ∧ 目标≠∅) ⇒ 背景与目标存在共享词项（连贯性）
        Set<String> bTokens = tokenize(bg, 12);
        boolean l3;
        if (bTokens.isEmpty() || gTokens.isEmpty()) {
            l3 = true;
            items.add(new FormalConsistencyItemDto(
                    "L3_BG_GOALS_COHERENCE",
                    "(背景≠∅ ∧ 目标≠∅) ⇒ (背景词 ∩ 目标词 ≠ ∅)",
                    "背景与目标连贯",
                    true,
                    "背景或目标过短，跳过连贯性检查。"
            ));
        } else {
            boolean overlap = false;
            for (String a : bTokens) {
                for (String b : gTokens) {
                    if (a.length() >= 2 && a.equalsIgnoreCase(b)) {
                        overlap = true;
                        break;
                    }
                }
                if (overlap) {
                    break;
                }
            }
            l3 = overlap;
            items.add(new FormalConsistencyItemDto(
                    "L3_BG_GOALS_COHERENCE",
                    "(背景≠∅ ∧ 目标≠∅) ⇒ ∃w∈(背景词∩目标词)",
                    "背景与目标连贯",
                    l3,
                    l3 ? "背景与目标存在共同主题词。"
                            : "背景与目标词项交集为空，建议检查是否同一项目语境。"
            ));
        }

        // L4: 英文主语言 ⇒ 用户故事结构提示（软约束，不满足仅提示）
        boolean l4 = true;
        if ("EN".equals(language.primaryLanguage())) {
            String low = (tit + "\n" + textContent).toLowerCase(Locale.ROOT);
            boolean hasStory = low.contains("as a") || low.contains("i want") || low.contains("user story");
            l4 = hasStory;
            items.add(new FormalConsistencyItemDto(
                    "L4_EN_USER_STORY",
                    "Lang=EN ⇒ (出现 As a / I want 等结构) [软约束]",
                    "英文用户故事结构",
                    l4,
                    l4 ? "英文主语言下已出现常见用户故事措辞。"
                            : "英文主语言文档中未检出典型用户故事句式，建议补充 As a … I want …。"
            ));
        }

        // L5: 潜在矛盾：离线能力与实时性同时强主张（极轻量启发式）
        boolean offline = text.contains("离线") || text.contains("offline");
        boolean realtime = text.contains("实时") || text.contains("real-time") || text.contains("realtime");
        boolean l5 = !(offline && realtime);
        items.add(new FormalConsistencyItemDto(
                "L5_OFFLINE_REALTIME",
                "¬(离线强约束 ∧ 实时强约束) 若无并列说明",
                "离线/实时约束",
                l5,
                l5 ? "未同时强主张离线与实时冲突（或未检出）。"
                        : "同时出现「离线」与「实时」相关表述，请检查场景是否需分模式说明以避免矛盾。"
        ));

        boolean all = items.stream().allMatch(FormalConsistencyItemDto::satisfied);
        String summary = all ? "当前规则集下未检出逻辑冲突（启发式）。"
                : "存在未满足的蕴含规则，请根据提示修订文档。";
        return new LogicalConsistencyDto(all, summary, items);
    }

    private JsonNode readSpec(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private static String textOf(JsonNode spec, String field) {
        if (spec == null || !spec.has(field)) {
            return "";
        }
        return spec.get(field).asText("").trim();
    }

    private static Set<String> tokenize(String s, int max) {
        Set<String> out = new LinkedHashSet<>();
        if (s == null || s.isBlank()) {
            return out;
        }
        for (String p : SPLIT.split(s)) {
            String t = p.trim();
            if (t.length() >= 2 && t.length() <= 32) {
                out.add(t);
            }
            if (out.size() >= max) {
                break;
            }
        }
        return out;
    }
}
