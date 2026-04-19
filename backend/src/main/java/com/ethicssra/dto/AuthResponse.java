package com.ethicssra.dto;

import com.ethicssra.domain.Role;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        Role role,
        String displayName
) {
}
