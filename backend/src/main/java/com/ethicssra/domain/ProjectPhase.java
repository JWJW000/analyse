package com.ethicssra.domain;

public enum ProjectPhase {
    LITERATURE("文献调研"),
    REQUIREMENTS("需求分析"),
    ETHICS("思政融合"),
    SUBMISSION("作业提交"),
    REVIEW("审核反馈");

    private final String displayName;

    ProjectPhase(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}