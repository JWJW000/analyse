package com.ethicssra.controller;

import com.ethicssra.dto.*;
import com.ethicssra.service.AiMatchService;
import com.ethicssra.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiMatchService aiMatchService;
    private final RequirementService requirementService;

    public AiController(AiMatchService aiMatchService, RequirementService requirementService) {
        this.aiMatchService = aiMatchService;
        this.requirementService = requirementService;
    }

    @PostMapping("/match")
    public ApiResponse<List<ModuleMatchDto>> match(@Valid @RequestBody MatchRequestDto req) {
        try {
            int k = req.topK() != null ? req.topK() : 10;
            return ApiResponse.ok(aiMatchService.match(req.requirementText(), req.requirementId(), k));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(aiMatchService.aiHealth());
    }

    /** 智能嵌入反馈：对比需求正文与已选思政模块的语义匹配，并给出替代推荐。 */
    @PostMapping("/embed-feedback")
    public ApiResponse<EmbedFeedbackDto> embedFeedback(@Valid @RequestBody EmbedFeedbackRequestDto req) {
        try {
            RequirementDto r = requirementService.get(req.requirementId());
            String combined = ((r.textContent() != null ? r.textContent() : "") + "\n"
                    + (r.title() != null ? r.title() : "")).trim();
            return ApiResponse.ok(aiMatchService.embedFeedback(combined, parseEmbeddedIds(r.embeddedModules())));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/generate-requirements")
    public ApiResponse<List<AiGeneratedContentDto>> generateRequirements(@RequestBody GenerateRequirementsRequest req) {
        try {
            return ApiResponse.ok(aiMatchService.generateRequirements(req.description()));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/generate-user-stories")
    public ApiResponse<Map<String, List<UserStoryDto>>> generateUserStories(@RequestBody Map<String, String> body) {
        try {
            String requirements = body.get("requirements");
            return ApiResponse.ok(Map.of("stories", aiMatchService.generateUserStories(requirements)));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/generate-use-cases")
    public ApiResponse<Map<String, List<UseCaseDto>>> generateUseCases(@RequestBody Map<String, String> body) {
        try {
            String requirements = body.get("requirements");
            return ApiResponse.ok(Map.of("useCases", aiMatchService.generateUseCases(requirements)));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/fusion-content")
    public ApiResponse<Map<String, String>> generateFusionContent(@RequestBody Map<String, Object> body) {
        try {
            String text = (String) body.get("text");
            Long ethicsModuleId = ((Number) body.get("ethicsModuleId")).longValue();
            String content = aiMatchService.generateFusionContent(text, ethicsModuleId);
            return ApiResponse.ok(Map.of("content", content));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private static List<Long> parseEmbeddedIds(String embedded) {
        if (embedded == null || embedded.isBlank()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String p : embedded.split(",")) {
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
