package com.ethicssra.service;

import com.ethicssra.dto.IntegrityCheckDto;
import com.ethicssra.dto.IntegrityCheckRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RequirementIntegrityService {

    private final ObjectMapper objectMapper;

    public RequirementIntegrityService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IntegrityCheckDto check(IntegrityCheckRequest req) {
        List<IntegrityCheckDto.CheckItem> items = new ArrayList<>();
        int okPoints = 0;
        int total = 7;

        String title = req.title() != null ? req.title().trim() : "";
        boolean titleOk = title.length() >= 2;
        items.add(new IntegrityCheckDto.CheckItem(
                "title", titleOk, "标题",
                titleOk ? "标题清晰" : "标题过短或为空，建议至少 2 个字符"));
        if (titleOk) {
            okPoints++;
        }

        String text = req.textContent() != null ? req.textContent().trim() : "";
        boolean textOk = text.length() >= 80;
        items.add(new IntegrityCheckDto.CheckItem(
                "text", textOk, "需求正文篇幅",
                textOk ? "正文信息量足够" : "正文建议不少于约 80 字，便于评审与 AI 匹配"));
        if (textOk) {
            okPoints++;
        }

        boolean hasUserStory = text.contains("作为") || text.contains("用户") || text.contains("角色");
        items.add(new IntegrityCheckDto.CheckItem(
                "story", hasUserStory, "用户故事/角色表述",
                hasUserStory ? "已出现角色或用户相关表述" : "可补充「作为…用户，我希望…」类描述"));
        if (hasUserStory) {
            okPoints++;
        }

        JsonNode spec = readSpec(req.specWizardJson());
        boolean bg = nonEmpty(spec, "background");
        boolean goals = nonEmpty(spec, "goals");
        boolean ethics = nonEmpty(spec, "ethics");
        boolean specOk = bg && goals && ethics;
        items.add(new IntegrityCheckDto.CheckItem(
                "spec", specOk, "规格向导三节",
                specOk ? "背景/目标/伦理说明已填写" : "请在规格向导中补全项目背景、目标与范围、伦理与思政说明"));
        if (specOk) {
            okPoints++;
        }

        String diagram = req.diagramJson();
        boolean diagramOk = diagram != null && diagram.length() > 30 && !"null".equals(diagram);
        items.add(new IntegrityCheckDto.CheckItem(
                "diagram", diagramOk, "用例图",
                diagramOk ? "已保存用例图数据" : "建议在用例图页绘制参与者与用例"));
        if (diagramOk) {
            okPoints++;
        }

        String emb = req.embeddedModules();
        boolean embOk = emb != null && !emb.trim().isEmpty();
        items.add(new IntegrityCheckDto.CheckItem(
                "embed", embOk, "思政模块嵌入",
                embOk ? "已记录嵌入的模块 ID" : "可在 AI 推荐页选择并加入嵌入列表"));
        if (embOk) {
            okPoints++;
        }

        boolean nonFunctional = text.contains("性能") || text.contains("安全") || text.contains("可靠")
                || text.contains("并发") || text.contains("隐私");
        items.add(new IntegrityCheckDto.CheckItem(
                "nfr", nonFunctional, "非功能/约束提示",
                nonFunctional ? "正文涉及性能或安全等约束" : "可考虑补充性能、安全、合规等非功能需求"));
        if (nonFunctional) {
            okPoints++;
        }

        int score = (int) Math.round(okPoints * 100.0 / total);
        String summary = score >= 85 ? "整体较完整，可提交评审"
                : score >= 55 ? "尚有改进空间，建议按清单补齐"
                : "关键项缺失较多，请先完善后再提交";
        return new IntegrityCheckDto(score, summary, items);
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

    private boolean nonEmpty(JsonNode spec, String field) {
        if (spec == null || !spec.has(field)) {
            return false;
        }
        String v = spec.get(field).asText("");
        return v != null && v.trim().length() >= 8;
    }
}
