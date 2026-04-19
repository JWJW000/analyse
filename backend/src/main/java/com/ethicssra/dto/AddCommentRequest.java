package com.ethicssra.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
    Long projectId,
    Long requirementId,
    @NotBlank(message = "评论内容不能为空")
    String content,
    Long parentId
) {}