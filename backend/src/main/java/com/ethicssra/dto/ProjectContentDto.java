package com.ethicssra.dto;

import com.ethicssra.domain.Literature;
import java.time.Instant;

public record ProjectContentDto(
    Long id,
    String title,
    String author,
    String keywords,
    Instant createdAt
) {
    public static ProjectContentDto from(Literature l) {
        return new ProjectContentDto(
            l.getId(),
            l.getTitle(),
            l.getAuthor(),
            l.getKeywords(),
            l.getCreatedAt()
        );
    }
}
