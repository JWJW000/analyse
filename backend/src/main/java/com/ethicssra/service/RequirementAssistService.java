package com.ethicssra.service;

import com.ethicssra.config.DashScopeConfig;
import com.ethicssra.dto.ContinueTextDto;
import com.ethicssra.dto.DraftSpecDto;
import com.ethicssra.service.llm.DashScopeGateway;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RequirementAssistService {

    private static final Pattern SPLIT = Pattern.compile("[\\s,，;；、。！？!?\n\r]+");

    private final DashScopeGateway llmGateway;
    private final DashScopeConfig dashScopeConfig;

    public RequirementAssistService(DashScopeGateway llmGateway, DashScopeConfig dashScopeConfig) {
        this.llmGateway = llmGateway;
        this.dashScopeConfig = dashScopeConfig;
    }

    public DraftSpecDto draftSpec(String title, String textContent) {
        String t = title != null ? title.trim() : "";
        String body = textContent != null ? textContent.trim() : "";
        List<String> keywords = extractKeywords(t + " " + body);
        String source = body.isEmpty() ? t : body;
        String[] sentences = splitSentences(source);
        String bg;
        if (sentences.length > 0) {
            bg = String.join("。", java.util.Arrays.copyOfRange(sentences, 0, Math.min(2, sentences.length))) + "。";
        } else if (!source.isEmpty()) {
            bg = source.length() > 240 ? source.substring(0, 240) + "…" : source;
        } else {
            bg = t.isEmpty() ? "（请补充项目或课题背景。）" : "本需求围绕「" + t + "」展开，请补充业务场景与干系人。";
        }
        String goals = t.isEmpty()
                ? "（请概括功能目标、范围与验收要点。）"
                : "围绕「" + t + "」完成需求分析：明确功能范围、用户角色、主要用例及非功能约束（性能、安全、合规等）。";
        String ethics = "结合工程伦理与课程思政要求：在需求中体现社会责任、安全可信、公平公正与可持续发展等价值取向，并说明与所选思政模块的对应关系。";
        return new DraftSpecDto(keywords, bg, goals, ethics);
    }

    public ContinueTextDto continueText(String title, String textContent, String specWizardJson) {
        String t = title != null ? title.trim() : "";
        String body = textContent != null ? textContent.trim() : "";
        if (canUseLlm()) {
            try {
                String generated = llmGateway.chat(
                        "你是一名软件工程需求分析助教。请只输出可直接追加到需求正文后的中文段落，不要输出标题、编号外的解释。",
                        buildContinuePrompt(t, body, specWizardJson)
                );
                String cleaned = generated != null ? generated.trim() : "";
                if (!cleaned.isBlank()) {
                    return new ContinueTextDto(cleaned, "llm");
                }
            } catch (Exception ignored) {
                // Fallback keeps classroom demos usable when external AI is not configured.
            }
        }
        return new ContinueTextDto(ruleContinue(t, body), "rule");
    }

    private boolean canUseLlm() {
        if (llmGateway == null || dashScopeConfig == null) {
            return false;
        }
        String key = dashScopeConfig.getApiKey();
        return key != null && !key.isBlank() && !"your-api-key-here".equals(key);
    }

    private static String buildContinuePrompt(String title, String body, String specWizardJson) {
        String base = body.isBlank() ? "（当前正文为空）" : body;
        return """
                需求标题：%s
                当前需求正文：
                %s

                规格向导 JSON：
                %s

                请续写 2 到 4 段，补充：
                1. 关键功能流程与角色边界
                2. 验收标准
                3. 非功能约束
                4. 工程伦理或课程思政融入点
                """.formatted(title.isBlank() ? "未命名需求" : title, base, specWizardJson != null ? specWizardJson : "{}");
    }

    private static String ruleContinue(String title, String body) {
        String subject = title != null && !title.isBlank() ? "「" + title.trim() + "」" : "本系统";
        String context = body != null && !body.isBlank()
                ? "在已有需求描述基础上，后续设计应进一步明确主要用户、关键业务对象以及异常处理边界。"
                : subject + "应先明确目标用户、核心场景与任务边界，形成可评审的需求描述。";
        return context + "\n\n"
                + "功能流程方面，" + subject + "需要围绕用户发起操作、系统校验、结果反馈和数据留痕形成闭环。每个关键操作应说明前置条件、主成功场景、失败提示与恢复方式，避免只描述功能名称而缺少可执行流程。\n\n"
                + "验收标准方面，应至少覆盖输入合法性、核心流程完成率、关键页面响应、数据保存一致性和权限控制。对于教师或管理员参与的场景，还应说明审核、退回、再次提交等状态变化是否可追踪。\n\n"
                + "非功能与伦理要求方面，应补充安全性、隐私保护、可用性和公平性约束。系统在采集、展示和分析用户数据时，应遵循最小必要原则，并通过日志、提示和人工复核机制降低误判、滥用或信息泄露风险。";
    }

    private static String[] splitSentences(String text) {
        if (text.isBlank()) {
            return new String[0];
        }
        String[] parts = text.split("[。！？\n]");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.length() >= 8) {
                out.add(s);
            }
        }
        return out.toArray(new String[0]);
    }

    private static List<String> extractKeywords(String raw) {
        Set<String> seen = new LinkedHashSet<>();
        for (String token : SPLIT.split(raw)) {
            String s = token.trim();
            if (s.length() < 2 || s.length() > 24) {
                continue;
            }
            if (s.matches("^[\\p{IsHan}a-zA-Z0-9_-]+$")) {
                seen.add(s);
            }
            if (seen.size() >= 12) {
                break;
            }
        }
        return new ArrayList<>(seen);
    }
}
