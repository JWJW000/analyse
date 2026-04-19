package com.ethicssra.dto;

import com.ethicssra.domain.Role;

public record UserProfileDto(
        Long id,
        String username,
        Role role,
        String displayName
) {
}
