package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.CollaborationSessionDto;
import com.ethicssra.service.CollaborationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/collaboration")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @PostMapping("/lock/{requirementId}")
    public ApiResponse<CollaborationSessionDto> lockDocument(@PathVariable Long requirementId) {
        try {
            return ApiResponse.ok(collaborationService.lockDocument(requirementId));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @DeleteMapping("/lock/{requirementId}")
    public ApiResponse<Void> unlockDocument(@PathVariable Long requirementId) {
        try {
            collaborationService.unlockDocument(requirementId);
            return ApiResponse.ok(null);
        } catch (IllegalStateException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/lock/{requirementId}")
    public ApiResponse<Map<String, Object>> getLockStatus(@PathVariable Long requirementId) {
        Optional<CollaborationSessionDto> status = collaborationService.getLockStatus(requirementId);
        if (status.isEmpty()) {
            return ApiResponse.ok(Map.of("locked", false));
        }
        CollaborationSessionDto lock = status.get();
        return ApiResponse.ok(Map.of(
            "locked", true,
            "session", lock
        ));
    }

    @PostMapping("/extend/{requirementId}")
    public ApiResponse<CollaborationSessionDto> extendLock(@PathVariable Long requirementId) {
        try {
            collaborationService.extendLock(requirementId);
            return collaborationService.getLockStatus(requirementId)
                .map(ApiResponse::ok)
                .orElse(ApiResponse.fail("锁已过期"));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
