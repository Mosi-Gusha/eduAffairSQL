package com.student.management.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.student.management.common.ApiException;
import com.student.management.common.MapUtil;
import com.student.management.common.RedisCacheService;
import com.student.management.mapper.CommonMapper;
import com.student.management.mapper.StudentMapper;
import com.student.management.security.SessionUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {
    };

    private final StudentMapper studentMapper;
    private final CommonMapper commonMapper;
    private final RedisCacheService cache;

    public StudentService(StudentMapper studentMapper, CommonMapper commonMapper, RedisCacheService cache) {
        this.studentMapper = studentMapper;
        this.commonMapper = commonMapper;
        this.cache = cache;
    }

    public Map<String, Object> dashboard(SessionUser user) {
        Long studentId = studentId(user);
        return cache.get("student:" + studentId + ":dashboard", MAP_TYPE, () -> {
            Map<String, Object> row = studentMapper.dashboard(studentId);
            return AdminService.mapOf(
                    "summary", List.of(
                            AdminService.item("已选课程", valueOrZero(row, "selectedCourses")),
                            AdminService.item("已选学分", valueOrZero(row, "selectedCredits")),
                            AdminService.item("平均绩点", row.get("gpa") == null ? "-" : row.get("gpa"))
                    ),
                    "notices", commonMapper.listRecentNotices("student", 6)
            );
        });
    }

    public Map<String, Object> offerings(SessionUser user, String keyword) {
        Long studentId = studentId(user);
        return cache.get("student:" + studentId + ":offerings:" + cache.keyPart(keyword), MAP_TYPE, () -> {
            Map<String, Object> semester = commonMapper.currentSemester();
            if (semester == null) {
                throw new ApiException(500, "未设置当前学期");
            }
            Long semesterId = MapUtil.longValue(semester, "id");
            return AdminService.mapOf(
                    "semester", semester,
                    "selectionOpen", selectionOpen(),
                    "rows", studentMapper.listCurrentOfferings(studentId, semesterId, keyword)
            );
        });
    }

    @Transactional
    public Map<String, Object> selectCourse(SessionUser user, Long offeringId) {
        if (!selectionOpen()) {
            throw new ApiException(400, "当前不在选课时间内，学生不能选课");
        }
        studentMapper.callSelectCourse(studentId(user), offeringId);
        clearTeachingCaches();
        return AdminService.message("选课成功");
    }

    @Transactional
    public Map<String, Object> dropCourse(SessionUser user, Long enrollmentId) {
        if (!selectionOpen()) {
            throw new ApiException(400, "当前不在选课时间内，学生不能退课");
        }
        // Check if this course has already been graded
        if (studentMapper.isEnrollmentGraded(enrollmentId)) {
            throw new ApiException(400, "该课程已被教师登分，不能退课");
        }
        int updated = studentMapper.dropCourse(studentId(user), enrollmentId);
        clearTeachingCaches();
        if (updated == 0) {
            throw new ApiException(400, "未找到可退选课程");
        }
        return AdminService.message("退课成功");
    }

    public List<Map<String, Object>> schedule(SessionUser user) {
        Long studentId = studentId(user);
        return cache.get("student:" + studentId + ":schedule", LIST_TYPE,
                () -> studentMapper.schedule(studentId));
    }

    public List<Map<String, Object>> grades(SessionUser user) {
        Long studentId = studentId(user);
        return cache.get("student:" + studentId + ":grades", LIST_TYPE,
                () -> studentMapper.grades(studentId));
    }

    public Map<String, Object> transcript(SessionUser user, Long semesterId) {
        Long studentId = studentId(user);
        return cache.get("student:" + studentId + ":transcript:" + cache.keyPart(semesterId), MAP_TYPE,
                () -> AdminService.mapOf(
                        "rows", studentMapper.transcript(studentId, semesterId)
                ));
    }

    private Long studentId(SessionUser user) {
        if (!user.profile().containsKey("studentId")) {
            throw new ApiException(403, "当前账号不是学生");
        }
        return MapUtil.longValue(user.profile(), "studentId");
    }

    private Object valueOrZero(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0 : value;
    }

    private void clearTeachingCaches() {
        cache.evictByPrefix("admin:", "student:", "teacher:");
    }

    private boolean selectionOpen() {
        Map<String, Object> semester = commonMapper.currentSemester();
        if (semester == null) {
            return false;
        }
        String startDate = MapUtil.stringValue(semester, "startDate");
        String endDate = MapUtil.stringValue(semester, "endDate");
        if (startDate == null || endDate == null) {
            return false;
        }
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
