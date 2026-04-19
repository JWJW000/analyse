package com.ethicssra.util;

import com.ethicssra.security.SecurityUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static SecurityUserDetails currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof SecurityUserDetails sud)) {
            throw new IllegalStateException("未登录");
        }
        return sud;
    }

    public static Long currentUserId() {
        return currentUser().id();
    }
}
