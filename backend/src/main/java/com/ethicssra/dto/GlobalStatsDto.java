package com.ethicssra.dto;

import java.util.List;
import java.util.Map;

public record GlobalStatsDto(
    long totalUsers,
    long totalStudents,
    long totalTeachers,
    long totalCourses,
    long totalProjects,
    long activeProjects,
    long completedProjects,
    ModuleUsageStats moduleUsage,
    AiUsageStats aiUsage,
    TeachingEffectivenessStats teachingEffectiveness
) {
    public record ModuleUsageStats(
        long literatureCount,
        long requirementCount,
        long ethicsModuleCount,
        long projectCount,
        Map<String, Long> literatureByCourse,
        Map<String, Long> requirementsByStatus,
        double avgRequirementsPerProject,
        double avgLiteraturesPerProject,
        double avgEthicsModulesPerProject
    ) {}

    public record AiUsageStats(
        long totalMatchEvents,
        long totalAiGenerations,
        long totalLiteratureAnalyses,
        long totalAiAnswers,
        Map<String, Long> aiActionsByUser,
        Map<String, Long> aiActionsByDay,
        double avgMatchEventsPerUser,
        double aiUsageRate
    ) {}

    public record TeachingEffectivenessStats(
        double avgSubmissionRate,
        double avgApprovalRate,
        double avgEthicsIntegrationRate,
        Map<String, Double> courseCompletionRates,
        List<CourseEffectiveness> courseEffectiveness,
        Map<String, Long> commonMistakesSummary,
        double overallQualityScore
    ) {}

    public record CourseEffectiveness(
        String courseName,
        Long courseId,
        long studentCount,
        long submissionCount,
        double completionRate,
        double ethicsIntegrationRate,
        double avgScore
    ) {}
}
