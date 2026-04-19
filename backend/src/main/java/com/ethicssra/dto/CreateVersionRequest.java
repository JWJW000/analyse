package com.ethicssra.dto;

public record CreateVersionRequest(
    Long projectId,
    Long requirementId,
    String content,
    String changeSummary
) {}