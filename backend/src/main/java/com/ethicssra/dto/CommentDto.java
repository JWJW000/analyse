package com.ethicssra.dto;

import com.ethicssra.domain.Comment;
import java.time.Instant;

public record CommentDto(
    Long id,
    Long projectId,
    Long requirementId,
    Long userId,
    String userName,
    String content,
    Long parentId,
    Instant createdAt,
    Instant updatedAt
) {
    public static CommentDto from(Comment c, String userName) {
        return new CommentDto(
            c.getId(),
            c.getProjectId(),
            c.getRequirementId(),
            c.getUserId(),
            userName,
            c.getContent(),
            c.getParentId(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}