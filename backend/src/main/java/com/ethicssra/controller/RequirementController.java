package com.ethicssra.controller;

import com.ethicssra.domain.RequirementStatus;
import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.BatchReviewRequest;
import com.ethicssra.dto.DocumentAnalysisDto;
import com.ethicssra.dto.IntegrityCheckDto;
import com.ethicssra.dto.IntegrityCheckRequest;
import com.ethicssra.dto.RequirementDto;
import com.ethicssra.dto.RequirementSaveRequest;
import com.ethicssra.service.DocumentAnalysisService;
import com.ethicssra.service.RequirementIntegrityService;
import com.ethicssra.service.RequirementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

    private final RequirementService requirementService;
    private final RequirementIntegrityService requirementIntegrityService;
    private final DocumentAnalysisService documentAnalysisService;

    public RequirementController(
            RequirementService requirementService,
            RequirementIntegrityService requirementIntegrityService,
            DocumentAnalysisService documentAnalysisService
    ) {
        this.requirementService = requirementService;
        this.requirementIntegrityService = requirementIntegrityService;
        this.documentAnalysisService = documentAnalysisService;
    }

    @PostMapping("/check-integrity")
    public ApiResponse<IntegrityCheckDto> checkIntegrity(@RequestBody IntegrityCheckRequest req) {
        return ApiResponse.ok(requirementIntegrityService.check(req));
    }

    /** 多语言脚本分析 + 基于显式蕴含规则的逻辑一致性检查（教学可解释）。 */
    @PostMapping("/analyze-document")
    public ApiResponse<DocumentAnalysisDto> analyzeDocument(@RequestBody IntegrityCheckRequest req) {
        return ApiResponse.ok(documentAnalysisService.analyze(req));
    }

    @PostMapping("/batch-review")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<RequirementDto>> batchReview(@RequestBody BatchReviewRequest body) {
        try {
            RequirementStatus st = RequirementStatus.valueOf(body.status());
            return ApiResponse.ok(requirementService.batchReview(
                    body.requirementIds(), st, body.comment(), body.commentsByRequirementId()));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/mine")
    public ApiResponse<List<RequirementDto>> mine() {
        return ApiResponse.ok(requirementService.myList());
    }

    @GetMapping("/{id}")
    public ApiResponse<RequirementDto> get(@PathVariable Long id) {
        try {
            return ApiResponse.ok(requirementService.get(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping
    public ApiResponse<RequirementDto> create(@RequestBody RequirementSaveRequest req) {
        return ApiResponse.ok(requirementService.save(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<RequirementDto> update(@PathVariable Long id, @RequestBody RequirementSaveRequest req) {
        try {
            return ApiResponse.ok(requirementService.update(id, req));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        try {
            requirementService.submit(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<RequirementDto> review(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body
    ) {
        try {
            RequirementStatus st = RequirementStatus.valueOf(String.valueOf(body.get("status")));
            String comment = body.get("comment") != null ? String.valueOf(body.get("comment")) : null;
            return ApiResponse.ok(requirementService.review(id, st, comment));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
