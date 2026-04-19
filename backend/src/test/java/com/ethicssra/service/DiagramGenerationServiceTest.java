package com.ethicssra.service;

import com.ethicssra.dto.DiagramGenerationDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiagramGenerationServiceTest {

    @Test
    void generateUseCaseDiagramExtractsActorsUseCasesAndRelationsFromRequirementText() {
        DiagramGenerationService service = new DiagramGenerationService();

        DiagramGenerationDto diagram = service.generateUseCaseDiagram("""
                学生可以注册登录校园二手交易平台，发布商品、搜索商品、提交订单并与卖家沟通。
                管理员需要审核商品信息、管理违规内容，并导出交易统计报表。
                教师可以查看课程任务提交情况。
                """);

        assertThat(diagram.diagramType()).isEqualTo("usecase");
        assertThat(diagram.nodes())
                .extracting(DiagramGenerationDto.DiagramNode::label)
                .contains("学生", "管理员", "教师", "发布商品", "搜索商品", "审核商品信息", "导出交易统计报表");
        assertThat(diagram.edges()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(diagram.explanation()).contains("参与者", "用例");
    }
}
