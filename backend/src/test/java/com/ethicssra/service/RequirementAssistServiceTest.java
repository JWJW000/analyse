package com.ethicssra.service;

import com.ethicssra.dto.ContinueTextDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementAssistServiceTest {

    @Test
    void continueTextGeneratesAppendableRequirementContent() {
        RequirementAssistService service = new RequirementAssistService(null, null);

        ContinueTextDto result = service.continueText(
                "校园二手交易平台",
                "系统应支持学生发布商品、搜索商品并完成交易沟通。",
                null
        );

        assertThat(result.continuedText()).contains("验收");
        assertThat(result.continuedText()).contains("非功能");
        assertThat(result.continuedText()).contains("伦理");
        assertThat(result.source()).isEqualTo("rule");
    }
}
