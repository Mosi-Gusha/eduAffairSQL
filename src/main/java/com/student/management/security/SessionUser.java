package com.student.management.security;

import java.util.Map;

public record SessionUser(
        Long id,
        String username,
        String displayName,
        String email,
        String role,
        String roleName,
        Map<String, Object> profile
) {
}
