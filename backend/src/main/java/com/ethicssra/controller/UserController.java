package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.AuditLogDto;
import com.ethicssra.dto.ChangePasswordRequest;
import com.ethicssra.dto.UserOptionDto;
import com.ethicssra.dto.UserProfileDto;
import com.ethicssra.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserAdminService userAdminService;

    public UserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> me() {
        return ApiResponse.ok(userAdminService.me());
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileDto> update(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(userAdminService.updateProfile(body.get("displayName")));
    }

    @PostMapping("/me/password")
    public ApiResponse<Void> password(@Valid @RequestBody ChangePasswordRequest req) {
        try {
            userAdminService.changePassword(req);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 当前用户操作记录（来自审计日志） */
    @GetMapping("/me/activity")
    public ApiResponse<Page<AuditLogDto>> activity(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(userAdminService.myActivity(page, size));
    }

    @GetMapping("/users/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<UserOptionDto>> students(@RequestParam(required = false) String q) {
        return ApiResponse.ok(userAdminService.searchStudents(q));
    }
}
