package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.RequirementReferenceLinkCreateRequest;
import com.ethicssra.dto.RequirementReferenceLinkDto;
import com.ethicssra.service.RequirementReferenceLinkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RequirementReferenceLinkController {

    private final RequirementReferenceLinkService linkService;

    public RequirementReferenceLinkController(RequirementReferenceLinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping("/requirements/{requirementId}/reference-links")
    public ApiResponse<List<RequirementReferenceLinkDto>> list(@PathVariable Long requirementId) {
        try {
            return ApiResponse.ok(linkService.list(requirementId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/requirements/{requirementId}/reference-links")
    public ApiResponse<RequirementReferenceLinkDto> create(
            @PathVariable Long requirementId,
            @Valid @RequestBody RequirementReferenceLinkCreateRequest req
    ) {
        try {
            return ApiResponse.ok(linkService.create(requirementId, req));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/reference-links/{linkId}")
    public ApiResponse<Void> delete(@PathVariable Long linkId) {
        try {
            linkService.delete(linkId);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
