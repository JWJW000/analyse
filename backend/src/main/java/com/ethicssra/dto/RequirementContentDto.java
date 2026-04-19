package com.ethicssra.dto;

import com.ethicssra.domain.Requirement;
import java.time.Instant;

public record RequirementContentDto(
    Long id,
    String title,
    String textContent,
    Instant createdAt
) {
    public static RequirementContentDto from(Requirement r) {
        return new RequirementContentDto(
            r.getId(),
            r.getTitle(),
            r.getTextContent(),
            r.getCreatedAt()
        );
    }
}
