package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String password,
        @NotBlank(message = "不能为空") String displayName,
        @NotBlank(message = "不能为空") String role
) {
}
