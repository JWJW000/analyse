package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.TaskChecksDto;
import com.ethicssra.dto.TaskWorkspaceDto;
import com.ethicssra.service.TaskWorkspaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskWorkspaceController {

    private final TaskWorkspaceService taskWorkspaceService;

    public TaskWorkspaceController(TaskWorkspaceService taskWorkspaceService) {
        this.taskWorkspaceService = taskWorkspaceService;
    }

    @GetMapping("/{assignmentId}/workspace")
    public ApiResponse<TaskWorkspaceDto> workspace(@PathVariable Long assignmentId) {
        try {
            return ApiResponse.ok(taskWorkspaceService.getWorkspace(assignmentId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/{assignmentId}/checks")
    public ApiResponse<TaskChecksDto> checks(@PathVariable Long assignmentId) {
        try {
            TaskWorkspaceDto workspace = taskWorkspaceService.getWorkspace(assignmentId);
            return ApiResponse.ok(new TaskChecksDto(workspace.readyForSubmission(), workspace.blockingIssues()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
