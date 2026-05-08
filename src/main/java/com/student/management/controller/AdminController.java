package com.student.management.controller;

import java.util.List;
import java.util.Map;

import com.student.management.common.ApiResponse;
import com.student.management.dto.CourseRequest;
import com.student.management.dto.CreateOfferingRequest;
import com.student.management.dto.CreateUserRequest;
import com.student.management.dto.NoticeRequest;
import com.student.management.dto.SemesterRequest;
import com.student.management.dto.StudentProfileRequest;
import com.student.management.dto.TeacherRequest;
import com.student.management.security.RequireRole;
import com.student.management.security.SessionUser;
import com.student.management.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequireRole("admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.ok(adminService.listUsers());
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(adminService.createUser(request));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Map<String, Object>> deleteUser(SessionUser user, @PathVariable Long userId) {
        return ApiResponse.ok(adminService.deleteUser(user, userId));
    }

    @GetMapping("/teachers")
    public ApiResponse<List<Map<String, Object>>> teachers(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.listTeachers(keyword));
    }

    @PostMapping("/teachers")
    public ApiResponse<Map<String, Object>> createTeacher(@Valid @RequestBody TeacherRequest request) {
        return ApiResponse.ok(adminService.createTeacher(request));
    }

    @PutMapping("/teachers/{teacherId}")
    public ApiResponse<Map<String, Object>> updateTeacher(@PathVariable Long teacherId,
                                                          @Valid @RequestBody TeacherRequest request) {
        return ApiResponse.ok(adminService.updateTeacher(teacherId, request));
    }

    @PostMapping("/teachers/{teacherId}/disable")
    public ApiResponse<Map<String, Object>> disableTeacher(@PathVariable Long teacherId) {
        return ApiResponse.ok(adminService.disableTeacher(teacherId));
    }

    @PostMapping("/teachers/{teacherId}/enable")
    public ApiResponse<Map<String, Object>> enableTeacher(@PathVariable Long teacherId) {
        return ApiResponse.ok(adminService.enableTeacher(teacherId));
    }

    @GetMapping("/students")
    public ApiResponse<List<Map<String, Object>>> students(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.listStudents(keyword));
    }

    @PostMapping("/students")
    public ApiResponse<Map<String, Object>> createStudent(@Valid @RequestBody StudentProfileRequest request) {
        return ApiResponse.ok(adminService.createStudent(request));
    }

    @PutMapping("/students/{studentId}")
    public ApiResponse<Map<String, Object>> updateStudent(@PathVariable Long studentId,
                                                          @Valid @RequestBody StudentProfileRequest request) {
        return ApiResponse.ok(adminService.updateStudent(studentId, request));
    }

    @PostMapping("/students/{studentId}/disable")
    public ApiResponse<Map<String, Object>> disableStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(adminService.disableStudent(studentId));
    }

    @PostMapping("/students/{studentId}/enable")
    public ApiResponse<Map<String, Object>> enableStudent(@PathVariable Long studentId) {
        return ApiResponse.ok(adminService.enableStudent(studentId));
    }

    @GetMapping("/catalog")
    public ApiResponse<Map<String, Object>> catalog() {
        return ApiResponse.ok(adminService.catalog());
    }

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(adminService.listCourses(keyword));
    }

    @PostMapping("/courses")
    public ApiResponse<Map<String, Object>> createCourse(@Valid @RequestBody CourseRequest request) {
        return ApiResponse.ok(adminService.createCourse(request));
    }

    @PostMapping("/courses/{courseId}/enable")
    public ApiResponse<Map<String, Object>> enableCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(adminService.enableCourse(courseId));
    }

    @PostMapping("/courses/{courseId}/disable")
    public ApiResponse<Map<String, Object>> disableCourse(@PathVariable Long courseId) {
        return ApiResponse.ok(adminService.disableCourse(courseId));
    }

    @GetMapping("/offerings")
    public ApiResponse<List<Map<String, Object>>> offerings(@RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "false") boolean currentOnly) {
        return ApiResponse.ok(adminService.listOfferings(keyword, currentOnly));
    }

    @PostMapping("/offerings")
    public ApiResponse<Map<String, Object>> createOffering(@Valid @RequestBody CreateOfferingRequest request) {
        return ApiResponse.ok(adminService.createOffering(request));
    }

    @PutMapping("/offerings/{offeringId}")
    public ApiResponse<Map<String, Object>> updateOffering(@PathVariable Long offeringId,
                                                           @Valid @RequestBody CreateOfferingRequest request) {
        return ApiResponse.ok(adminService.updateOffering(offeringId, request));
    }

    @DeleteMapping("/offerings/{offeringId}")
    public ApiResponse<Map<String, Object>> deleteOffering(@PathVariable Long offeringId) {
        return ApiResponse.ok(adminService.deleteOffering(offeringId));
    }

    @PostMapping("/semesters/{semesterId}/current")
    public ApiResponse<Map<String, Object>> setCurrentSemester(@PathVariable Long semesterId) {
        return ApiResponse.ok(adminService.setCurrentSemester(semesterId));
    }

    @PostMapping("/semesters")
    public ApiResponse<Map<String, Object>> createSemester(@Valid @RequestBody SemesterRequest request) {
        return ApiResponse.ok(adminService.createSemester(request));
    }

    @PutMapping("/semesters/{semesterId}")
    public ApiResponse<Map<String, Object>> updateSemester(@PathVariable Long semesterId,
                                                           @Valid @RequestBody SemesterRequest request) {
        return ApiResponse.ok(adminService.updateSemester(semesterId, request));
    }

    @GetMapping("/enrollment-report")
    public ApiResponse<List<Map<String, Object>>> enrollmentReport() {
        return ApiResponse.ok(adminService.enrollmentReport());
    }

    @GetMapping("/offerings/{offeringId}/roster")
    public ApiResponse<List<Map<String, Object>>> courseRoster(@PathVariable Long offeringId) {
        return ApiResponse.ok(adminService.courseRoster(offeringId));
    }

    @GetMapping("/teachers/{teacherId}/offerings")
    public ApiResponse<List<Map<String, Object>>> teacherOfferings(@PathVariable Long teacherId) {
        return ApiResponse.ok(adminService.teacherCurrentOfferings(teacherId));
    }

    @GetMapping("/students/{studentId}/enrollments")
    public ApiResponse<List<Map<String, Object>>> studentEnrollments(@PathVariable Long studentId) {
        return ApiResponse.ok(adminService.studentCurrentEnrollments(studentId));
    }

    @GetMapping("/offerings/{offeringId}/grade-stats")
    public ApiResponse<Map<String, Object>> courseGradeStats(@PathVariable Long offeringId) {
        return ApiResponse.ok(adminService.courseGradeStats(offeringId));
    }

    @PostMapping("/teaching/select")
    public ApiResponse<Map<String, Object>> adminSelectCourse(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(adminService.adminSelectCourse(
                String.valueOf(body.get("studentNo")),
                Long.valueOf(String.valueOf(body.get("offeringId")))
        ));
    }

    @PostMapping("/teaching/drop")
    public ApiResponse<Map<String, Object>> adminDropCourse(@RequestBody Map<String, Object> body) {
        if (body.get("enrollmentId") != null) {
            return ApiResponse.ok(adminService.adminDropEnrollment(Long.valueOf(String.valueOf(body.get("enrollmentId")))));
        }
        return ApiResponse.ok(adminService.adminDropCourse(
                String.valueOf(body.get("studentNo")),
                Long.valueOf(String.valueOf(body.get("offeringId")))
        ));
    }

    @GetMapping("/students/{studentNo}/teaching")
    public ApiResponse<Map<String, Object>> studentTeaching(@PathVariable String studentNo) {
        return ApiResponse.ok(adminService.studentTeachingInfo(studentNo));
    }

    @PostMapping("/notices")
    public ApiResponse<Map<String, Object>> createNotice(SessionUser user, @Valid @RequestBody NoticeRequest request) {
        return ApiResponse.ok(adminService.createNotice(user, request));
    }

    @PutMapping("/notices/{noticeId}")
    public ApiResponse<Map<String, Object>> updateNotice(@PathVariable Long noticeId,
                                                         @Valid @RequestBody NoticeRequest request) {
        return ApiResponse.ok(adminService.updateNotice(noticeId, request));
    }

    @DeleteMapping("/notices/{noticeId}")
    public ApiResponse<Map<String, Object>> deleteNotice(@PathVariable Long noticeId) {
        return ApiResponse.ok(adminService.deleteNotice(noticeId));
    }
}
