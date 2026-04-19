package com.ethicssra.service;

import com.ethicssra.domain.Assignment;
import com.ethicssra.domain.Course;
import com.ethicssra.domain.Enrollment;
import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.Role;
import com.ethicssra.domain.Submission;
import com.ethicssra.domain.User;
import com.ethicssra.dto.AssignmentDto;
import com.ethicssra.dto.CourseDto;
import com.ethicssra.dto.SubmissionRowDto;
import com.ethicssra.dto.UserOptionDto;
import com.ethicssra.repository.AssignmentRepository;
import com.ethicssra.repository.CourseRepository;
import com.ethicssra.repository.EnrollmentRepository;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.repository.SubmissionRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.util.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CourseService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final RequirementRepository requirementRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            SubmissionRepository submissionRepository,
            RequirementRepository requirementRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.requirementRepository = requirementRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public List<SubmissionRowDto> listAssignmentSubmissions(Long courseId, Long assignmentId) {
        assertCourseAccess(courseId);
        Assignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("作业不存在"));
        if (!a.getCourseId().equals(courseId)) {
            throw new IllegalArgumentException("作业不属于该课程");
        }
        if (SecurityUtils.currentUser().role() == Role.TEACHER) {
            assertTeacherOfCourse(courseId);
        }
        List<Submission> subs = submissionRepository.findByAssignmentId(assignmentId);
        List<SubmissionRowDto> rows = new ArrayList<>();
        for (Submission s : subs) {
            Requirement req = requirementRepository.findById(s.getRequirementId()).orElseThrow();
            User student = userRepository.findById(s.getStudentId()).orElseThrow();
            String name = student.getDisplayName() != null ? student.getDisplayName() : student.getUsername();
            rows.add(new SubmissionRowDto(
                    s.getId(),
                    s.getStudentId(),
                    name,
                    req.getId(),
                    req.getTitle(),
                    s.getStatus().name(),
                    s.getSubmittedAt() != null ? FMT.format(s.getSubmittedAt()) : null,
                    s.getTeacherComment()
            ));
        }
        return rows;
    }

    public List<CourseDto> myCourses() {
        var u = SecurityUtils.currentUser();
        if (u.role() == Role.TEACHER) {
            return courseRepository.findByTeacherId(u.id()).stream().map(this::toCourseDto).toList();
        }
        if (u.role() == Role.STUDENT) {
            return enrollmentRepository.findByStudentId(u.id()).stream()
                    .map(e -> courseRepository.findById(e.getCourseId()).orElseThrow())
                    .map(this::toCourseDto)
                    .toList();
        }
        return courseRepository.findAll().stream().map(this::toCourseDto).toList();
    }

    @Transactional
    public CourseDto create(String name, String code) {
        Long tid = SecurityUtils.currentUserId();
        Course c = new Course();
        c.setName(name);
        c.setCode(code);
        c.setTeacherId(tid);
        c = courseRepository.save(c);
        auditService.log(tid, "COURSE_CREATE", "Course", c.getId(), null);
        return toCourseDto(c);
    }

    @Transactional
    public void enrollStudent(Long courseId, Long studentId) {
        if (!courseRepository.findById(courseId).map(co -> co.getTeacherId().equals(SecurityUtils.currentUserId())).orElse(false)
                && SecurityUtils.currentUser().role() != Role.ADMIN) {
            throw new IllegalArgumentException("无权操作");
        }
        if (enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId).isPresent()) {
            return;
        }
        Enrollment e = new Enrollment();
        e.setCourseId(courseId);
        e.setStudentId(studentId);
        enrollmentRepository.save(e);
        auditService.log(SecurityUtils.currentUserId(), "COURSE_ENROLL", "Course", courseId, java.util.Map.of("studentId", studentId));
    }

    @Transactional
    public UserOptionDto createStudentAndEnroll(Long courseId, String username, String password, String displayName) {
        assertTeacherOfCourse(courseId);
        String u = username != null ? username.trim() : "";
        String p = password != null ? password : "";
        if (u.length() < 3) {
            throw new IllegalArgumentException("用户名至少 3 位");
        }
        if (p.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (userRepository.existsByUsername(u)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User student = new User();
        student.setUsername(u);
        student.setPassword(passwordEncoder.encode(p));
        student.setRole(Role.STUDENT);
        student.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : u);
        student = userRepository.save(student);
        enrollStudent(courseId, student.getId());
        auditService.log(SecurityUtils.currentUserId(), "COURSE_STUDENT_CREATE", "Course", courseId,
                java.util.Map.of("studentId", student.getId()));
        return new UserOptionDto(student.getId(), student.getUsername(), student.getDisplayName(), student.getRole().name());
    }

    public List<AssignmentDto> listAssignments(Long courseId) {
        assertCourseAccess(courseId);
        return assignmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(this::toAssignDto)
                .toList();
    }

    @Transactional
    public AssignmentDto createAssignment(Long courseId, String title, String description, Instant dueAt) {
        assertTeacherOfCourse(courseId);
        Assignment a = new Assignment();
        a.setCourseId(courseId);
        a.setTitle(title);
        a.setDescription(description);
        a.setDueAt(dueAt);
        a.setCreatedBy(SecurityUtils.currentUserId());
        a = assignmentRepository.save(a);
        auditService.log(SecurityUtils.currentUserId(), "ASSIGNMENT_CREATE", "Assignment", a.getId(), null);
        return toAssignDto(a);
    }

    private void assertCourseAccess(Long courseId) {
        var u = SecurityUtils.currentUser();
        Course c = courseRepository.findById(courseId).orElseThrow();
        if (u.role() == Role.ADMIN) {
            return;
        }
        if (u.role() == Role.TEACHER && c.getTeacherId().equals(u.id())) {
            return;
        }
        if (u.role() == Role.STUDENT && enrollmentRepository.findByCourseIdAndStudentId(courseId, u.id()).isPresent()) {
            return;
        }
        throw new IllegalArgumentException("无权访问课程");
    }

    private void assertTeacherOfCourse(Long courseId) {
        Course c = courseRepository.findById(courseId).orElseThrow();
        if (!c.getTeacherId().equals(SecurityUtils.currentUserId()) && SecurityUtils.currentUser().role() != Role.ADMIN) {
            throw new IllegalArgumentException("无权操作");
        }
    }

    private CourseDto toCourseDto(Course c) {
        return new CourseDto(c.getId(), c.getName(), c.getCode(), c.getTeacherId(),
                c.getCreatedAt() != null ? FMT.format(c.getCreatedAt()) : null);
    }

    private AssignmentDto toAssignDto(Assignment a) {
        return new AssignmentDto(
                a.getId(),
                a.getCourseId(),
                a.getTitle(),
                a.getDescription(),
                a.getDueAt() != null ? FMT.format(a.getDueAt()) : null,
                a.getCreatedBy(),
                a.getCreatedAt() != null ? FMT.format(a.getCreatedAt()) : null
        );
    }
}
