package com.student.management.service;

import java.util.Collections;
import java.util.Map;

import com.student.management.common.ApiException;
import com.student.management.common.MapUtil;
import com.student.management.common.PasswordUtil;
import com.student.management.dto.LoginRequest;
import com.student.management.dto.LoginResponse;
import com.student.management.dto.ChangePasswordRequest;
import com.student.management.mapper.AuthMapper;
import com.student.management.security.SessionRegistry;
import com.student.management.security.SessionUser;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthMapper authMapper;
    private final SessionRegistry sessionRegistry;

    public AuthService(AuthMapper authMapper, SessionRegistry sessionRegistry) {
        this.authMapper = authMapper;
        this.sessionRegistry = sessionRegistry;
    }

    public LoginResponse login(LoginRequest request) {
        Map<String, Object> row = authMapper.findUserByUsername(request.username());
        if (row == null || !"enabled".equals(MapUtil.stringValue(row, "status"))) {
            throw new ApiException(401, "用户名或密码错误");
        }

        String passwordHash = MapUtil.stringValue(row, "passwordHash");
        if (!PasswordUtil.sha256(request.password()).equals(passwordHash)) {
            throw new ApiException(401, "用户名或密码错误");
        }

        Long userId = MapUtil.longValue(row, "id");
        String role = MapUtil.stringValue(row, "role");
        Map<String, Object> profile = switch (role) {
            case "student" -> authMapper.findStudentProfile(userId);
            case "teacher" -> authMapper.findTeacherProfile(userId);
            default -> Collections.emptyMap();
        };
        if (profile == null) {
            profile = Collections.emptyMap();
        }

        SessionUser user = new SessionUser(
                userId,
                MapUtil.stringValue(row, "username"),
                MapUtil.stringValue(row, "displayName"),
                MapUtil.stringValue(row, "email"),
                role,
                MapUtil.stringValue(row, "roleName"),
                profile
        );
        return new LoginResponse(sessionRegistry.create(user), user);
    }

    public void logout(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            sessionRegistry.remove(authorization.substring(7));
        }
    }

    public Map<String, Object> changePassword(SessionUser user, ChangePasswordRequest request) {
        String currentHash = authMapper.passwordHashByUserId(user.id());
        if (!PasswordUtil.sha256(request.oldPassword()).equals(currentHash)) {
            throw new ApiException(400, "原密码错误");
        }
        if (request.newPassword().length() < 2) {
            throw new ApiException(400, "新密码长度不能少于 2 位");
        }
        authMapper.updatePassword(user.id(), PasswordUtil.sha256(request.newPassword()));
        return Map.of("message", "密码已修改，请重新登录");
    }
}
