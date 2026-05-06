package com.student.management.dto;

import jakarta.validation.constraints.NotNull;

public record DropCourseRequest(@NotNull(message = "不能为空") Long enrollmentId) {
}
