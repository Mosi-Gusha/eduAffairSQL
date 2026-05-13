package com.student.management.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.student.management.common.ApiException;
import com.student.management.common.MapUtil;
import com.student.management.common.RedisCacheService;
import com.student.management.dto.GradeRequest;
import com.student.management.mapper.CommonMapper;
import com.student.management.mapper.TeacherMapper;
import com.student.management.security.SessionUser;
import org.springframework.stereotype.Service;

@Service
public class TeacherService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {
    };

    private final TeacherMapper teacherMapper;
    private final CommonMapper commonMapper;
    private final RedisCacheService cache;

    public TeacherService(TeacherMapper teacherMapper, CommonMapper commonMapper, RedisCacheService cache) {
        this.teacherMapper = teacherMapper;
        this.commonMapper = commonMapper;
        this.cache = cache;
    }

    public Map<String, Object> dashboard(SessionUser user) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":dashboard", MAP_TYPE, () -> {
            Map<String, Object> row = teacherMapper.dashboard(teacherId);
            return AdminService.mapOf(
                    "summary", List.of(
                            AdminService.item("负责课程", valueOrZero(row, "offeringCount")),
                            AdminService.item("授课学生", valueOrZero(row, "studentCount")),
                            AdminService.item("平均成绩", row.get("avgScore") == null ? "暂无" : row.get("avgScore"))
                    ),
                    "notices", commonMapper.listRecentNotices("teacher", 6)
            );
        });
    }

    public List<Map<String, Object>> courses(SessionUser user) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":courses", LIST_TYPE,
                () -> withOfferingTimes(teacherMapper.courses(teacherId), "id"));
    }

    public List<Map<String, Object>> schedule(SessionUser user) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":schedule", LIST_TYPE,
                () -> withOfferingTimes(teacherMapper.schedule(teacherId), "id"));
    }

    public List<Map<String, Object>> gradeCourses(SessionUser user) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":grade-courses", LIST_TYPE,
                () -> withOfferingTimes(teacherMapper.gradeCourses(teacherId), "id"));
    }

    public List<Map<String, Object>> roster(SessionUser user, Long offeringId) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":roster:" + offeringId, LIST_TYPE,
                () -> teacherMapper.roster(teacherId, offeringId));
    }

    public List<Map<String, Object>> gradeRoster(SessionUser user, Long offeringId) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":grade-roster:" + offeringId, LIST_TYPE,
                () -> teacherMapper.gradeRoster(teacherId, offeringId));
    }

    public Map<String, Object> saveGrade(SessionUser user, GradeRequest request) {
        teacherMapper.callSaveGrade(teacherId(user), request.enrollmentId(), request.usualScore(), request.examScore(), user.id());
        clearTeachingCaches();
        return AdminService.message("成绩已保存");
    }

    private Long teacherId(SessionUser user) {
        if (!user.profile().containsKey("teacherId")) {
            throw new ApiException(403, "当前账号不是教师账号");
        }
        return MapUtil.longValue(user.profile(), "teacherId");
    }

    private Object valueOrZero(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? 0 : value;
    }

    public Map<String, Object> courseGradeStats(SessionUser user, Long offeringId) {
        Long teacherId = teacherId(user);
        return cache.get("teacher:" + teacherId + ":grade-stats:" + offeringId, MAP_TYPE,
                () -> AdminService.nullToEmpty(teacherMapper.courseGradeStats(teacherId, offeringId)));
    }

    private void clearTeachingCaches() {
        cache.evictByPrefix("admin:", "student:", "teacher:");
    }

    private List<Map<String, Object>> withOfferingTimes(List<Map<String, Object>> rows, String idKey) {
        List<Long> offeringIds = AdminService.offeringIds(rows, idKey);
        List<Map<String, Object>> times = offeringIds.isEmpty() ? List.of() : commonMapper.offeringTimes(offeringIds);
        return AdminService.attachOfferingTimes(rows, times, idKey);
    }

}
