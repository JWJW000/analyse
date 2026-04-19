package com.ethicssra.service;

import com.ethicssra.domain.*;
import com.ethicssra.repository.*;
import com.ethicssra.service.llm.DashScopeGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiMentorService {

    private static final Logger log = LoggerFactory.getLogger(AiMentorService.class);

    private final DashScopeGateway llmGateway;
    private final ProjectRepository projectRepository;
    private final ProjectRequirementRepository projectRequirementRepository;
    private final LiteratureRepository literatureRepository;
    private final EthicsModuleRepository ethicsModuleRepository;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;
    private final CommentRepository commentRepository;

    public AiMentorService(
            DashScopeGateway llmGateway,
            ProjectRepository projectRepository,
            ProjectRequirementRepository projectRequirementRepository,
            LiteratureRepository literatureRepository,
            EthicsModuleRepository ethicsModuleRepository,
            CourseRepository courseRepository,
            SubmissionRepository submissionRepository,
            CommentRepository commentRepository) {
        this.llmGateway = llmGateway;
        this.projectRepository = projectRepository;
        this.projectRequirementRepository = projectRequirementRepository;
        this.literatureRepository = literatureRepository;
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.courseRepository = courseRepository;
        this.submissionRepository = submissionRepository;
        this.commentRepository = commentRepository;
    }

    public List<MentorSuggestion> getSuggestions(Long projectId, String currentPage) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return List.of();
        }

        String context = buildProjectContext(project, currentPage);
        String systemPrompt = buildMentorSystemPrompt();
        String userMessage = buildSuggestionRequest(context, project, currentPage);

        try {
            String response = llmGateway.chat(systemPrompt, userMessage);
            return parseSuggestions(response);
        } catch (Exception e) {
            log.error("Failed to get AI suggestions for project {}: {}", projectId, e.getMessage());
            return getDefaultSuggestions(project, currentPage);
        }
    }

    public String chat(Long projectId, String userMessage, List<ChatMessage> history) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        String context = buildProjectContext(project, "chat");
        String systemPrompt = buildChatSystemPrompt(context);

        try {
            List<DashScopeGateway.ChatMessage> chatHistory = new ArrayList<>();
            if (history != null) {
                for (ChatMessage msg : history) {
                    chatHistory.add(new DashScopeGateway.ChatMessage(msg.getRole(), msg.getContent()));
                }
            }
            return llmGateway.chat(systemPrompt, userMessage, chatHistory);
        } catch (Exception e) {
            log.error("Failed to chat with AI for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException("AI chat failed: " + e.getMessage(), e);
        }
    }

    public String analyzeRequirement(Long projectId, String requirementTitle, String requirementContent) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        String systemPrompt = """
            你是一位专业的软件工程导师，擅长需求分析。
            请分析学生提交的需求文档，提供专业的改进建议。

            分析要点：
            1. 需求是否完整（功能、性能、安全、可用性等）
            2. 需求描述是否清晰、无歧义
            3. 需求是否可测试
            4. 是否符合软件工程最佳实践
            5. 是否有遗漏的关键功能

            请以结构化的方式给出分析结果：
            - 优点：列出做得好的地方
            - 改进建议：列出需要改进的地方
            - 完整性评分：0-100分
            """;

        String userMessage = String.format("项目名称：%s\n需求标题：%s\n\n需求内容：\n%s",
            project.getName(),
            requirementTitle != null ? requirementTitle : "无标题",
            requirementContent != null ? requirementContent : "无内容");

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to analyze requirement: {}", e.getMessage());
            throw new RuntimeException("Requirement analysis failed: " + e.getMessage(), e);
        }
    }

    public String generateRequirementDraft(Long projectId, String projectDescription) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        String systemPrompt = """
            你是一位专业的软件工程导师，擅长需求分析。
            请根据项目的描述，帮助学生生成需求文档的初稿。

            要求：
            1. 生成标准的需求结构（功能需求、性能需求、安全需求等）
            2. 提供每个部分的简要说明
            3. 使用清晰、专业的语言
            4. 格式要规范，便于学生理解和填写

            请生成一份需求文档模板，包含：
            - 项目概述
            - 功能需求
            - 非功能需求（性能、安全、可用性等）
            - 约束条件
            """;

        String userMessage = String.format("项目名称：%s\n项目描述：%s",
            project.getName(),
            projectDescription != null ? projectDescription : "无详细描述");

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to generate requirement draft: {}", e.getMessage());
            throw new RuntimeException("Requirement draft generation failed: " + e.getMessage(), e);
        }
    }

    public String generateUseCases(Long projectId, String requirementContent) {
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            throw new IllegalArgumentException("Project not found");
        }

        String systemPrompt = """
            你是一位专业的软件工程导师，擅长用例建模。
            请根据以下需求描述，生成规范的用例。
            每个用例应包含：用例名称、参与者、前置条件、基本流程、后置条件。
            格式要求：清晰、简洁、可操作。
            """;

        String userMessage = "项目名称：" + project.getName() + "\n\n需求内容：\n" + requirementContent;

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to generate use cases: {}", e.getMessage());
            throw new RuntimeException("Use case generation failed: " + e.getMessage(), e);
        }
    }

    public String generateFusionContent(Long projectId, String requirementText, Long ethicsModuleId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        EthicsModule module = ethicsModuleRepository.findById(ethicsModuleId).orElse(null);

        if (project == null || module == null) {
            throw new IllegalArgumentException("Project or EthicsModule not found");
        }

        String systemPrompt = """
            你是一位专业的工程教育导师，擅长将思政元素与专业内容有机融合。
            请根据给定的需求文本和思政模块，生成一段融合说明。
            要求：
            1. 自然地连接专业内容与思政元素
            2. 体现工程伦理和社会责任
            3. 语言专业且有教育意义
            4. 内容可直接用于报告
            """;

        String userMessage = String.format("""
            项目名称：%s
            课程名称：%s

            需求文本：
            %s

            思政模块：
            标题：%s
            关键词：%s
            描述：%s
            案例：%s
            """,
            project.getName(),
            project.getCourseId() != null ? "课程ID:" + project.getCourseId() : "未知课程",
            requirementText,
            module.getTitle(),
            module.getKeywords() != null ? module.getKeywords() : "",
            module.getDescription() != null ? module.getDescription() : "",
            module.getCaseText() != null ? module.getCaseText() : ""
        );

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to generate fusion content: {}", e.getMessage());
            throw new RuntimeException("Fusion content generation failed: " + e.getMessage(), e);
        }
    }

    private String buildProjectContext(Project project, String currentPage) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("项目名称：").append(project.getName()).append("\n");
        ctx.append("当前阶段：").append(project.getCurrentPhase().name()).append("\n");

        if (project.getCourseId() != null) {
            ctx.append("课程ID：").append(project.getCourseId()).append("\n");
        }

        long literatureCount = literatureRepository.countByProjectId(project.getId());
        long requirementCount = projectRequirementRepository.countByProjectId(project.getId());
        long ethicsCount = ethicsModuleRepository.countByProjectId(project.getId());

        ctx.append("已关联文献：").append(literatureCount).append(" 篇\n");
        ctx.append("已添加需求：").append(requirementCount).append(" 条\n");
        ctx.append("已关联思政：").append(ethicsCount).append(" 个\n");

        return ctx.toString();
    }

    private String buildMentorSystemPrompt() {
        return """
            你是一位友好的AI学习导师，名为"小思"。
            你的职责是帮助学生高效完成软件工程实践项目。
            特点：
            1. 了解软件工程全流程（需求分析、系统设计、编码、测试、部署）
            2. 熟悉工程伦理和思政教育要求
            3. 善于给出具体、可操作的建议
            4. 语气友好、鼓励为主

            请根据学生的项目情况和当前页面，提供1-3条具体建议。
            建议格式：[建议类型] 具体建议内容
            建议类型包括：action（操作建议）、warning（提醒）、tip（技巧）
            """;
    }

    private String buildChatSystemPrompt(String context) {
        return """
            你是一位友好的AI学习导师，名为"小思"。
            你正在辅导学生完成软件工程实践项目。

            项目上下文：
            %s

            你的职责：
            1. 回答学生关于项目的问题
            2. 提供专业的技术指导
            3. 给出实用的建议和技巧
            4. 帮助学生理解工程伦理和社会责任

            请用友好、专业的语气回答。
            """.formatted(context);
    }

    private String buildSuggestionRequest(String context, Project project, String currentPage) {
        return String.format("""
            项目信息：
            %s

            当前页面：%s
            项目阶段：%s

            请给出针对当前情况的具体建议。
            """, context, currentPage, project.getCurrentPhase().name());
    }

    private List<MentorSuggestion> parseSuggestions(String response) {
        List<MentorSuggestion> suggestions = new ArrayList<>();

        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String type = "tip";
            if (line.startsWith("[action]") || line.startsWith("[操作]")) {
                type = "action";
                line = line.replaceAll("\\[action\\]|\\[操作\\]", "").trim();
            } else if (line.startsWith("[warning]") || line.startsWith("[提醒]")) {
                type = "warning";
                line = line.replaceAll("\\[warning\\]|\\[提醒\\]", "").trim();
            }

            if (!line.isEmpty()) {
                suggestions.add(new MentorSuggestion(type, line));
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add(new MentorSuggestion("tip", response));
        }

        return suggestions;
    }

    // ==================== 教师端 AI 功能 ====================

    public String generateFeedback(Long requirementId, String requirementTitle, String requirementContent,
                                  String studentName, String courseName) {
        String systemPrompt = """
            你是一位经验丰富的软件工程教师，擅长批改学生作业并给出建设性的反馈。

            你的任务是：
            1. 分析学生提交的需求文档
            2. 指出优点和不足
            3. 给出具体的改进建议
            4. 评分（0-100分）

            请以教师的口吻给出反馈，要：
            - 客观公正
            - 鼓励为主
            - 具体明确
            - 有建设性

            格式：
            - 总体评价
            - 优点
            - 不足与建议
            - 评分
            - 总结
            """;

        String userMessage = String.format("""
            学生姓名：%s
            课程名称：%s
            需求标题：%s

            需求内容：
            %s
            """,
            studentName != null ? studentName : "学生",
            courseName != null ? courseName : "未知课程",
            requirementTitle != null ? requirementTitle : "无标题",
            requirementContent != null ? requirementContent : "无内容");

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to generate feedback: {}", e.getMessage());
            throw new RuntimeException("Feedback generation failed: " + e.getMessage(), e);
        }
    }

    public String analyzeSubmissionQuality(Long submissionId, String submissionContent,
                                          String studentName, String projectName) {
        String systemPrompt = """
            你是一位专业的软件工程教育评估专家，擅长评估学生学习成果的质量。

            请评估学生提交的内容，从以下维度进行分析：
            1. 完整性 - 是否包含所有必要部分
            2. 准确性 - 内容是否正确、无误
            3. 专业性 - 是否符合工程规范
            4. 思政融合 - 思政元素是否自然融入
            5. 创新性 - 是否有独特的见解

            请给出：
            - 各维度评分（0-100）
            - 综合评分
            - 详细的改进建议
            """;

        String userMessage = String.format("""
            项目名称：%s
            学生姓名：%s
            提交内容ID：%d

            内容：
            %s
            """,
            projectName != null ? projectName : "未知项目",
            studentName != null ? studentName : "未知学生",
            submissionId,
            submissionContent != null ? submissionContent : "无内容");

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to analyze submission quality: {}", e.getMessage());
            throw new RuntimeException("Submission quality analysis failed: " + e.getMessage(), e);
        }
    }

    public String suggestImprovements(Long requirementId, String requirementTitle,
                                     String requirementContent, String currentFeedback) {
        String systemPrompt = """
            你是一位耐心的软件工程导师，帮助学生根据教师反馈改进他们的工作。

            给定学生的需求文档和教师的反馈，请：
            1. 理解教师的反馈要点
            2. 提供具体的改进方向
            3. 给出改进后的示例（如果适用）

            语气要友好、鼓励，帮助学生看到进步的方向。
            """;

        String userMessage = String.format("""
            需求标题：%s

            当前需求内容：
            %s

            教师反馈：
            %s

            请给出具体的改进建议：
            """,
            requirementTitle != null ? requirementTitle : "无标题",
            requirementContent != null ? requirementContent : "无内容",
            currentFeedback != null ? currentFeedback : "无反馈");

        try {
            return llmGateway.chat(systemPrompt, userMessage);
        } catch (Exception e) {
            log.error("Failed to suggest improvements: {}", e.getMessage());
            throw new RuntimeException("Improvement suggestion failed: " + e.getMessage(), e);
        }
    }

    private List<MentorSuggestion> getDefaultSuggestions(Project project, String currentPage) {
        List<MentorSuggestion> suggestions = new ArrayList<>();
        var phase = project.getCurrentPhase();
        long litCount = project.getLiteratures().size();
        long reqCount = project.getRequirements().size();
        long ethCount = project.getEthicsModules().size();

        // 基于阶段的智能建议
        switch (phase) {
            case LITERATURE -> {
                if (litCount == 0) {
                    suggestions.add(new MentorSuggestion("action", "开始添加文献吧！建议搜索与项目主题相关的论文"));
                    suggestions.add(new MentorSuggestion("tip", "好的文献调研是需求分析的基础"));
                } else if (litCount < 3) {
                    suggestions.add(new MentorSuggestion("action", "已添加 " + litCount + " 篇文献，建议再添加 2-3 篇相关文献"));
                    suggestions.add(new MentorSuggestion("tip", "文献可以帮助您更好地理解项目背景"));
                } else {
                    suggestions.add(new MentorSuggestion("action", "文献调研完成！可以进入需求分析阶段了"));
                    suggestions.add(new MentorSuggestion("tip", "在进入下一阶段前，确保已阅读并理解了关键文献"));
                }
            }
            case REQUIREMENTS -> {
                if (reqCount == 0) {
                    suggestions.add(new MentorSuggestion("action", "开始创建需求文档吧！建议从用户故事开始"));
                    suggestions.add(new MentorSuggestion("tip", "一个好的需求文档应该描述用户想要完成的任务"));
                } else if (reqCount < 3) {
                    suggestions.add(new MentorSuggestion("action", "已创建 " + reqCount + " 个需求，建议完善功能需求、性能需求等"));
                    suggestions.add(new MentorSuggestion("tip", "别忘了添加非功能需求，如性能、安全等"));
                } else {
                    suggestions.add(new MentorSuggestion("action", "需求文档已初具规模！建议生成用例图来可视化需求"));
                    suggestions.add(new MentorSuggestion("tip", "您可以让 AI 导师帮您分析和完善需求"));
                }
            }
            case ETHICS -> {
                if (ethCount == 0) {
                    suggestions.add(new MentorSuggestion("action", "开始关联思政模块吧！AI 可以帮您推荐相关的模块"));
                    suggestions.add(new MentorSuggestion("warning", "思政融合是课程要求的重要部分，请认真对待"));
                } else if (ethCount < 2) {
                    suggestions.add(new MentorSuggestion("action", "已关联 " + ethCount + " 个思政模块，建议再添加 1-2 个"));
                    suggestions.add(new MentorSuggestion("tip", "思政融合要自然，避免生硬添加"));
                } else {
                    suggestions.add(new MentorSuggestion("action", "思政融合完成！可以进入作业提交阶段了"));
                    suggestions.add(new MentorSuggestion("tip", "确保每个思政模块都与需求有清晰的对应关系"));
                }
            }
            case SUBMISSION -> {
                suggestions.add(new MentorSuggestion("action", "检查所有需求是否已提交审核"));
                suggestions.add(new MentorSuggestion("tip", "确保所有关联的需求都处于「已提交」或「已通过」状态"));
            }
            case REVIEW -> {
                suggestions.add(new MentorSuggestion("tip", "当前处于审核反馈阶段，请等待教师评审"));
                suggestions.add(new MentorSuggestion("action", "如果有反馈意见，可以主动联系教师沟通"));
            }
        }

        // 如果当前页面不是概览，添加页面特定的建议
        if (!"dashboard".equals(currentPage) && !"overview".equals(currentPage)) {
            switch (currentPage) {
                case "literature" -> {
                    suggestions.add(new MentorSuggestion("tip", "在文献页面可以上传、搜索和管理文献"));
                }
                case "requirement" -> {
                    suggestions.add(new MentorSuggestion("tip", "在需求页面可以创建、编辑需求文档"));
                }
                case "ethics" -> {
                    suggestions.add(new MentorSuggestion("tip", "在思政页面可以关联和管理思政模块"));
                }
            }
        }

        return suggestions;
    }

    public record MentorSuggestion(String type, String content) {}

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
