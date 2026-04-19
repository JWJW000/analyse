package com.ethicssra.service;

import com.ethicssra.domain.*;
import com.ethicssra.dto.ReportRequest;
import com.ethicssra.dto.ReportResponse;
import com.ethicssra.repository.*;
import com.ethicssra.util.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ReportService {

    private final ProjectRepository projectRepository;
    private final LiteratureRepository literatureRepository;
    private final RequirementRepository requirementRepository;
    private final EthicsModuleRepository ethicsModuleRepository;
    private final UserRepository userRepository;
    private final Map<String, ReportDownload> generatedReports = new ConcurrentHashMap<>();

    public ReportService(
            ProjectRepository projectRepository,
            LiteratureRepository literatureRepository,
            RequirementRepository requirementRepository,
            EthicsModuleRepository ethicsModuleRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.literatureRepository = literatureRepository;
        this.requirementRepository = requirementRepository;
        this.ethicsModuleRepository = ethicsModuleRepository;
        this.userRepository = userRepository;
    }

    public ReportResponse generateReport(ReportRequest request) {
        Long userId = SecurityUtils.currentUserId();
        if (request.projectId() != null && request.projectId() <= 0) {
            return generateRequirementOnlyReport(request, userId);
        }

        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new EntityNotFoundException("项目不存在"));

        if (!hasViewPermission(project, userId)) {
            throw new AccessDeniedException("无权访问该项目");
        }

        StringBuilder content = new StringBuilder();
        content.append("# ").append(project.getName()).append("\n\n");
        content.append("> 创建时间: ").append(project.getCreatedAt()).append("\n");
        content.append("> 项目负责人: ").append(getOwnerName(project.getOwnerId())).append("\n\n");

        if (request.content().includeLiterature() && request.literatureIds() != null) {
            content.append(generateLiteratureSection(request.literatureIds()));
        }

        if (request.content().includeRequirements() && request.requirementIds() != null) {
            content.append(generateRequirementsSection(request.requirementIds()));
        }

        if (request.content().includeEthicsFusion() && request.ethicsModuleIds() != null) {
            content.append(generateEthicsFusionSection(request.ethicsModuleIds()));
        }

        if (request.content().includeAppendix()) {
            content.append(generateAppendix());
        }

        String fileName = project.getName() + "_报告_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        if (request.format() == ReportRequest.ReportFormat.WORD) {
            fileName += ".docx";
        } else {
            fileName += ".pdf";
        }

        return buildResponse(fileName, content.toString());
    }

    private ReportResponse generateRequirementOnlyReport(ReportRequest request, Long userId) {
        StringBuilder content = new StringBuilder();
        content.append("# 我的需求报告\n\n");
        content.append("> 生成时间: ").append(LocalDate.now().format(DateTimeFormatter.ISO_DATE)).append("\n\n");

        if (request.content().includeRequirements() && request.requirementIds() != null) {
            content.append(generateRequirementsSectionForUser(request.requirementIds(), userId));
        }
        if (request.content().includeEthicsFusion() && request.ethicsModuleIds() != null) {
            content.append(generateEthicsFusionSection(request.ethicsModuleIds()));
        }
        if (request.content().includeAppendix()) {
            content.append(generateAppendix());
        }

        String fileName = "我的需求报告_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                + (request.format() == ReportRequest.ReportFormat.PDF ? ".pdf" : ".docx");
        return buildResponse(fileName, content.toString());
    }

    public ReportDownload getDownload(String id) {
        ReportDownload download = generatedReports.get(id);
        if (download == null) {
            throw new EntityNotFoundException("报告文件不存在或已过期，请重新生成");
        }
        return download;
    }

    private ReportResponse buildResponse(String fileName, String content) {
        byte[] docBytes = content.getBytes();
        String id = UUID.randomUUID().toString();
        generatedReports.put(id, new ReportDownload(fileName, docBytes, contentType(fileName)));
        return new ReportResponse(fileName, "/api/reports/download/" + id, docBytes.length);
    }

    private String contentType(String fileName) {
        if (fileName != null && fileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private String generateLiteratureSection(List<Long> literatureIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 文献综述\n\n");

        for (Long id : literatureIds) {
            literatureRepository.findById(id).ifPresent(lit -> {
                sb.append("## ").append(lit.getTitle()).append("\n\n");
                if (lit.getAuthor() != null) {
                    sb.append("**作者:** ").append(lit.getAuthor()).append("\n\n");
                }
                if (lit.getSource() != null) {
                    sb.append("**来源:** ").append(lit.getSource()).append("\n\n");
                }
                if (lit.getAbstractText() != null) {
                    sb.append("**摘要:** ").append(lit.getAbstractText()).append("\n\n");
                }
                if (lit.getKeywords() != null) {
                    sb.append("**关键词:** ").append(lit.getKeywords()).append("\n\n");
                }
                sb.append("---\n\n");
            });
        }
        return sb.toString();
    }

    private String generateRequirementsSection(List<Long> requirementIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 需求分析\n\n");

        for (Long id : requirementIds) {
            requirementRepository.findById(id).ifPresent(req -> appendRequirement(sb, req));
        }
        return sb.toString();
    }

    private String generateRequirementsSectionForUser(List<Long> requirementIds, Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 需求分析\n\n");

        for (Long id : requirementIds) {
            requirementRepository.findById(id)
                    .filter(req -> req.getUserId().equals(userId))
                    .ifPresent(req -> appendRequirement(sb, req));
        }
        return sb.toString();
    }

    private void appendRequirement(StringBuilder sb, Requirement req) {
        sb.append("## ").append(req.getTitle()).append("\n\n");
        sb.append("**状态:** ").append(req.getStatus()).append("\n\n");
        if (req.getTextContent() != null) {
            sb.append(req.getTextContent()).append("\n\n");
        }
        if (req.getEmbeddedModules() != null && !req.getEmbeddedModules().isBlank()) {
            sb.append("**思政模块:** ").append(req.getEmbeddedModules()).append("\n\n");
        }
        if (req.getDiagramJson() != null && !req.getDiagramJson().isBlank()) {
            sb.append("**用例图:** 已保存用例图数据\n\n");
        }
        sb.append("---\n\n");
    }

    private String generateEthicsFusionSection(List<Long> ethicsModuleIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 思政融合章节\n\n");

        for (Long id : ethicsModuleIds) {
            ethicsModuleRepository.findById(id).ifPresent(module -> {
                sb.append("## ").append(module.getTitle()).append("\n\n");
                if (module.getCategory() != null) {
                    sb.append("**类别:** ").append(module.getCategory()).append("\n\n");
                }
                if (module.getCaseText() != null) {
                    sb.append("**案例:** ").append(module.getCaseText()).append("\n\n");
                }
                if (module.getDescription() != null) {
                    sb.append("**融合说明:** ").append(module.getDescription()).append("\n\n");
                }
                if (module.getKeywords() != null) {
                    sb.append("**关键词:** ").append(module.getKeywords()).append("\n\n");
                }
                sb.append("---\n\n");
            });
        }
        return sb.toString();
    }

    private String generateAppendix() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 附录\n\n");
        sb.append("本报告由工程教育思政融合平台自动生成。\n\n");
        sb.append("生成时间: ").append(LocalDate.now().format(DateTimeFormatter.ISO_DATE)).append("\n\n");
        return sb.toString();
    }

    private boolean hasViewPermission(Project project, Long userId) {
        if (project.getOwnerId().equals(userId)) return true;
        return false;
    }

    private String getOwnerName(Long ownerId) {
        return userRepository.findById(ownerId)
                .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getUsername())
                .orElse("Unknown");
    }

    public record ReportDownload(String fileName, byte[] bytes, String contentType) {}
}
