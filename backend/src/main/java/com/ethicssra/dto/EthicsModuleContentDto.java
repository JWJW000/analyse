package com.ethicssra.dto;

import com.ethicssra.domain.EthicsModule;
import java.time.Instant;

public record EthicsModuleContentDto(
    Long id,
    String title,
    String category,
    String keywords,
    Instant createdAt
) {
    public static EthicsModuleContentDto from(EthicsModule e) {
        return new EthicsModuleContentDto(
            e.getId(),
            e.getTitle(),
            e.getCategory(),
            e.getKeywords(),
            e.getCreatedAt()
        );
    }
}
