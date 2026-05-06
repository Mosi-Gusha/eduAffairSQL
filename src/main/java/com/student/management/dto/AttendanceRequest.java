package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttendanceRequest(
        @NotNull(message = "不能为空") Long enrollmentId,
        @NotBlank(message = "不能为空") String lessonDate,
        @NotBlank(message = "不能为空") String status,
        String remark
) {
}
