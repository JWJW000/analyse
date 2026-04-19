package com.ethicssra.controller;

import com.ethicssra.dto.AiAnswerDto;
import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.CreateDiscussionPostRequest;
import com.ethicssra.dto.DiscussionPostDto;
import com.ethicssra.dto.DiscussionStatsDto;
import com.ethicssra.dto.PatchDiscussionPostRequest;
import com.ethicssra.service.CourseDiscussionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses/{courseId}/discussion/posts")
public class CourseDiscussionController {

    private final CourseDiscussionService courseDiscussionService;

    public CourseDiscussionController(CourseDiscussionService courseDiscussionService) {
        this.courseDiscussionService = courseDiscussionService;
    }

    @GetMapping
    public ApiResponse<List<DiscussionPostDto>> list(
            @PathVariable Long courseId,
            @RequestParam(required = false) String category
    ) {
        try {
            if (category != null && !category.isBlank()) {
                return ApiResponse.ok(courseDiscussionService.list(courseId, category));
            }
            return ApiResponse.ok(courseDiscussionService.list(courseId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ApiResponse<DiscussionStatsDto> stats(@PathVariable Long courseId) {
        try {
            return ApiResponse.ok(courseDiscussionService.getStats(courseId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<DiscussionPostDto> create(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateDiscussionPostRequest req
    ) {
        try {
            return ApiResponse.ok(courseDiscussionService.create(courseId, req));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PatchMapping("/{postId}")
    public ApiResponse<DiscussionPostDto> patch(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @RequestBody PatchDiscussionPostRequest req
    ) {
        try {
            if (req.visible() == null) {
                return ApiResponse.fail("请指定 visible");
            }
            return ApiResponse.ok(courseDiscussionService.setVisible(courseId, postId, req.visible()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/ai-answer")
    public ApiResponse<AiAnswerDto> generateAiAnswer(
            @PathVariable Long courseId,
            @RequestBody Map<String, String> body
    ) {
        try {
            String question = body.get("question");
            if (question == null || question.isBlank()) {
                return ApiResponse.fail("问题不能为空");
            }
            return ApiResponse.ok(courseDiscussionService.generateAiAnswer(courseId, question));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
