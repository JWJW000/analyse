package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.GlobalStatsDto;
import com.ethicssra.dto.MyStatsDto;
import com.ethicssra.dto.StudentProfileDto;
import com.ethicssra.service.StatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/class/{courseId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','TA')")
    public ApiResponse<Map<String, Object>> classStats(@PathVariable Long courseId) {
        try {
            return ApiResponse.ok(statsService.teacherCourseStats(courseId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> global() {
        return ApiResponse.ok(statsService.globalStats());
    }

    @GetMapping("/global/detailed")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GlobalStatsDto> globalDetailed() {
        return ApiResponse.ok(statsService.getGlobalStats());
    }

    @GetMapping("/me")
    public ApiResponse<MyStatsDto> me() {
        return ApiResponse.ok(statsService.myStats());
    }

    @GetMapping("/course/{courseId}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','TA')")
    public ApiResponse<List<StudentProfileDto>> courseStudentProfiles(@PathVariable Long courseId) {
        return ApiResponse.ok(statsService.getCourseStudentProfiles(courseId));
    }

    @GetMapping("/course/{courseId}/student/{studentId}")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','TA')")
    public ApiResponse<StudentProfileDto> studentProfile(@PathVariable Long courseId, @PathVariable Long studentId) {
        return ApiResponse.ok(statsService.getStudentProfile(studentId, courseId));
    }

    @GetMapping("/course/{courseId}/analytics")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','TA')")
    public ApiResponse<Map<String, Object>> courseAnalytics(@PathVariable Long courseId) {
        return ApiResponse.ok(statsService.getCourseAnalytics(courseId));
    }
}
