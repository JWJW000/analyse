package com.ethicssra.service;

import com.ethicssra.domain.Requirement;
import com.ethicssra.domain.Submission;
import com.ethicssra.dto.AiGradingDto;
import com.ethicssra.repository.RequirementRepository;
import com.ethicssra.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiGradingService {

    private final SubmissionRepository submissionRepository;
    private final RequirementRepository requirementRepository;

    public AiGradingService(SubmissionRepository submissionRepository, RequirementRepository requirementRepository) {
        this.submissionRepository = submissionRepository;
        this.requirementRepository = requirementRepository;
    }

    public AiGradingDto gradeSubmission(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElseThrow();
        Requirement requirement = requirementRepository.findById(submission.getRequirementId()).orElseThrow();

        double completenessScore = calculateCompletenessScore(requirement);
        double ethicsIntegrationScore = calculateEthicsIntegrationScore(requirement);
        double innovationScore = calculateInnovationScore(requirement);
        
        double suggestedScore = (completenessScore * 0.4 + ethicsIntegrationScore * 0.35 + innovationScore * 0.25);
        
        List<String> strengths = identifyStrengths(requirement);
        List<String> improvements = generateImprovementSuggestions(requirement);
        String comment = generateIntelligentComment(requirement, suggestedScore, strengths);
        String overallFeedback = generateOverallFeedback(suggestedScore, completenessScore, ethicsIntegrationScore);

        return new AiGradingDto(
            submissionId,
            Math.round(suggestedScore * 100) / 100.0,
            comment,
            improvements,
            strengths,
            Math.round(ethicsIntegrationScore * 100) / 100.0,
            Math.round(completenessScore * 100) / 100.0,
            Math.round(innovationScore * 100) / 100.0,
            overallFeedback
        );
    }

    private double calculateCompletenessScore(Requirement req) {
        double score = 50.0;
        
        if (req.getTextContent() != null && req.getTextContent().length() > 0) {
            int length = req.getTextContent().length();
            if (length >= 500) score += 20;
            else if (length >= 200) score += 15;
            else if (length >= 100) score += 10;
            else score += 5;
        }
        
        if (req.getDiagramJson() != null && !req.getDiagramJson().isBlank()) {
            score += 15;
        }
        
        if (req.getEmbeddedModules() != null && !req.getEmbeddedModules().isBlank()) {
            score += 15;
        }
        
        return Math.min(100, score);
    }

    private double calculateEthicsIntegrationScore(Requirement req) {
        double score = 0.0;
        
        if (req.getEmbeddedModules() == null || req.getEmbeddedModules().isBlank()) {
            return 0.0;
        }
        
        String[] modules = req.getEmbeddedModules().split(",");
        score += Math.min(40, modules.length * 20);
        
        if (req.getTextContent() != null) {
            String text = req.getTextContent().toLowerCase();
            if (text.contains("社会责任") || text.contains("工程伦理") || text.contains("安全")) {
                score += 20;
            }
            if (text.contains("创新") || text.contains("可持续")) {
                score += 15;
            }
            if (text.contains("质量") || text.contains("标准")) {
                score += 15;
            }
        }
        
        if (modules.length >= 3) {
            score += 10;
        }
        
        return Math.min(100, score);
    }

    private double calculateInnovationScore(Requirement req) {
        double score = 50.0;
        
        if (req.getTextContent() == null || req.getTextContent().isBlank()) {
            return score;
        }
        
        String text = req.getTextContent();
        String lower = text.toLowerCase();
        
        String[] innovationKeywords = {"创新", "新型", "独特", "突破", "改进", "优化", "提升"};
        for (String kw : innovationKeywords) {
            if (lower.contains(kw)) {
                score += 5;
                if (score >= 90) break;
            }
        }
        
        String[] techKeywords = {"人工智能", "大数据", "云计算", "区块链", "物联网", "机器学习"};
        for (String kw : techKeywords) {
            if (lower.contains(kw)) {
                score += 8;
                if (score >= 95) break;
            }
        }
        
        if (req.getDiagramJson() != null && !req.getDiagramJson().isBlank()) {
            score += 10;
        }
        
        return Math.min(100, score);
    }

    private List<String> identifyStrengths(Requirement req) {
        List<String> strengths = new ArrayList<>();
        
        if (req.getTextContent() != null && req.getTextContent().length() >= 500) {
            strengths.add("需求文档内容详实，描述较为完整");
        }
        
        if (req.getDiagramJson() != null && !req.getDiagramJson().isBlank()) {
            strengths.add("包含用例图等图示说明，直观清晰");
        }
        
        if (req.getEmbeddedModules() != null && !req.getEmbeddedModules().isBlank()) {
            String[] modules = req.getEmbeddedModules().split(",");
            if (modules.length >= 2) {
                strengths.add("融合了多个思政模块，体现了综合思考");
            } else {
                strengths.add("包含了思政元素融合");
            }
        }
        
        if (strengths.isEmpty()) {
            strengths.add("需求基本结构完整");
        }
        
        return strengths;
    }

    private List<String> generateImprovementSuggestions(Requirement req) {
        List<String> suggestions = new ArrayList<>();
        
        if (req.getTextContent() == null || req.getTextContent().length() < 200) {
            suggestions.add("建议补充更详细的需求描述，包括功能需求和非功能需求");
        }
        
        if (req.getDiagramJson() == null || req.getDiagramJson().isBlank()) {
            suggestions.add("建议添加用例图或流程图，使需求更加直观");
        }
        
        if (req.getEmbeddedModules() == null || req.getEmbeddedModules().isBlank()) {
            suggestions.add("建议使用AI思政推荐功能，选择合适的工程伦理模块进行融合");
        } else {
            String[] modules = req.getEmbeddedModules().split(",");
            if (modules.length < 2) {
                suggestions.add("可以尝试融合更多思政模块，如社会责任、工程安全等");
            }
        }
        
        if (req.getTextContent() != null) {
            String text = req.getTextContent().toLowerCase();
            boolean hasInnovation = false;
            String[] innovationWords = {"创新", "改进", "优化", "提升", "新型"};
            for (String w : innovationWords) {
                if (text.contains(w)) {
                    hasInnovation = true;
                    break;
                }
            }
            if (!hasInnovation) {
                suggestions.add("可以考虑增加创新性描述，如技术选型创新或业务流程改进");
            }
        }
        
        return suggestions;
    }

    private String generateIntelligentComment(Requirement req, double suggestedScore, List<String> strengths) {
        StringBuilder comment = new StringBuilder();
        
        if (suggestedScore >= 85) {
            comment.append("这份需求文档质量优秀。");
        } else if (suggestedScore >= 70) {
            comment.append("这份需求文档整体质量良好，但仍有一定提升空间。");
        } else if (suggestedScore >= 60) {
            comment.append("这份需求文档基本满足要求，建议针对以下方面进行改进。");
        } else {
            comment.append("这份需求文档需要较多改进，建议完善各部分内容。");
        }
        
        if (!strengths.isEmpty()) {
            comment.append("优点：").append(String.join("、", strengths)).append("。");
        }
        
        return comment.toString();
    }

    private String generateOverallFeedback(double suggestedScore, double completeness, double ethics) {
        if (suggestedScore >= 85) {
            return "该学生表现出色，特别是在需求完整性和思政融合方面都有很好的体现。建议继续保持并发扬这种综合分析能力。";
        } else if (suggestedScore >= 70) {
            return "该学生作业质量良好，在某些方面表现突出。建议继续加强创新能力培养和思政融合深度。";
        } else if (suggestedScore >= 60) {
            return "该学生作业基本达标。建议关注需求完整性和思政元素的有效融合，可参考优秀案例进行改进。";
        } else {
            return "该学生作业需要较多改进。建议认真阅读教材和优秀案例，重点补充缺失部分，并与老师或同学讨论改进方向。";
        }
    }
}
