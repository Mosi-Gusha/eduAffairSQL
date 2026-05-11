package com.student.management.controller;

import java.util.List;
import java.util.Map;

import com.student.management.common.ApiResponse;
import com.student.management.dto.GradeRequest;
import com.student.management.security.RequireRole;
import com.student.management.security.SessionUser;
import com.student.management.service.TeacherService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequireRole("teacher")
public class TeacherController {
    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses(SessionUser user) {
        return ApiResponse.ok(teacherService.courses(user));
    }

    @GetMapping("/schedule")
    public ApiResponse<List<Map<String, Object>>> schedule(SessionUser user) {
        return ApiResponse.ok(teacherService.schedule(user));
    }

    @GetMapping("/grade-courses")
    public ApiResponse<List<Map<String, Object>>> gradeCourses(SessionUser user) {
        return ApiResponse.ok(teacherService.gradeCourses(user));
    }

    @GetMapping("/roster")
    public ApiResponse<List<Map<String, Object>>> roster(SessionUser user, @RequestParam Long offeringId) {
        return ApiResponse.ok(teacherService.roster(user, offeringId));
    }

    @GetMapping("/grade-roster")
    public ApiResponse<List<Map<String, Object>>> gradeRoster(SessionUser user, @RequestParam Long offeringId) {
        return ApiResponse.ok(teacherService.gradeRoster(user, offeringId));
    }

    @PostMapping("/grades")
    public ApiResponse<Map<String, Object>> saveGrade(SessionUser user, @Valid @RequestBody GradeRequest request) {
        return ApiResponse.ok(teacherService.saveGrade(user, request));
    }

    @GetMapping("/grade-stats")
    public ApiResponse<Map<String, Object>> gradeStats(SessionUser user, @RequestParam Long offeringId) {
        return ApiResponse.ok(teacherService.courseGradeStats(user, offeringId));
    }
}
