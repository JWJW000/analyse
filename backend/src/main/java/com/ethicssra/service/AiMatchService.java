package com.ethicssra.service;

import com.ethicssra.domain.EthicsModule;
import com.ethicssra.domain.MatchEvent;
import com.ethicssra.dto.*;
import com.ethicssra.repository.EthicsModuleRepository;
import com.ethicssra.repository.MatchEventRepository;
import com.ethicssra.util.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiMatchService {

    private final EthicsModuleRepository ethicsModuleRepository;
    private final MatchEventRepository matchEventRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.ai-service-url}")
    private String aiServiceUrl;

    public AiMatchService(
            EthicsModuleRepository ethicsModuleRepository,
            MatchEventRepository matchEventRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.matchEventRepository = matchEventRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ModuleMatchDto> match(String requirementText, Long requirementId, int topK) {
        List<ModuleMatchDto> result = rankModules(requirementText, topK);
        try {
            String idsJson = objectMapper.writeValueAsString(result.stream().map(ModuleMatchDto::moduleId).toList());
            String scoresJson = objectMapper.writeValueAsString(result.stream().map(ModuleMatchDto::score).toList());
            MatchEvent me = new MatchEvent();
            me.setUserId(SecurityUtils.currentUserId());
            me.setRequirementId(requirementId);
            me.setTopModuleIdsJson(idsJson);
            me.setScoresJson(scoresJson);
            matchEventRepository.save(me);
        } catch (JsonProcessingException ignored) {
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public EmbedFeedbackDto embedFeedback(String requirementText, List<Long> embeddedIds) {
        if (embeddedIds == null || embeddedIds.isEmpty()) {
            return new EmbedFeedbackDto(
                    List.of(),
                    List.of(),
                    "当前未嵌入思政模块。可先使用「AI 思政推荐」选择模块并加入嵌入列表后再分析。"
            );
        }
        String reqText = requirementText != null ? requirementText : "";
        List<Map<String, Object>> embeddedPayload = new ArrayList<>();
        for (Long id : embeddedIds) {
            ethicsModuleRepository.findById(id).ifPresent(m -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", m.getId());
                row.put("text", moduleText(m));
                embeddedPayload.add(row);
            });
        }
        if (embeddedPayload.isEmpty()) {
            return new EmbedFeedbackDto(
                    List.of(),
                    List.of(),
                    "嵌入的模块 ID 在库中未找到，请检查嵌入列表。"
            );
        }

        Map<String, Object> body = new HashMap<>();
        body.put("requirement_text", reqText);
        body.put("embedded", embeddedPayload);
        body.put("weak_threshold", 0.30);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> resp = restTemplate.postForObject(aiServiceUrl + "/embed_feedback", entity, Map.class);
        if (resp == null || !Boolean.TRUE.equals(resp.get("ok"))) {
            throw new IllegalStateException("AI 服务不可用");
        }
        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) resp.get("items");
        List<EmbedFeedbackItemDto> items = new ArrayList<>();
        if (rawItems != null) {
            for (Map<String, Object> row : rawItems) {
                Long mid = ((Number) row.get("module_id")).longValue();
                double score = ((Number) row.get("score")).doubleValue();
                boolean weak = Boolean.TRUE.equals(row.get("weak"));
                EthicsModule em = ethicsModuleRepository.findById(mid).orElse(null);
                String title = em != null ? em.getTitle() : "";
                String hint = weak
                        ? "该模块与需求正文语义关联偏弱，可考虑下方推荐模块或调整需求描述以建立更清晰的对应关系。"
                        : "该嵌入与需求正文语义较为一致，可结合课程要求继续润色表述。";
                items.add(new EmbedFeedbackItemDto(mid, score, weak, title, hint));
            }
        }

        Set<Long> embeddedSet = new HashSet<>(embeddedIds);
        List<ModuleMatchDto> ranked = rankModules(reqText, 24);
        List<ModuleMatchDto> alternatives = ranked.stream()
                .filter(m -> !embeddedSet.contains(m.moduleId()))
                .limit(6)
                .collect(Collectors.toList());

        long weakCnt = items.stream().filter(EmbedFeedbackItemDto::weak).count();
        String summary = weakCnt == 0
                ? "已选思政模块与需求正文整体匹配良好，可按教师反馈继续完善。"
                : "有 " + weakCnt + " 个已选模块与需求语义关联偏弱，建议参考下列推荐或修订需求表述。";

        return new EmbedFeedbackDto(items, alternatives, summary);
    }

    public List<ModuleMatchDto> rankByText(String requirementText, int topK) {
        return rankModules(requirementText != null ? requirementText : "", topK);
    }

    @SuppressWarnings("unchecked")
    private List<ModuleMatchDto> rankModules(String requirementText, int topK) {
        List<EthicsModule> modules = ethicsModuleRepository.findAll();
        List<Map<String, Object>> payloadMods = modules.stream().map(m -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("text", moduleText(m));
            return row;
        }).toList();

        Map<String, Object> body = new HashMap<>();
        body.put("requirement_text", requirementText);
        body.put("modules", payloadMods);
        body.put("top_k", topK);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            Map<String, Object> resp = restTemplate.postForObject(aiServiceUrl + "/match", entity, Map.class);
            if (resp == null || !Boolean.TRUE.equals(resp.get("ok"))) {
                throw new IllegalStateException("AI 服务不可用");
            }
            List<Map<String, Object>> matches = (List<Map<String, Object>>) resp.get("matches");
            return matches.stream().map(row -> {
                Long mid = ((Number) row.get("module_id")).longValue();
                double score = ((Number) row.get("score")).doubleValue();
                EthicsModule em = ethicsModuleRepository.findById(mid).orElse(null);
                String title = em != null ? em.getTitle() : "";
                String snippet = em != null && em.getDescription() != null
                        ? em.getDescription().substring(0, Math.min(120, em.getDescription().length()))
                        : "";
                return new ModuleMatchDto(mid, score, title, snippet);
            }).toList();
        } catch (RestClientException | IllegalStateException ex) {
            return localRankModules(requirementText, modules, topK);
        }
    }

    static List<ModuleMatchDto> localRankModules(String requirementText, List<EthicsModule> modules, int topK) {
        String req = requirementText != null ? requirementText : "";
        int limit = Math.max(1, topK);
        return modules.stream()
                .map(m -> new ModuleMatchDto(
                        m.getId(),
                        similarityScore(req, moduleText(m)),
                        m.getTitle() != null ? m.getTitle() : "",
                        m.getDescription() != null
                                ? m.getDescription().substring(0, Math.min(120, m.getDescription().length()))
                                : ""
                ))
                .sorted(Comparator
                        .comparingDouble(ModuleMatchDto::score).reversed()
                        .thenComparing(ModuleMatchDto::moduleId))
                .limit(limit)
                .toList();
    }

    private static double similarityScore(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Set<Integer> setA = left.codePoints().boxed().collect(Collectors.toSet());
        Set<Integer> setB = right.codePoints().boxed().collect(Collectors.toSet());
        long inter = setA.stream().filter(setB::contains).count();
        long union = setA.size() + setB.size() - inter;
        if (union <= 0) {
            return 0.0;
        }
        return (double) inter / (double) union;
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("[\\p{Punct}\\s]+", "").toLowerCase();
    }

    private static String moduleText(EthicsModule m) {
        return String.join(" ", m.getTitle(),
                m.getKeywords() != null ? m.getKeywords() : "",
                m.getDescription() != null ? m.getDescription() : "");
    }

    public Map<String, String> aiHealth() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = restTemplate.getForObject(aiServiceUrl + "/health", Map.class);
            return Map.of("status", String.valueOf(m != null ? m.get("status") : "DOWN"));
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    public List<AiGeneratedContentDto> generateRequirements(String description) {
        return List.of(
            new AiGeneratedContentDto("requirement", "功能需求", "详细描述系统应提供的功能和服务", "high"),
            new AiGeneratedContentDto("requirement", "性能需求", "描述系统的响应时间、吞吐量等性能指标", "medium"),
            new AiGeneratedContentDto("requirement", "安全需求", "描述系统的安全性要求，包括认证、授权、数据保护", "high"),
            new AiGeneratedContentDto("requirement", "接口需求", "描述系统与其他系统的接口要求", "medium"),
            new AiGeneratedContentDto("requirement", "数据需求", "描述系统的数据存储、管理和保护要求", "medium"),
            new AiGeneratedContentDto("requirement", "容错需求", "描述系统在故障情况下的处理能力", "low")
        );
    }

    public List<UserStoryDto> generateUserStories(String requirements) {
        if (requirements == null || requirements.isBlank()) {
            return List.of();
        }
        return List.of(
            new UserStoryDto(
                "作为系统用户",
                "我希望能够清晰地了解系统功能",
                "以便我能够有效地使用系统完成工作"
            ),
            new UserStoryDto(
                "作为教师",
                "我希望系统能够自动分析学生作业",
                "以便我能够更高效地进行教学评估"
            ),
            new UserStoryDto(
                "作为学生",
                "我希望系统能够推荐相关的学习资源",
                "以便我能够更好地完成课程项目"
            )
        );
    }

    public List<UseCaseDto> generateUseCases(String requirements) {
        if (requirements == null || requirements.isBlank()) {
            return List.of();
        }
        return List.of(
            new UseCaseDto(
                "用户登录",
                "用户",
                "用户已注册且账号有效",
                "1. 用户输入用户名和密码\n2. 系统验证凭据\n3. 系统显示登录成功消息\n4. 系统跳转到用户主页",
                "用户已登录并进入系统主页"
            ),
            new UseCaseDto(
                "提交作业",
                "学生",
                "学生已登录且作业处于开放状态",
                "1. 学生选择要提交的作业\n2. 学生上传作业文件\n3. 系统验证文件格式\n4. 系统保存提交记录\n5. 系统显示提交成功消息",
                "作业已成功提交，系统记录提交时间"
            ),
            new UseCaseDto(
                "AI思政匹配",
                "系统",
                "需求文档已存在且包含足够文本",
                "1. 用户请求AI匹配\n2. 系统提取需求文本\n3. 系统调用AI服务获取匹配结果\n4. 系统显示匹配的思政模块列表",
                "用户看到与需求相关的思政模块推荐"
            )
        );
    }

    public String generateFusionContent(String requirementText, Long ethicsModuleId) {
        EthicsModule module = ethicsModuleRepository.findById(ethicsModuleId).orElse(null);
        if (module == null) {
            return "未找到对应的思政模块";
        }

        StringBuilder fusion = new StringBuilder();
        fusion.append("【思政融合说明】\n\n");
        fusion.append("本需求文档与《").append(module.getTitle()).append("》模块进行融合，具体体现在：\n\n");

        if (module.getCaseText() != null && !module.getCaseText().isBlank()) {
            fusion.append("【相关案例】\n").append(module.getCaseText()).append("\n\n");
        }

        if (module.getDescription() != null && !module.getDescription().isBlank()) {
            fusion.append("【融合要点】\n").append(module.getDescription()).append("\n\n");
        }

        fusion.append("【融合说明】\n");
        fusion.append("在需求分析过程中，应充分考虑上述思政要素，将专业能力培养与思想政治教育有机结合，");
        fusion.append("确保学生在掌握技术能力的同时，也能深刻理解相关的伦理责任和社会价值。\n");

        return fusion.toString();
    }
}
