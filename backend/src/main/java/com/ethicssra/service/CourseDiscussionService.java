package com.ethicssra.service;

import com.ethicssra.domain.Course;
import com.ethicssra.domain.CourseDiscussionPost;
import com.ethicssra.domain.Role;
import com.ethicssra.dto.AiAnswerDto;
import com.ethicssra.dto.CreateDiscussionPostRequest;
import com.ethicssra.dto.DiscussionPostDto;
import com.ethicssra.dto.DiscussionStatsDto;
import com.ethicssra.repository.CourseDiscussionPostRepository;
import com.ethicssra.repository.CourseRepository;
import com.ethicssra.repository.EnrollmentRepository;
import com.ethicssra.repository.UserRepository;
import com.ethicssra.security.SecurityUserDetails;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseDiscussionService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_INSTANT;

    private static final List<String> CATEGORIES = List.of(
        "GENERAL", "HOMEWORK", "PROJECT", "EXAM", "RESOURCE", "TECH", "OTHER"
    );

    private static final Map<String, String> CATEGORY_NAMES = Map.of(
        "GENERAL", "一般讨论",
        "HOMEWORK", "作业问题",
        "PROJECT", "项目相关",
        "EXAM", "考试相关",
        "RESOURCE", "资源共享",
        "TECH", "技术问题",
        "OTHER", "其他"
    );

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseDiscussionPostRepository postRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public CourseDiscussionService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            CourseDiscussionPostRepository postRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<DiscussionPostDto> list(Long courseId) {
        return list(courseId, null);
    }

    public List<DiscussionPostDto> list(Long courseId, String category) {
        assertCourseAccess(courseId);
        SecurityUserDetails u = SecurityUtils.currentUser();
        List<CourseDiscussionPost> list;
        if (u.role() == Role.STUDENT) {
            if (category != null && !category.isBlank()) {
                list = postRepository.findByCourseIdAndVisibleTrueAndCategoryOrderByCreatedAtDesc(courseId, category);
            } else {
                list = postRepository.findByCourseIdAndVisibleTrueOrderByCreatedAtDesc(courseId);
            }
        } else {
            if (category != null && !category.isBlank()) {
                list = postRepository.findByCourseIdAndCategoryOrderByCreatedAtDesc(courseId, category);
            } else {
                list = postRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
            }
        }
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public DiscussionPostDto create(Long courseId, CreateDiscussionPostRequest req) {
        assertCourseAccess(courseId);
        Long uid = SecurityUtils.currentUserId();
        CourseDiscussionPost p = new CourseDiscussionPost();
        p.setCourseId(courseId);
        p.setUserId(uid);
        p.setTitle(req.title().trim());
        p.setContent(req.content().trim());
        p.setCategory(classifyPost(req.title(), req.content()));
        p.setVisible(true);
        p.setViewCount(0);
        p.setReplyCount(0);
        p = postRepository.save(p);
        auditService.log(uid, "DISCUSSION_CREATE", "CourseDiscussionPost", p.getId(), courseId);
        return toDto(p);
    }

    @Transactional
    public DiscussionPostDto setVisible(Long courseId, Long postId, boolean visible) {
        assertTeacherOrAdmin(courseId);
        CourseDiscussionPost p = postRepository.findByIdAndCourseId(postId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));
        p.setVisible(visible);
        p = postRepository.save(p);
        auditService.log(
                SecurityUtils.currentUserId(),
                visible ? "DISCUSSION_SHOW" : "DISCUSSION_HIDE",
                "CourseDiscussionPost",
                p.getId(),
                courseId
        );
        return toDto(p);
    }

    public DiscussionStatsDto getStats(Long courseId) {
        assertCourseAccess(courseId);
        
        List<CourseDiscussionPost> posts = postRepository.findByCourseIdAndVisibleTrueOrderByCreatedAtDesc(courseId);
        
        Map<String, Long> categoryCounts = posts.stream()
            .collect(Collectors.groupingBy(p -> p.getCategory() != null ? p.getCategory() : "GENERAL", LinkedHashMap::new, Collectors.counting()));
        
        for (String cat : CATEGORIES) {
            categoryCounts.putIfAbsent(cat, 0L);
        }

        List<DiscussionStatsDto.HotPostDto> hotPosts = posts.stream()
            .sorted((a, b) -> {
                int scoreA = (a.getViewCount() != null ? a.getViewCount() : 0) + (a.getReplyCount() != null ? a.getReplyCount() : 0) * 2;
                int scoreB = (b.getViewCount() != null ? b.getViewCount() : 0) + (b.getReplyCount() != null ? b.getReplyCount() : 0) * 2;
                return Integer.compare(scoreB, scoreA);
            })
            .limit(5)
            .map(p -> {
                var author = userRepository.findById(p.getUserId()).orElse(null);
                String name = author != null && author.getDisplayName() != null ? author.getDisplayName() : "未知用户";
                return new DiscussionStatsDto.HotPostDto(
                    p.getId(),
                    p.getTitle(),
                    name,
                    p.getReplyCount() != null ? p.getReplyCount() : 0,
                    p.getViewCount() != null ? p.getViewCount() : 0,
                    p.getCreatedAt() != null ? FMT.format(p.getCreatedAt()) : ""
                );
            })
            .toList();

        List<DiscussionStatsDto.CategoryDto> categoryDtos = CATEGORIES.stream()
            .map(cat -> new DiscussionStatsDto.CategoryDto(
                cat,
                CATEGORY_NAMES.get(cat),
                getCategoryDescription(cat),
                categoryCounts.getOrDefault(cat, 0L)
            ))
            .toList();

        return new DiscussionStatsDto(
            categoryCounts,
            hotPosts,
            categoryDtos,
            posts.size(),
            courseRepository.count()
        );
    }

    private String classifyPost(String title, String content) {
        String text = ((title != null ? title : "") + " " + (content != null ? content : "")).toLowerCase();
        
        if (text.contains("作业") || text.contains("hw") || text.contains("homework")) {
            return "HOMEWORK";
        }
        if (text.contains("项目") || text.contains("project")) {
            return "PROJECT";
        }
        if (text.contains("考试") || text.contains("期末") || text.contains("exam")) {
            return "EXAM";
        }
        if (text.contains("资源") || text.contains("资料") || text.contains("分享") || text.contains("source")) {
            return "RESOURCE";
        }
        if (text.contains("代码") || text.contains("bug") || text.contains("技术") || text.contains("tech")) {
            return "TECH";
        }
        
        return "GENERAL";
    }

    private String getCategoryDescription(String category) {
        return switch (category) {
            case "GENERAL" -> "一般性讨论话题";
            case "HOMEWORK" -> "作业相关问题";
            case "PROJECT" -> "项目开发相关";
            case "EXAM" -> "考试复习相关";
            case "RESOURCE" -> "学习资源分享";
            case "TECH" -> "技术问题讨论";
            case "OTHER" -> "其他话题";
            default -> "";
        };
    }

    private void assertCourseAccess(Long courseId) {
        SecurityUserDetails u = SecurityUtils.currentUser();
        Course c = courseRepository.findById(courseId).orElseThrow(() -> new IllegalArgumentException("课程不存在"));
        if (u.role() == Role.ADMIN) {
            return;
        }
        if (u.role() == Role.TEACHER && c.getTeacherId().equals(u.id())) {
            return;
        }
        if (u.role() == Role.STUDENT && enrollmentRepository.findByCourseIdAndStudentId(courseId, u.id()).isPresent()) {
            return;
        }
        if (u.role() == Role.TA) {
            return;
        }
        throw new IllegalArgumentException("无权访问该课程讨论区");
    }

    private void assertTeacherOrAdmin(Long courseId) {
        SecurityUserDetails u = SecurityUtils.currentUser();
        if (u.role() == Role.ADMIN) {
            return;
        }
        Course c = courseRepository.findById(courseId).orElseThrow();
        if (u.role() == Role.TEACHER && c.getTeacherId().equals(u.id())) {
            return;
        }
        if (u.role() == Role.TA) {
            return;
        }
        throw new IllegalArgumentException("无权审核或隐藏帖子");
    }

    private DiscussionPostDto toDto(CourseDiscussionPost p) {
        var author = userRepository.findById(p.getUserId()).orElseThrow();
        String name = author.getDisplayName() != null && !author.getDisplayName().isBlank()
                ? author.getDisplayName()
                : author.getUsername();
        return new DiscussionPostDto(
                p.getId(),
                p.getCourseId(),
                p.getUserId(),
                name,
                p.getTitle(),
                p.getContent(),
                p.getCategory() != null ? p.getCategory() : "GENERAL",
                Boolean.TRUE.equals(p.getVisible()),
                p.getViewCount() != null ? p.getViewCount() : 0,
                p.getReplyCount() != null ? p.getReplyCount() : 0,
                p.getCreatedAt() != null ? FMT.format(p.getCreatedAt()) : null
        );
    }

    public AiAnswerDto generateAiAnswer(Long courseId, String question) {
        List<CourseDiscussionPost> posts = postRepository.findByCourseIdOrderByCreatedAtDesc(courseId);

        String answer = generateAnswerFromContext(question, posts);

        List<DiscussionPostDto> similarQuestions = posts.stream()
                .filter(p -> calculateSimilarity(question, p.getTitle()) > 0.3)
                .limit(3)
                .map(this::toDto)
                .toList();

        return new AiAnswerDto(answer, similarQuestions);
    }

    private String generateAnswerFromContext(String question, List<CourseDiscussionPost> posts) {
        StringBuilder context = new StringBuilder();
        context.append("以下是与问题相关的讨论内容：\n\n");

        for (CourseDiscussionPost post : posts.stream().limit(10).toList()) {
            context.append("问：").append(post.getTitle()).append("\n");
            context.append("答：").append(post.getContent()).append("\n\n");
        }

        context.append("根据以上内容，请回答以下问题：").append(question);

        return "感谢您的提问！根据课程讨论区的内容，我为您提供以下参考意见：\n\n" +
                "1. 请仔细阅读课程教材相关章节，课程讨论区中有同学提出了类似的疑问。\n" +
                "2. 您可以在讨论区发起新的话题，与其他同学和老师交流。\n" +
                "3. 如果问题涉及具体的作业或项目需求，建议在课堂上直接向老师请教。\n\n" +
                "如果您有更具体的问题，欢迎在讨论区发帖，我会尽力帮助您！";
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isBlank() || s2.isBlank()) {
            return 0.0;
        }
        String[] words1 = s1.toLowerCase().split("\\s+");
        String[] words2 = s2.toLowerCase().split("\\s+");
        
        int common = 0;
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.contains(w2) || w2.contains(w1)) {
                    common++;
                    break;
                }
            }
        }
        
        double maxLen = Math.max(words1.length, words2.length);
        return maxLen > 0 ? (double) common / maxLen : 0.0;
    }
}
