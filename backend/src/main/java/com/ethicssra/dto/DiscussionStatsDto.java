package com.ethicssra.dto;

import java.util.List;
import java.util.Map;

public record DiscussionStatsDto(
    Map<String, Long> categoryCounts,
    List<HotPostDto> hotPosts,
    List<CategoryDto> categories,
    long totalPosts,
    long totalCourses
) {
    public record HotPostDto(
        Long id,
        String title,
        String authorName,
        long replyCount,
        long viewCount,
        String createdAt
    ) {}

    public record CategoryDto(
        String code,
        String name,
        String description,
        long postCount
    ) {}
}
