package com.ethicssra.dto;

import com.ethicssra.domain.ProjectMember;
import com.ethicssra.domain.ProjectRole;
import java.time.Instant;

public record ProjectMemberDto(
    Long id,
    Long userId,
    String userName,
    ProjectRole role,
    Instant joinedAt
) {
    public static ProjectMemberDto from(ProjectMember m) {
        return new ProjectMemberDto(
            m.getId(),
            m.getUser().getId(),
            m.getUser().getDisplayName() != null ? m.getUser().getDisplayName() : m.getUser().getUsername(),
            m.getRole(),
            m.getJoinedAt()
        );
    }
}