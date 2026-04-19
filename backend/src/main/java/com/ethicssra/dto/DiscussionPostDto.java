package com.ethicssra.dto;

public record DiscussionPostDto(
        Long id,
        Long courseId,
        Long authorId,
        String authorDisplayName,
        String title,
        String content,
        String category,
        boolean visible,
        int viewCount,
        int replyCount,
        String createdAt
) {
}
