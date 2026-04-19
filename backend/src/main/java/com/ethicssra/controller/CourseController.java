package com.ethicssra.controller;

import com.ethicssra.dto.ApiResponse;
import com.ethicssra.dto.AssignmentDto;
import com.ethicssra.dto.CourseDto;
import com.ethicssra.dto.SubmissionRowDto;
import com.ethicssra.dto.UserOptionDto;
import com.ethicssra.service.CourseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/mine")
    public ApiResponse<List<CourseDto>> mine() {
        return ApiResponse.ok(courseService.myCourses());
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<CourseDto> create(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(courseService.create(body.get("name"), body.get("code")));
    }

    @PostMapping("/{courseId}/enroll")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<Void> enroll(@PathVariable Long courseId, @RequestBody Map<String, Long> body) {
        try {
            courseService.enrollStudent(courseId, body.get("studentId"));
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/{courseId}/students")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<UserOptionDto> createStudentAndEnroll(
            @PathVariable Long courseId,
            @RequestBody Map<String, String> body
    ) {
        try {
            return ApiResponse.ok(courseService.createStudentAndEnroll(
                    courseId,
                    body.get("username"),
                    body.get("password"),
                    body.get("displayName")
            ));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/{courseId}/assignments")
    public ApiResponse<List<AssignmentDto>> assignments(@PathVariable Long courseId) {
        try {
            return ApiResponse.ok(courseService.listAssignments(courseId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/{courseId}/assignments")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<AssignmentDto> createAssignment(
            @PathVariable Long courseId,
            @RequestBody Map<String, Object> body
    ) {
        try {
            String title = String.valueOf(body.get("title"));
            String desc = body.get("description") != null ? String.valueOf(body.get("description")) : null;
            Instant due = body.get("dueAt") != null ? Instant.parse(String.valueOf(body.get("dueAt"))) : null;
            return ApiResponse.ok(courseService.createAssignment(courseId, title, desc, due));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /** 某作业下全班提交与需求关联（教师批改入口） */
    @GetMapping("/{courseId}/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    public ApiResponse<List<SubmissionRowDto>> assignmentSubmissions(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId
    ) {
        try {
            return ApiResponse.ok(courseService.listAssignmentSubmissions(courseId, assignmentId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
