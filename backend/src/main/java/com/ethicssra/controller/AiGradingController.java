package com.ethicssra.controller;

import com.ethicssra.dto.AiGradingDto;
import com.ethicssra.dto.ApiResponse;
import com.ethicssra.service.AiGradingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiGradingController {

    private final AiGradingService aiGradingService;

    public AiGradingController(AiGradingService aiGradingService) {
        this.aiGradingService = aiGradingService;
    }

    @PostMapping("/grade/{submissionId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','TA')")
    public ApiResponse<AiGradingDto> gradeSubmission(@PathVariable Long submissionId) {
        try {
            return ApiResponse.ok(aiGradingService.gradeSubmission(submissionId));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
