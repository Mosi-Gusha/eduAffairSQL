package com.student.management.service;

import java.util.Map;

import com.student.management.security.SessionUser;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final AdminService adminService;
    private final TeacherService teacherService;
    private final StudentService studentService;

    public DashboardService(AdminService adminService, TeacherService teacherService, StudentService studentService) {
        this.adminService = adminService;
        this.teacherService = teacherService;
        this.studentService = studentService;
    }

    public Map<String, Object> dashboard(SessionUser user) {
        return switch (user.role()) {
            case "admin" -> adminService.dashboard();
            case "teacher" -> teacherService.dashboard(user);
            case "student" -> studentService.dashboard(user);
            default -> Map.of();
        };
    }
}
