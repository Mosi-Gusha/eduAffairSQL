package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "不能为空") String oldPassword,
        @NotBlank(message = "不能为空") String newPassword
) {
}
