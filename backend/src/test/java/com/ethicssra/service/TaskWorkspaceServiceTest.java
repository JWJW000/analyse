package com.ethicssra.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWorkspaceServiceTest {

    @Test
    void countEmbeddedModulesIgnoresEmptyParts() {
        assertThat(TaskWorkspaceService.countEmbeddedModules("1, 2, ,x, 3")).isEqualTo(3);
        assertThat(TaskWorkspaceService.countEmbeddedModules(" ")).isEqualTo(0);
    }

    @Test
    void collectBlockingIssuesIncludesMissingLinks() {
        List<String> issues = TaskWorkspaceService.collectBlockingIssues(List.of(
                new TaskWorkspaceService.RequirementSnapshot(10L, "", 0, 0),
                new TaskWorkspaceService.RequirementSnapshot(11L, "需求文本", 1, 0)
        ));

        assertThat(issues).contains("需求 #10 缺少正文内容");
        assertThat(issues).contains("需求 #10 缺少文献证据映射");
        assertThat(issues).contains("需求 #10 缺少伦理模块映射");
        assertThat(issues).contains("需求 #11 缺少伦理模块映射");
    }
}
