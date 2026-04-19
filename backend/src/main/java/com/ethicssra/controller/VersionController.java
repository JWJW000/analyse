package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.CreateVersionRequest;
import com.ethicssra.dto.DocumentVersionDto;
import com.ethicssra.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/versions")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping
    public ApiResponse<DocumentVersionDto> createVersion(@Valid @RequestBody CreateVersionRequest request) {
        return ApiResponse.ok(versionService.createVersion(request));
    }

    @GetMapping
    public ApiResponse<List<DocumentVersionDto>> getVersions(
            @RequestParam Long projectId,
            @RequestParam Long requirementId) {
        return ApiResponse.ok(versionService.getVersions(projectId, requirementId));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentVersionDto> getVersion(@PathVariable Long id) {
        return ApiResponse.ok(versionService.getVersion(id));
    }
}