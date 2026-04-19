package com.ethicssra.service;

import com.ethicssra.domain.EthicsModule;
import com.ethicssra.dto.ModuleMatchDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiMatchServiceFallbackTest {

    @Test
    void localRankingPrioritizesMoreRelevantModules() {
        EthicsModule m1 = module(1L, "学术诚信", "引用规范 学术不端", "强调论文引用和学术诚信规范");
        EthicsModule m2 = module(2L, "网络安全", "漏洞 防护", "介绍基本网络安全防护");

        List<ModuleMatchDto> ranked = AiMatchService.localRankModules(
                "请补充学术诚信和引用规范要求",
                List.of(m1, m2),
                2
        );

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).moduleId()).isEqualTo(1L);
        assertThat(ranked.get(0).score()).isGreaterThan(ranked.get(1).score());
    }

    private static EthicsModule module(Long id, String title, String keywords, String description) {
        EthicsModule m = new EthicsModule();
        m.setId(id);
        m.setTitle(title);
        m.setKeywords(keywords);
        m.setDescription(description);
        return m;
    }
}
