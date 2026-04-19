package com.ethicssra.controller;

import com.ethicssra.dto.*;
import com.ethicssra.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ApiResponse<ProjectDto> create(@Valid @RequestBody CreateProjectRequest request) {
        return ApiResponse.ok(projectService.createProject(request));
    }

    @GetMapping
    public ApiResponse<List<ProjectDto>> list() {
        return ApiResponse.ok(projectService.getUserProjects());
    }

    @GetMapping("/{id}/phase-checklist")
    public ApiResponse<List<ProjectPhaseChecklistItemDto>> phaseChecklist(@PathVariable Long id) {
        return ApiResponse.ok(projectService.phaseChecklist(id));
    }

    @PostMapping("/{id}/advance-phase")
    public ApiResponse<ProjectDto> advancePhase(@PathVariable Long id) {
        return ApiResponse.ok(projectService.advancePhase(id));
    }

    @PostMapping("/{id}/rollback-phase")
    public ApiResponse<ProjectDto> rollbackPhase(@PathVariable Long id, @Valid @RequestBody RollbackPhaseRequest body) {
        return ApiResponse.ok(projectService.rollbackPhase(id, body.reason().trim()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDto> get(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProject(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProjectDto> update(@PathVariable Long id, @RequestBody UpdateProjectRequest request) {
        return ApiResponse.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{projectId}/literatures/{literatureId}")
    public ApiResponse<ProjectDto> addLiterature(@PathVariable Long projectId, @PathVariable Long literatureId) {
        return ApiResponse.ok(projectService.addLiterature(projectId, literatureId));
    }

    @DeleteMapping("/{projectId}/literatures/{literatureId}")
    public ApiResponse<ProjectDto> removeLiterature(@PathVariable Long projectId, @PathVariable Long literatureId) {
        return ApiResponse.ok(projectService.removeLiterature(projectId, literatureId));
    }

    @PostMapping("/{projectId}/requirements/{requirementId}")
    public ApiResponse<ProjectDto> addRequirement(@PathVariable Long projectId, @PathVariable Long requirementId) {
        return ApiResponse.ok(projectService.addRequirement(projectId, requirementId));
    }

    @DeleteMapping("/{projectId}/requirements/{requirementId}")
    public ApiResponse<ProjectDto> removeRequirement(@PathVariable Long projectId, @PathVariable Long requirementId) {
        return ApiResponse.ok(projectService.removeRequirement(projectId, requirementId));
    }

    @PostMapping("/{projectId}/ethics-modules/{ethicsModuleId}")
    public ApiResponse<ProjectDto> addEthicsModule(@PathVariable Long projectId, @PathVariable Long ethicsModuleId) {
        return ApiResponse.ok(projectService.addEthicsModule(projectId, ethicsModuleId));
    }

    @DeleteMapping("/{projectId}/ethics-modules/{ethicsModuleId}")
    public ApiResponse<ProjectDto> removeEthicsModule(@PathVariable Long projectId, @PathVariable Long ethicsModuleId) {
        return ApiResponse.ok(projectService.removeEthicsModule(projectId, ethicsModuleId));
    }

    @GetMapping("/{id}/content/literatures")
    public ApiResponse<List<ProjectContentDto>> getProjectLiteratures(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProjectLiteratures(id));
    }

    @GetMapping("/{id}/content/requirements")
    public ApiResponse<List<RequirementContentDto>> getProjectRequirements(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProjectRequirements(id));
    }

    @GetMapping("/{id}/content/ethics-modules")
    public ApiResponse<List<EthicsModuleContentDto>> getProjectEthicsModules(@PathVariable Long id) {
        return ApiResponse.ok(projectService.getProjectEthicsModules(id));
    }
}