package com.ethicssra.service;

import com.ethicssra.domain.*;
import com.ethicssra.dto.CommonMistakeDto;
import com.ethicssra.dto.GlobalStatsDto;
import com.ethicssra.dto.MyStatsDto;
import com.ethicssra.dto.StudentProfileDto;
import com.ethicssra.repository.*;
import com.ethicssra.util.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatsService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RequirementRepository requirementRepository;
    private final MatchEventRepository matchEventRepository;
    private final UserRepository userRepository;
    private final EthicsModuleRepository ethicsModuleRepository;
    private final LiteratureRepository literatureRepository;
    private final SubmissionRepository submissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProjectRepository projectRepository;
    private final ProjectLiteratureRepository projectLiteratureRepository;
    private final ProjectRequirementRepository projectRequirementRepository;
    private final ProjectEthicsModuleRepository projectEthicsModuleRepository;

    public StatsService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            RequirementRepository requirementRepository,
            MatchEventRepository matchEventRepository,
            UserRepository userRepository,
            EthicsModuleRepository ethicsModuleRepository,
            LiteratureRepository literatureRepository,
            SubmissionRepository submissionRepository,
            AuditLogRepository auditLogRepository,
            ProjectRepository projectRepository,
            ProjectLiteratureRepository projectLiteratureRepository,
            ProjectRequirementRepository projectRequirementRepository,
            ProjectEthicsModuleRepository projectEthicsModuleRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.requirementRepository = requirementRepository;
        this.matchEventRepository = matchEventRepository;
        this.userRepository = userRepository;
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.literatureRepository = literatureRepository;
        this.submissionRepository = submissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.projectRepository = projectRepository;
        this.projectLiteratureRepository = projectLiteratureRepository;
        this.projectRequirementRepository = projectRequirementRepository;
        this.projectEthicsModuleRepository = projectEthicsModuleRepository;
    }

    public MyStatsDto myStats() {
        Long uid = SecurityUtils.currentUserId();
        long total = requirementRepository.countByUserId(uid);
        long draft = requirementRepository.countByUserIdAndStatus(uid, RequirementStatus.DRAFT);
        long submitted = requirementRepository.countByUserIdAndStatus(uid, RequirementStatus.SUBMITTED);
        long approved = requirementRepository.countByUserIdAndStatus(uid, RequirementStatus.APPROVED);
        long matches = matchEventRepository.countByUserId(uid);
        long lit = literatureRepository.countByCreatedBy(uid);
        return new MyStatsDto(total, draft, submitted, approved, matches, lit);
    }

    public Map<String, Object> teacherCourseStats(Long courseId) {
        Course c = courseRepository.findById(courseId).orElseThrow();
        if (!c.getTeacherId().equals(SecurityUtils.currentUserId())
                && SecurityUtils.currentUser().role() != Role.ADMIN) {
            throw new IllegalArgumentException("无权查看");
        }
        List<Long> students = enrollmentRepository.findByCourseId(courseId).stream()
                .map(e -> e.getStudentId())
                .toList();
        List<Requirement> reqs = requirementRepository.findByCourseId(courseId);
        long submitted = reqs.stream()
                .filter(r -> r.getStatus() == com.ethicssra.domain.RequirementStatus.SUBMITTED
                        || r.getStatus() == com.ethicssra.domain.RequirementStatus.APPROVED)
                .count();
        List<MatchEvent> events = matchEventRepository.findAll().stream()
                .filter(m -> m.getUserId() != null && students.contains(m.getUserId()))
                .toList();
        Map<String, Object> out = new HashMap<>();
        out.put("courseName", c.getName());
        out.put("students", students.size());
        out.put("requirements", reqs.size());
        out.put("submittedOrApproved", submitted);
        out.put("matchEvents", events.size());
        Map<Long, Long> embedFreq = new HashMap<>();
        for (Requirement r : reqs) {
            if (r.getEmbeddedModules() == null) {
                continue;
            }
            for (String part : r.getEmbeddedModules().split(",")) {
                try {
                    Long id = Long.parseLong(part.trim());
                    embedFreq.merge(id, 1L, Long::sum);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        out.put("embeddedModuleFrequency", embedFreq);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (RequirementStatus rs : RequirementStatus.values()) {
            long cnt = reqs.stream().filter(r -> r.getStatus() == rs).count();
            statusCounts.put(rs.name(), cnt);
        }
        out.put("requirementStatusCounts", statusCounts);

        List<Map<String, Object>> studentProgress = new ArrayList<>();
        for (Long sid : students) {
            User u = userRepository.findById(sid).orElse(null);
            String name = u == null
                    ? "?"
                    : (u.getDisplayName() != null && !u.getDisplayName().isBlank()
                    ? u.getDisplayName()
                    : u.getUsername());
            List<Requirement> mine = reqs.stream().filter(r -> r.getUserId().equals(sid)).toList();
            long withEmbed = mine.stream()
                    .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                    .count();
            long subApp = mine.stream()
                    .filter(r -> r.getStatus() == RequirementStatus.SUBMITTED
                            || r.getStatus() == RequirementStatus.APPROVED)
                    .count();
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", sid);
            row.put("studentName", name);
            row.put("requirementCount", mine.size());
            row.put("withEmbeddingCount", withEmbed);
            row.put("submittedOrApprovedCount", subApp);
            studentProgress.add(row);
        }
        studentProgress.sort(Comparator.comparing(m -> String.valueOf(m.get("studentName"))));
        out.put("studentProgress", studentProgress);
        return out;
    }

    public Map<String, Object> globalStats() {
        Map<String, Object> out = new HashMap<>();
        out.put("users", userRepository.count());
        out.put("requirements", requirementRepository.count());
        out.put("ethicsModules", ethicsModuleRepository.count());
        out.put("matchEvents", matchEventRepository.count());
        return out;
    }

    public GlobalStatsDto getGlobalStats() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.findAll().stream().filter(u -> u.getRole() == Role.STUDENT).count();
        long totalTeachers = userRepository.findAll().stream().filter(u -> u.getRole() == Role.TEACHER).count();
        long totalCourses = courseRepository.count();
        long totalProjects = projectRepository.count();
        long activeProjects = projectRepository.findAll().stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
        long completedProjects = projectRepository.findAll().stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();

        GlobalStatsDto.ModuleUsageStats moduleUsage = calculateModuleUsage();
        GlobalStatsDto.AiUsageStats aiUsage = calculateAiUsage();
        GlobalStatsDto.TeachingEffectivenessStats teachingEffectiveness = calculateTeachingEffectiveness();

        return new GlobalStatsDto(
            totalUsers, totalStudents, totalTeachers, totalCourses,
            totalProjects, activeProjects, completedProjects,
            moduleUsage, aiUsage, teachingEffectiveness
        );
    }

    private GlobalStatsDto.ModuleUsageStats calculateModuleUsage() {
        long literatureCount = literatureRepository.count();
        long requirementCount = requirementRepository.count();
        long ethicsModuleCount = ethicsModuleRepository.count();
        long projectCount = projectRepository.count();

        Map<String, Long> literatureByCourse = new LinkedHashMap<>();
        courseRepository.findAll().forEach(c -> {
            long count = projectLiteratureRepository.findAll().stream()
                .filter(pl -> pl.getProject() != null && c.getId().equals(pl.getProject().getCourseId()))
                .count();
            if (count > 0) literatureByCourse.put(c.getName(), count);
        });

        Map<String, Long> requirementsByStatus = new LinkedHashMap<>();
        for (RequirementStatus rs : RequirementStatus.values()) {
            long cnt = requirementRepository.findAll().stream().filter(r -> r.getStatus() == rs).count();
            requirementsByStatus.put(rs.name(), cnt);
        }

        List<Project> projects = projectRepository.findAll();
        double avgRequirementsPerProject = projects.isEmpty() ? 0 : (double) projectRequirementRepository.findAll().size() / projects.size();
        double avgLiteraturesPerProject = projects.isEmpty() ? 0 : (double) projectLiteratureRepository.findAll().size() / projects.size();
        double avgEthicsModulesPerProject = projects.isEmpty() ? 0 : (double) projectEthicsModuleRepository.findAll().size() / projects.size();

        return new GlobalStatsDto.ModuleUsageStats(
            literatureCount, requirementCount, ethicsModuleCount, projectCount,
            literatureByCourse, requirementsByStatus,
            avgRequirementsPerProject, avgLiteraturesPerProject, avgEthicsModulesPerProject
        );
    }

    private GlobalStatsDto.AiUsageStats calculateAiUsage() {
        long totalMatchEvents = matchEventRepository.count();
        long totalAiGenerations = auditLogRepository.findAll().stream()
            .filter(log -> log.getAction() != null && log.getAction().startsWith("AI_")).count();
        long totalLiteratureAnalyses = auditLogRepository.findAll().stream()
            .filter(log -> "LITERATURE_ANALYZE".equals(log.getAction())).count();
        long totalAiAnswers = auditLogRepository.findAll().stream()
            .filter(log -> "AI_ANSWER".equals(log.getAction())).count();

        Map<String, Long> aiActionsByUser = new LinkedHashMap<>();
        auditLogRepository.findAll().stream()
            .filter(log -> log.getAction() != null && log.getAction().startsWith("AI_"))
            .forEach(log -> {
                String key = log.getUserId() != null ? "用户" + log.getUserId() : "匿名";
                aiActionsByUser.merge(key, 1L, Long::sum);
            });

        Map<String, Long> aiActionsByDay = new LinkedHashMap<>();
        auditLogRepository.findAll().stream()
            .filter(log -> log.getAction() != null && log.getAction().startsWith("AI_"))
            .filter(log -> log.getCreatedAt() != null)
            .forEach(log -> {
                String day = log.getCreatedAt().toString().substring(0, 10);
                aiActionsByDay.merge(day, 1L, Long::sum);
            });

        long totalUsers = userRepository.count();
        double avgMatchEventsPerUser = totalUsers > 0 ? (double) totalMatchEvents / totalUsers : 0;
        double aiUsageRate = totalUsers > 0 ? (double) (totalAiGenerations + totalMatchEvents) / totalUsers / 10 : 0;
        aiUsageRate = Math.min(100, aiUsageRate * 100);

        return new GlobalStatsDto.AiUsageStats(
            totalMatchEvents, totalAiGenerations, totalLiteratureAnalyses, totalAiAnswers,
            aiActionsByUser, aiActionsByDay, avgMatchEventsPerUser, aiUsageRate
        );
    }

    private GlobalStatsDto.TeachingEffectivenessStats calculateTeachingEffectiveness() {
        List<Course> courses = courseRepository.findAll();
        List<Submission> allSubmissions = submissionRepository.findAll();
        List<Requirement> allRequirements = requirementRepository.findAll();

        double avgSubmissionRate = 0;
        double avgApprovalRate = 0;
        double avgEthicsIntegrationRate = 0;

        if (!allRequirements.isEmpty()) {
            long submitted = allRequirements.stream()
                .filter(r -> r.getStatus() == RequirementStatus.SUBMITTED || r.getStatus() == RequirementStatus.APPROVED)
                .count();
            avgSubmissionRate = (double) submitted / allRequirements.size() * 100;

            long approved = allRequirements.stream()
                .filter(r -> r.getStatus() == RequirementStatus.APPROVED)
                .count();
            avgApprovalRate = (double) approved / allRequirements.size() * 100;

            long withEthics = allRequirements.stream()
                .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                .count();
            avgEthicsIntegrationRate = (double) withEthics / allRequirements.size() * 100;
        }

        Map<String, Double> courseCompletionRates = new LinkedHashMap<>();
        List<GlobalStatsDto.CourseEffectiveness> courseEffectivenessList = new ArrayList<>();
        Map<String, Long> commonMistakesSummary = new LinkedHashMap<>();

        long totalMistakes = allRequirements.stream()
            .filter(r -> r.getTextContent() == null || r.getTextContent().length() < 100)
            .count();
        commonMistakesSummary.put("需求不完整", totalMistakes);

        long noDiagram = allRequirements.stream()
            .filter(r -> r.getDiagramJson() == null || r.getDiagramJson().isBlank())
            .count();
        commonMistakesSummary.put("缺少用例图", noDiagram);

        long noEthics = allRequirements.stream()
            .filter(r -> r.getEmbeddedModules() == null || r.getEmbeddedModules().isBlank())
            .count();
        commonMistakesSummary.put("思政融合缺失", noEthics);

        for (Course course : courses) {
            List<Long> studentIds = enrollmentRepository.findByCourseId(course.getId()).stream()
                .map(Enrollment::getStudentId).toList();
            List<Requirement> courseReqs = requirementRepository.findByCourseId(course.getId());

            double completionRate = 0;
            if (!studentIds.isEmpty()) {
                long completed = courseReqs.stream()
                    .filter(r -> r.getStatus() == RequirementStatus.APPROVED)
                    .count();
                completionRate = (double) completed / studentIds.size() * 100;
            }
            courseCompletionRates.put(course.getName(), completionRate);

            List<Submission> courseSubs = allSubmissions.stream()
                .filter(s -> studentIds.contains(s.getStudentId()))
                .toList();
            double avgScore = courseSubs.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .average()
                .orElse(0);

            long courseWithEthics = courseReqs.stream()
                .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                .count();
            double ethicsRate = courseReqs.isEmpty() ? 0 : (double) courseWithEthics / courseReqs.size() * 100;

            courseEffectivenessList.add(new GlobalStatsDto.CourseEffectiveness(
                course.getName(), course.getId(), studentIds.size(), courseReqs.size(),
                completionRate, ethicsRate, avgScore
            ));
        }

        double overallQualityScore = (avgSubmissionRate * 0.3 + avgApprovalRate * 0.3 + avgEthicsIntegrationRate * 0.4);

        return new GlobalStatsDto.TeachingEffectivenessStats(
            avgSubmissionRate, avgApprovalRate, avgEthicsIntegrationRate,
            courseCompletionRates, courseEffectivenessList, commonMistakesSummary, overallQualityScore
        );
    }

    public StudentProfileDto getStudentProfile(Long studentId, Long courseId) {
        User student = userRepository.findById(studentId).orElseThrow();
        String studentName = student.getDisplayName() != null ? student.getDisplayName() : student.getUsername();

        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        List<Requirement> requirements = requirementRepository.findByUserId(studentId);

        double avgScore = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToDouble(Submission::getScore)
                .average()
                .orElse(0.0);

        double ethicsScore = calculateEthicsScore(requirements);
        double ethicsQualityScore = calculateEthicsQualityScore(requirements);

        List<CommonMistakeDto> commonMistakes = identifyCommonMistakes(requirements);

        Map<String, Double> abilities = Map.of(
                "需求分析", calculateRequirementAbility(requirements),
                "文档撰写", calculateDocAbility(requirements),
                "思政融合", ethicsScore,
                "创新能力", calculateInnovationAbility(requirements),
                "团队协作", calculateCollaborationAbility(requirements)
        );

        return new StudentProfileDto(
                studentId,
                studentName,
                avgScore,
                ethicsScore,
                ethicsQualityScore,
                commonMistakes,
                abilities,
                requirements.size(),
                (int) submissions.stream().filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED || s.getStatus() == SubmissionStatus.APPROVED).count()
        );
    }

    public List<StudentProfileDto> getCourseStudentProfiles(Long courseId) {
        List<Long> studentIds = enrollmentRepository.findByCourseId(courseId).stream()
                .map(Enrollment::getStudentId)
                .toList();

        return studentIds.stream()
                .map(id -> getStudentProfile(id, courseId))
                .toList();
    }

    public Map<String, Object> getCourseAnalytics(Long courseId) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        
        List<Long> studentIds = enrollmentRepository.findByCourseId(courseId).stream()
                .map(Enrollment::getStudentId)
                .toList();

        List<Requirement> requirements = requirementRepository.findByCourseId(courseId);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("courseName", course.getName());
        analytics.put("totalStudents", studentIds.size());
        analytics.put("totalRequirements", requirements.size());
        analytics.put("avgRequirementsPerStudent", studentIds.isEmpty() ? 0 : (double) requirements.size() / studentIds.size());

        long withEmbed = requirements.stream()
                .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                .count();
        analytics.put("requirementsWithEthics", withEmbed);
        analytics.put("ethicsIntegrationRate", requirements.isEmpty() ? 0 : (double) withEmbed / requirements.size() * 100);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (RequirementStatus rs : RequirementStatus.values()) {
            statusCounts.put(rs.name(), requirements.stream().filter(r -> r.getStatus() == rs).count());
        }
        analytics.put("requirementStatusCounts", statusCounts);

        List<CommonMistakeDto> commonMistakes = identifyCommonMistakes(requirements);
        analytics.put("commonMistakes", commonMistakes);

        return analytics;
    }

    private double calculateEthicsScore(List<Requirement> requirements) {
        if (requirements.isEmpty()) return 0.0;
        
        long withEmbed = requirements.stream()
                .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                .count();
        
        return (double) withEmbed / requirements.size() * 100;
    }

    private double calculateEthicsQualityScore(List<Requirement> requirements) {
        if (requirements.isEmpty()) return 0.0;
        
        double integrationRate = calculateEthicsScore(requirements);
        
        long multipleModules = requirements.stream()
                .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                .filter(r -> r.getEmbeddedModules().split(",").length >= 2)
                .count();
        
        double multiModuleRate = (double) multipleModules / requirements.size() * 100;
        
        long hasContent = requirements.stream()
                .filter(r -> r.getEmbeddedModules() != null && !r.getEmbeddedModules().isBlank())
                .filter(r -> {
                    String[] modules = r.getEmbeddedModules().split(",");
                    for (String m : modules) {
                        if (m.trim().length() > 0) return true;
                    }
                    return false;
                })
                .count();
        
        double hasContentRate = (double) hasContent / requirements.size() * 100;
        
        double qualityScore = integrationRate * 0.4 + multiModuleRate * 0.3 + hasContentRate * 0.3;
        
        return Math.round(qualityScore * 100) / 100.0;
    }

    private List<CommonMistakeDto> identifyCommonMistakes(List<Requirement> requirements) {
        List<CommonMistakeDto> mistakes = new ArrayList<>();

        long incompleteReqs = requirements.stream()
                .filter(r -> r.getTextContent() == null || r.getTextContent().length() < 100)
                .count();
        if (incompleteReqs > 0) {
            mistakes.add(new CommonMistakeDto(
                    "需求不完整",
                    "有 " + incompleteReqs + " 个需求文档内容过短或为空",
                    "建议补充完整的项目背景、功能需求描述和非功能需求",
                    (int) incompleteReqs
            ));
        }

        long noDiagram = requirements.stream()
                .filter(r -> r.getDiagramJson() == null || r.getDiagramJson().isBlank())
                .count();
        if (noDiagram > 0) {
            mistakes.add(new CommonMistakeDto(
                    "缺少用例图",
                    "有 " + noDiagram + " 个需求文档缺少用例图",
                    "建议使用系统提供的用例图工具绘制基本用例",
                    (int) noDiagram
            ));
        }

        long noEthics = requirements.stream()
                .filter(r -> r.getEmbeddedModules() == null || r.getEmbeddedModules().isBlank())
                .count();
        if (noEthics > 0) {
            mistakes.add(new CommonMistakeDto(
                    "思政融合缺失",
                    "有 " + noEthics + " 个需求文档未关联思政模块",
                    "建议使用AI思政推荐功能选择合适的思政模块",
                    (int) noEthics
            ));
        }

        return mistakes;
    }

    private double calculateRequirementAbility(List<Requirement> requirements) {
        if (requirements.isEmpty()) return 50.0;
        
        double completeness = requirements.stream()
                .filter(r -> r.getTextContent() != null && r.getTextContent().length() >= 200)
                .count() * 100.0 / requirements.size();
        
        double hasDiagram = requirements.stream()
                .filter(r -> r.getDiagramJson() != null && !r.getDiagramJson().isBlank())
                .count() * 100.0 / requirements.size();
        
        return (completeness * 0.6 + hasDiagram * 0.4);
    }

    private double calculateDocAbility(List<Requirement> requirements) {
        if (requirements.isEmpty()) return 50.0;
        
        double avgLength = requirements.stream()
                .filter(r -> r.getTextContent() != null)
                .mapToInt(r -> r.getTextContent().length())
                .average()
                .orElse(0);
        
        return Math.min(100, avgLength / 5);
    }

    private double calculateInnovationAbility(List<Requirement> requirements) {
        return 60.0;
    }

    private double calculateCollaborationAbility(List<Requirement> requirements) {
        return 55.0;
    }
}
