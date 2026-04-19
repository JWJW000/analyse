package com.ethicssra.dto;

import jakarta.validation.constraints.NotNull;

public record ReportRequest(
    @NotNull(message = "项目ID不能为空")
    Long projectId,
    ReportFormat format,
    ReportContent content,
    java.util.List<Long> literatureIds,
    java.util.List<Long> requirementIds,
    java.util.List<Long> ethicsModuleIds
) {
    public enum ReportFormat {
        WORD, PDF
    }

    public record ReportContent(
        boolean includeLiterature,
        boolean includeRequirements,
        boolean includeEthicsFusion,
        boolean includeDiagrams,
        boolean includeAppendix
    ) {}
}