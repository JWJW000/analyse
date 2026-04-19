package com.ethicssra.controller;

import com.ethicssra.dto.AddCommentRequest;
import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.CommentDto;
import com.ethicssra.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ApiResponse<CommentDto> addComment(@Valid @RequestBody AddCommentRequest request) {
        return ApiResponse.ok(commentService.addComment(request));
    }

    @GetMapping
    public ApiResponse<List<CommentDto>> getComments(
            @RequestParam Long projectId,
            @RequestParam(required = false) Long requirementId) {
        if (requirementId != null) {
            return ApiResponse.ok(commentService.getComments(projectId, requirementId));
        }
        return ApiResponse.ok(commentService.getProjectComments(projectId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ApiResponse.ok(null);
    }
}