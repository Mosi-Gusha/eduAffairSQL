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
            Map<String, Object> selectionSemester = commonMapper.selectionSemester();
            Map<String, Object> semester = selectionSemester == null ? commonMapper.currentSemester() : selectionSemester;
            if (semester == null) {
                throw new ApiException(500, "暂无可用学期");
            }
            Long semesterId = MapUtil.longValue(semester, "id");
            return AdminService.mapOf(
                    "semester", semester,
                    "selectionSemester", selectionSemester,
                    "selectionOpen", selectionSemester != null,
                    "rows", selectionSemester == null
                            ? List.of()
                            : withOfferingTimes(studentMapper.listCurrentOfferings(studentId, semesterId, keyword), "id")
            );
        });
    }

    public Map<String, Object> selectCourse(SessionUser user, Long offeringId) {
        studentMapper.callSelectCourse(studentId(user), offeringId, user.id());
        clearTeachingCaches();
        return AdminService.message("选课成功");
    }

    public Map<String, Object> dropCourse(SessionUser user, Long enrollmentId) {
        studentMapper.callStudentDropCourse(studentId(user), enrollmentId, user.id());
        clearTeachingCaches();
        return AdminService.message("退课成功");
    }

    public List<Map<String, Object>> schedule(SessionUser user, Long semesterId) {
        Long studentId = studentId(user);
        return cache.get("student:" + studentId + ":schedule:" + cache.keyPart(semesterId), LIST_TYPE,
                () -> withOfferingTimes(studentMapper.schedule(studentId, semesterId), "offeringId"));
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

    private List<Map<String, Object>> withOfferingTimes(List<Map<String, Object>> rows, String idKey) {
        List<Long> offeringIds = AdminService.offeringIds(rows, idKey);
        List<Map<String, Object>> times = offeringIds.isEmpty() ? List.of() : commonMapper.offeringTimes(offeringIds);
        return AdminService.attachOfferingTimes(rows, times, idKey);
    }

}
