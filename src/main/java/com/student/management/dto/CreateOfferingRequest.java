package com.student.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateOfferingRequest(
        @NotNull(message = "不能为空") Long courseId,
        @NotNull(message = "不能为空") Long semesterId,
        @NotNull(message = "不能为空") Long teacherId,
        @NotNull(message = "不能为空") Long classroomId,
        @NotNull(message = "不能为空") Integer dayOfWeek,
        @NotNull(message = "不能为空") Integer startSection,
        @NotNull(message = "不能为空") Integer endSection,
        @NotBlank(message = "不能为空") @Pattern(regexp = "all|odd|even", message = "周次只能是全部、单周或双周") String weekType,
        @NotNull(message = "不能为空") Integer capacity,
        String status,
        Double usualRatio,
        Double examRatio
) {
}
