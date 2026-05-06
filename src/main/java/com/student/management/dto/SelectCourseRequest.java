package com.student.management.dto;

import jakarta.validation.constraints.NotNull;

public record SelectCourseRequest(@NotNull(message = "不能为空") Long offeringId) {
}
