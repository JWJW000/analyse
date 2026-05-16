package com.ethicssra.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLibraryFieldShapeTest {

    @Test
    void literatureDtoCarriesTeachingResearchMetadata() {
        LiteratureDto dto = new LiteratureDto(
                1L,
                "需求工程证据链",
                "课程案例组",
                "教学资料",
                "摘要",
                "需求工程",
                null,
                7L,
                2025,
                "10.1000/example",
                "https://example.edu/paper",
                "教学案例",
                "案例研究",
                "需求证据映射",
                "核心结论",
                "支撑需求来源和验收标准"
        );

        assertThat(dto.publicationYear()).isEqualTo(2025);
        assertThat(dto.researchMethod()).isEqualTo("案例研究");
        assertThat(dto.evidenceValue()).contains("需求来源");
    }

    @Test
    void ethicsModuleDtoCarriesTeachingEmbeddingMetadata() {
        EthicsModuleDto dto = new EthicsModuleDto(
                2L,
                "诚信与职业操守",
                "职业伦理",
                "诚信",
                "知识点",
                "案例",
                "参考",
                1,
                "需求调研与报告撰写",
                "理解证据可追溯",
                "求真务实",
                "如何标注来源？",
                "伪造数据、遗漏风险",
                "写入非功能需求和验收标准",
                "软件工程",
                "中"
        );

        assertThat(dto.applicableScenario()).contains("需求调研");
        assertThat(dto.integrationSuggestion()).contains("验收标准");
        assertThat(dto.difficultyLevel()).isEqualTo("中");
    }
}
