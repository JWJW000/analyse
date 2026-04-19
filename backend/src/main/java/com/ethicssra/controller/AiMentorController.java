package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.service.AiMentorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/mentor")
public class AiMentorController {

    private final AiMentorService aiMentorService;

    public AiMentorController(AiMentorService aiMentorService) {
        this.aiMentorService = aiMentorService;
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<AiMentorService.MentorSuggestion>> getSuggestions(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "dashboard") String page) {
        return ApiResponse.ok(aiMentorService.getSuggestions(projectId, page));
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(
            @RequestParam Long projectId,
            @RequestBody ChatRequest request) {
        List<AiMentorService.ChatMessage> history = request.getHistory();
        String response = aiMentorService.chat(projectId, request.getMessage(), history);
        return ApiResponse.ok(Map.of("response", response));
    }

    @PostMapping("/analyze-requirement")
    public ApiResponse<Map<String, String>> analyzeRequirement(
            @RequestParam Long projectId,
            @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        String analysis = aiMentorService.analyzeRequirement(projectId, title, content);
        return ApiResponse.ok(Map.of("analysis", analysis));
    }

    @PostMapping("/generate-use-cases")
    public ApiResponse<Map<String, String>> generateUseCases(
            @RequestParam Long projectId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String useCases = aiMentorService.generateUseCases(projectId, content);
        return ApiResponse.ok(Map.of("useCases", useCases));
    }

    @PostMapping("/generate-requirement-draft")
    public ApiResponse<Map<String, String>> generateRequirementDraft(
            @RequestParam Long projectId,
            @RequestBody Map<String, String> body) {
        String description = body.get("description");
        String draft = aiMentorService.generateRequirementDraft(projectId, description);
        return ApiResponse.ok(Map.of("draft", draft));
    }

    @PostMapping("/generate-fusion")
    public ApiResponse<Map<String, String>> generateFusion(
            @RequestParam Long projectId,
            @RequestBody Map<String, Object> body) {
        String requirementText = (String) body.get("requirementText");
        Long ethicsModuleId = ((Number) body.get("ethicsModuleId")).longValue();
        String fusion = aiMentorService.generateFusionContent(projectId, requirementText, ethicsModuleId);
        return ApiResponse.ok(Map.of("fusion", fusion));
    }

    public static class ChatRequest {
        private String message;
        private List<AiMentorService.ChatMessage> history;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<AiMentorService.ChatMessage> getHistory() { return history; }
        public void setHistory(List<AiMentorService.ChatMessage> history) { this.history = history; }
    }

    // 教师端 AI 功能

    @PostMapping("/teacher/generate-feedback")
    public ApiResponse<Map<String, String>> generateFeedback(
            @RequestBody GenerateFeedbackRequest request) {
        String feedback = aiMentorService.generateFeedback(
            request.requirementId(),
            request.requirementTitle(),
            request.requirementContent(),
            request.studentName(),
            request.courseName()
        );
        return ApiResponse.ok(Map.of("feedback", feedback));
    }

    @PostMapping("/teacher/analyze-quality")
    public ApiResponse<Map<String, String>> analyzeSubmissionQuality(
            @RequestBody AnalyzeQualityRequest request) {
        String analysis = aiMentorService.analyzeSubmissionQuality(
            request.submissionId(),
            request.submissionContent(),
            request.studentName(),
            request.projectName()
        );
        return ApiResponse.ok(Map.of("analysis", analysis));
    }

    @PostMapping("/student/suggest-improvements")
    public ApiResponse<Map<String, String>> suggestImprovements(
            @RequestBody SuggestImprovementsRequest request) {
        String suggestions = aiMentorService.suggestImprovements(
            request.requirementId(),
            request.requirementTitle(),
            request.requirementContent(),
            request.currentFeedback()
        );
        return ApiResponse.ok(Map.of("suggestions", suggestions));
    }

    public record GenerateFeedbackRequest(
        Long requirementId,
        String requirementTitle,
        String requirementContent,
        String studentName,
        String courseName
    ) {}

    public record AnalyzeQualityRequest(
        Long submissionId,
        String submissionContent,
        String studentName,
        String projectName
    ) {}

    public record SuggestImprovementsRequest(
        Long requirementId,
        String requirementTitle,
        String requirementContent,
        String currentFeedback
    ) {}
}
