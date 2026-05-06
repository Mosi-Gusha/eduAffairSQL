package com.student.management.controller;

import com.student.management.common.ApiResponse;
import com.student.management.dto.LoginRequest;
import com.student.management.dto.LoginResponse;
import com.student.management.dto.ChangePasswordRequest;
import com.student.management.security.RequireRole;
import com.student.management.security.SessionUser;
import com.student.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/health")
    public ApiResponse<Object> health() {
        return ApiResponse.ok(java.util.Map.of("status", "ok"));
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/api/auth/logout")
    @RequireRole
    public ApiResponse<Object> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(authorization);
        return ApiResponse.ok(java.util.Map.of("message", "已退出"));
    }

    @GetMapping("/api/me")
    @RequireRole
    public ApiResponse<SessionUser> me(SessionUser user) {
        return ApiResponse.ok(user);
    }

    @PostMapping("/api/auth/password")
    @RequireRole
    public ApiResponse<Object> changePassword(SessionUser user, @Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.ok(authService.changePassword(user, request));
    }
}
