package com.ethicssra.dto;

import java.time.Instant;

public record CollaborationSessionDto(
    Long requirementId,
    Long userId,
    String userName,
    Instant lockedAt,
    Instant expiresAt,
    boolean active
) {}
