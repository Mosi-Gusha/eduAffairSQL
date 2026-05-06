package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;

public record SemesterRequest(
        @NotBlank(message = "不能为空") String name,
        @NotBlank(message = "不能为空") String startDate,
        @NotBlank(message = "不能为空") String endDate
) {
}
