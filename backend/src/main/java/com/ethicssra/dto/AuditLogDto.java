package com.ethicssra.dto;

public record AuditLogDto(
        Long id,
        String action,
        String entityType,
        Long entityId,
        String detailJson,
        String createdAt
) {}
