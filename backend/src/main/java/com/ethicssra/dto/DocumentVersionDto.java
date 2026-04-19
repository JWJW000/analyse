package com.ethicssra.dto;

import com.ethicssra.domain.DocumentVersion;
import java.time.Instant;

public record DocumentVersionDto(
    Long id,
    Long projectId,
    Long requirementId,
    Integer versionNumber,
    String content,
    String changeSummary,
    Long userId,
    String userName,
    Instant createdAt
) {
    public static DocumentVersionDto from(DocumentVersion v, String userName) {
        return new DocumentVersionDto(
            v.getId(),
            v.getProjectId(),
            v.getRequirementId(),
            v.getVersionNumber(),
            v.getContent(),
            v.getChangeSummary(),
            v.getUserId(),
            userName,
            v.getCreatedAt()
        );
    }
}