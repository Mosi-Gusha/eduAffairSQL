package com.student.management.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.student.management.common.ApiResponse;
import com.student.management.mapper.CommonMapper;
import com.student.management.security.RequireRole;
import com.student.management.security.SessionUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonController {
    private final CommonMapper commonMapper;

    public CommonController(CommonMapper commonMapper) {
        this.commonMapper = commonMapper;
    }

    @GetMapping("/api/public/landing")
    public ApiResponse<Map<String, Object>> landing() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("currentSemester", commonMapper.currentSemester());
        data.put("notices", commonMapper.listRecentNotices("student", 6));
        return ApiResponse.ok(data);
    }

    @GetMapping("/api/catalog")
    @RequireRole
    public ApiResponse<Map<String, Object>> catalog(SessionUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> currentSemester = commonMapper.currentSemester();
        data.put("semesters", commonMapper.semesters());
        data.put("currentSemester", currentSemester);
        data.put("selectionOpen", isSelectionOpen(currentSemester));
        data.put("notices", commonMapper.listNotices(user.role()));
        return ApiResponse.ok(data);
    }

    private boolean isSelectionOpen(Map<String, Object> semester) {
        if (semester == null) return false;
        String startDate = String.valueOf(semester.getOrDefault("startDate", ""));
        String endDate = String.valueOf(semester.getOrDefault("endDate", ""));
        if (startDate.isEmpty() || endDate.isEmpty()) return false;
        try {
            java.time.LocalDate now = java.time.LocalDate.now();
            java.time.LocalDate start = java.time.LocalDate.parse(startDate);
            java.time.LocalDate end = java.time.LocalDate.parse(endDate);
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }
}
