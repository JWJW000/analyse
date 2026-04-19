package com.ethicssra.service;

import com.ethicssra.domain.EthicsModule;
import com.ethicssra.domain.MatchEvent;
import com.ethicssra.domain.Requirement;
import com.ethicssra.dto.ModuleMatchDto;
import com.ethicssra.dto.RecommendedModuleDto;
import com.ethicssra.repository.EthicsModuleRepository;
import com.ethicssra.repository.MatchEventRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.util.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendationService {

    private static final String REASON_CONTENT = "CONTENT_SIMILARITY";
    private static final String REASON_POPULAR = "POPULAR";

    private final AiMatchService aiMatchService;
    private final RequirementRepository requirementRepository;
    private final MatchEventRepository matchEventRepository;
    private final EthicsModuleRepository ethicsModuleRepository;
    private final ObjectMapper objectMapper;

    public RecommendationService(
            AiMatchService aiMatchService,
            RequirementRepository requirementRepository,
            MatchEventRepository matchEventRepository,
            EthicsModuleRepository ethicsModuleRepository,
            ObjectMapper objectMapper
    ) {
        this.aiMatchService = aiMatchService;
        this.requirementRepository = requirementRepository;
        this.matchEventRepository = matchEventRepository;
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 混合推荐：优先根据当前用户最近一条需求正文做内容相似（需 AI 服务）；不足时用全站匹配热度补足。
     */
    public List<RecommendedModuleDto> recommendEthicsModules(int limit) {
        if (limit <= 0) {
            limit = 8;
        }
        Long uid = SecurityUtils.currentUserId();
        Set<Long> seen = new LinkedHashSet<>();
        List<RecommendedModuleDto> out = new ArrayList<>();

        String combinedText = latestRequirementCombinedText(uid);
        if (combinedText != null && !combinedText.isBlank()) {
            try {
                List<ModuleMatchDto> ranked = aiMatchService.rankByText(combinedText, limit + 4);
                for (ModuleMatchDto m : ranked) {
                    if (out.size() >= limit) {
                        break;
                    }
                    if (seen.add(m.moduleId())) {
                        out.add(new RecommendedModuleDto(
                                m.moduleId(),
                                m.title(),
                                m.snippet(),
                                REASON_CONTENT,
                                m.score()
                        ));
                    }
                }
            } catch (Exception ignored) {
                /* AI 不可用时仅走热度 */
            }
        }

        for (Long mid : popularModuleIds(limit + 8)) {
            if (out.size() >= limit) {
                break;
            }
            if (!seen.add(mid)) {
                continue;
            }
            EthicsModule em = ethicsModuleRepository.findById(mid).orElse(null);
            if (em == null) {
                continue;
            }
            String snip = em.getDescription() != null
                    ? em.getDescription().substring(0, Math.min(120, em.getDescription().length()))
                    : "";
            out.add(new RecommendedModuleDto(mid, em.getTitle(), snip, REASON_POPULAR, null));
        }

        return out.stream().limit(limit).toList();
    }

    private String latestRequirementCombinedText(Long userId) {
        List<Requirement> list = requirementRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (list.isEmpty()) {
            return null;
        }
        Requirement r = list.get(0);
        String t = r.getTitle() != null ? r.getTitle() : "";
        String b = r.getTextContent() != null ? r.getTextContent() : "";
        return (t + "\n" + b).trim();
    }

    private List<Long> popularModuleIds(int max) {
        var page = matchEventRepository.findAll(
                PageRequest.of(0, 400, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Map<Long, Integer> freq = new HashMap<>();
        for (MatchEvent me : page.getContent()) {
            if (me.getTopModuleIdsJson() == null || me.getTopModuleIdsJson().isBlank()) {
                continue;
            }
            try {
                List<Long> ids = objectMapper.readValue(
                        me.getTopModuleIdsJson(),
                        new TypeReference<List<Long>>() {
                        }
                );
                for (Long id : ids) {
                    freq.merge(id, 1, Integer::sum);
                }
            } catch (Exception ignored) {
            }
        }
        return freq.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .map(Map.Entry::getKey)
                .limit(max)
                .toList();
    }
}
