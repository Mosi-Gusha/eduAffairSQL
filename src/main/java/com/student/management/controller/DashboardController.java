package com.student.management.controller;

import java.util.Map;

import com.student.management.common.ApiResponse;
import com.student.management.security.RequireRole;
import com.student.management.security.SessionUser;
import com.student.management.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    @RequireRole
    public ApiResponse<Map<String, Object>> dashboard(SessionUser user) {
        return ApiResponse.ok(dashboardService.dashboard(user));
    }
}
