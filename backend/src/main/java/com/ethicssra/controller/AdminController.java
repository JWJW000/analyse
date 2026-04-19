package com.ethicssra.controller;

import com.ethicssra.domain.BackupRecord;
import com.ethicssra.domain.Role;
import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.UserProfileDto;
import com.ethicssra.repository.AuditLogRepository;
import com.ethicssra.service.BackupService;
import com.ethicssra.service.SystemConfigService;
import com.ethicssra.service.UserAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAdminService userAdminService;
    private final BackupService backupService;
    private final SystemConfigService systemConfigService;
    private final AuditLogRepository auditLogRepository;

    @Value("${logging.file.name:./logs/ethics-sra.log}")
    private String logFile;

    public AdminController(
            UserAdminService userAdminService,
            BackupService backupService,
            SystemConfigService systemConfigService,
            AuditLogRepository auditLogRepository
    ) {
        this.userAdminService = userAdminService;
        this.backupService = backupService;
        this.systemConfigService = systemConfigService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserProfileDto>> users() {
        return ApiResponse.ok(userAdminService.listAll());
    }

    @PostMapping("/users")
    public ApiResponse<UserProfileDto> createUser(@RequestBody Map<String, String> body) {
        try {
            Role role = Role.valueOf(body.get("role"));
            return ApiResponse.ok(userAdminService.createUser(
                    body.get("username"),
                    body.get("password"),
                    role,
                    body.get("displayName")
            ));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            userAdminService.resetPassword(id, body.get("newPassword"));
            return ApiResponse.ok(null);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/backup")
    public ApiResponse<BackupRecord> backup() {
        try {
            return ApiResponse.ok(backupService.runMysqlDump());
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/backups")
    public ApiResponse<List<BackupRecord>> backups() {
        return ApiResponse.ok(backupService.list());
    }

    @GetMapping("/logs")
    public ApiResponse<List<String>> logs(@RequestParam(defaultValue = "200") int lines) {
        try {
            return ApiResponse.ok(backupService.tailLog(logFile, lines));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, String>> getConfig() {
        return ApiResponse.ok(systemConfigService.all());
    }

    @PutMapping("/config")
    public ApiResponse<Void> putConfig(@RequestBody Map<String, String> body) {
        body.forEach(systemConfigService::put);
        return ApiResponse.ok(null);
    }

    @GetMapping("/audit")
    public ApiResponse<?> audit(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)));
    }
}
