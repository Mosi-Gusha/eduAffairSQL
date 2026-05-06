package com.student.management.dto;

import com.student.management.security.SessionUser;

public record LoginResponse(String token, SessionUser user) {
}
