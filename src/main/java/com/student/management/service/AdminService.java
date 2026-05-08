package com.student.management.service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.student.management.common.ApiException;
import com.student.management.common.MapUtil;
import com.student.management.common.PasswordUtil;
import com.student.management.common.RedisCacheService;
import com.student.management.dto.CourseRequest;
import com.student.management.dto.CreateOfferingRequest;
import com.student.management.dto.CreateUserRequest;
import com.student.management.dto.NoticeRequest;
import com.student.management.dto.SemesterRequest;
import com.student.management.dto.StudentProfileRequest;
import com.student.management.dto.TeacherRequest;
import com.student.management.mapper.AdminMapper;
import com.student.management.mapper.CommonMapper;
import com.student.management.security.SessionUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> LIST_TYPE = new TypeReference<>() {
    };

    private final AdminMapper adminMapper;
    private final CommonMapper commonMapper;
    private final RedisCacheService cache;

    public AdminService(AdminMapper adminMapper, CommonMapper commonMapper, RedisCacheService cache) {
        this.adminMapper = adminMapper;
        this.commonMapper = commonMapper;
        this.cache = cache;
    }

    public Map<String, Object> dashboard() {
        return mapOf(
                "summary", List.of(
                        item("系统用户", adminMapper.countUsers()),
                        item("启用课程", adminMapper.countEnabledCourses()),
                        item("开课计划", adminMapper.countOfferings()),
                        item("有效选课", adminMapper.countSelectedEnrollments())
                ),
                "systemStatus", systemStatus(),
                "notices", commonMapper.listRecentNotices("admin", 5)
        );
    }

    public List<Map<String, Object>> listUsers() {
        return cache.get("admin:users", LIST_TYPE, adminMapper::listUsers);
    }

    @Transactional
    public Map<String, Object> createUser(CreateUserRequest request) {
        if ("admin".equals(request.role())) {
            throw new ApiException(400, "系统管理员账号只有一个，不能新增管理员");
        }
        Long roleId = adminMapper.roleIdByCode(request.role());
        if (roleId == null) {
            throw new ApiException(400, "角色不存在");
        }
        adminMapper.insertUser(
                request.username(),
                PasswordUtil.hash(request.password()),
                request.displayName(),
                defaultEmail(request.username(), request.role()),
                roleId
        );
        clearTeachingCaches();
        return message("用户已创建");
    }

    public List<Map<String, Object>> listTeachers(String keyword) {
        return cache.get("admin:teachers:" + cache.keyPart(keyword), LIST_TYPE,
                () -> adminMapper.listTeachers(keyword));
    }

    @Transactional
    public Map<String, Object> createTeacher(TeacherRequest request) {
        Long roleId = adminMapper.roleIdByCode("teacher");
        adminMapper.insertUser(request.teacherNo(), PasswordUtil.hash(request.teacherNo()), request.name(), request.email(), roleId);
        Long userId = adminMapper.userIdByUsername(request.teacherNo());
        adminMapper.insertTeacher(userId, request);
        clearTeachingCaches();
        return message("教师已新增，初始密码为教师号");
    }

    @Transactional
    public Map<String, Object> updateTeacher(Long teacherId, TeacherRequest request) {
        Long userId = adminMapper.teacherUserId(teacherId);
        if (userId == null) {
            throw new ApiException(404, "教师不存在");
        }
        adminMapper.updateUserIdentity(userId, request.teacherNo(), request.name());
        adminMapper.updateUserEmail(userId, request.email());
        adminMapper.updateTeacher(teacherId, request);
        clearTeachingCaches();
        return message("教师信息已更新");
    }

    @Transactional
    public Map<String, Object> disableTeacher(Long teacherId) {
        Long userId = adminMapper.teacherUserId(teacherId);
        if (userId == null) {
            throw new ApiException(404, "教师不存在");
        }
        adminMapper.updateUserStatus(userId, "disabled");
        clearTeachingCaches();
        return message("教师已弃用");
    }

    @Transactional
    public Map<String, Object> enableTeacher(Long teacherId) {
        Long userId = adminMapper.teacherUserId(teacherId);
        if (userId == null) {
            throw new ApiException(404, "教师不存在");
        }
        adminMapper.updateUserStatus(userId, "enabled");
        clearTeachingCaches();
        return message("教师已启用");
    }

    public List<Map<String, Object>> listStudents(String keyword) {
        return cache.get("admin:students:" + cache.keyPart(keyword), LIST_TYPE,
                () -> adminMapper.listStudents(keyword));
    }

    @Transactional
    public Map<String, Object> createStudent(StudentProfileRequest request) {
        Long roleId = adminMapper.roleIdByCode("student");
        adminMapper.insertUser(request.studentNo(), PasswordUtil.hash(request.studentNo()), request.name(),
                request.email(), roleId);
        Long userId = adminMapper.userIdByUsername(request.studentNo());
        adminMapper.insertStudent(userId, request);
        clearTeachingCaches();
        return message("学生已新增，初始密码为学号");
    }

    @Transactional
    public Map<String, Object> updateStudent(Long studentId, StudentProfileRequest request) {
        Long userId = adminMapper.studentUserId(studentId);
        if (userId == null) {
            throw new ApiException(404, "学生不存在");
        }
        adminMapper.updateUserIdentity(userId, request.studentNo(), request.name());
        adminMapper.updateUserEmail(userId, request.email());
        adminMapper.updateStudent(studentId, request);
        clearTeachingCaches();
        return message("学生信息已更新");
    }

    @Transactional
    public Map<String, Object> disableStudent(Long studentId) {
        Long userId = adminMapper.studentUserId(studentId);
        if (userId == null) {
            throw new ApiException(404, "学生不存在");
        }
        adminMapper.updateUserStatus(userId, "disabled");
        clearTeachingCaches();
        return message("学生已弃用");
    }

    @Transactional
    public Map<String, Object> enableStudent(Long studentId) {
        Long userId = adminMapper.studentUserId(studentId);
        if (userId == null) {
            throw new ApiException(404, "学生不存在");
        }
        adminMapper.updateUserStatus(userId, "enabled");
        clearTeachingCaches();
        return message("学生已启用");
    }

    @Transactional
    public Map<String, Object> deleteUser(SessionUser currentUser, Long userId) {
        if (currentUser.id().equals(userId)) {
            throw new ApiException(400, "不能删除当前登录账号");
        }
        String role = adminMapper.roleCodeByUserId(userId);
        if (role == null) {
            throw new ApiException(404, "用户不存在");
        }
        if ("admin".equals(role)) {
            throw new ApiException(400, "系统管理员账号只有一个，不能删除");
        }
        adminMapper.updateUserStatus(userId, "disabled");
        clearTeachingCaches();
        return message("用户已删除");
    }

    public Map<String, Object> catalog() {
        return cache.get("admin:catalog", MAP_TYPE, () -> {
            Map<String, Object> currentSemester = commonMapper.currentSemester();
            return mapOf(
                    "semesters", adminMapper.semesters(),
                    "departments", adminMapper.departments(),
                    "majors", adminMapper.majors(),
                    "teachers", adminMapper.teachers(),
                    "courses", adminMapper.courses(),
                    "classrooms", adminMapper.classrooms(),
                    "currentSemester", currentSemester,
                    "selectionOpen", selectionOpen()
            );
        });
    }

    public List<Map<String, Object>> listOfferings(String keyword, boolean currentOnly) {
        return cache.get("admin:offerings:" + currentOnly + ":" + cache.keyPart(keyword), LIST_TYPE,
                () -> withOfferingTimes(adminMapper.listOfferings(keyword, currentOnly), "id"));
    }

    public List<Map<String, Object>> listCourses(String keyword) {
        return cache.get("admin:courses:" + cache.keyPart(keyword), LIST_TYPE,
                () -> adminMapper.listCourses(keyword));
    }

    @Transactional
    public Map<String, Object> createCourse(CourseRequest request) {
        adminMapper.insertCourse(request);
        clearTeachingCaches();
        return message("课程已新增");
    }

    @Transactional
    public Map<String, Object> enableCourse(Long courseId) {
        int updated = adminMapper.updateCourseStatus(courseId, "enabled");
        if (updated == 0) {
            throw new ApiException(404, "课程不存在");
        }
        clearTeachingCaches();
        return message("课程已启用");
    }

    @Transactional
    public Map<String, Object> disableCourse(Long courseId) {
        int updated = adminMapper.updateCourseStatus(courseId, "disabled");
        if (updated == 0) {
            throw new ApiException(404, "课程不存在");
        }
        clearTeachingCaches();
        return message("课程已弃用");
    }

    @Transactional
    public Map<String, Object> createOffering(CreateOfferingRequest request) {
        validateOfferingTimes(request.times());
        validateCourseForNewOffering(request.courseId());
        validateRatio(request.usualRatio(), request.examRatio());
        adminMapper.insertOffering(request);
        Long offeringId = adminMapper.lastInsertId();
        adminMapper.insertOfferingTimes(offeringId, request.times());
        clearTeachingCaches();
        return message("开课计划已创建");
    }

    @Transactional
    public Map<String, Object> updateOffering(Long offeringId, CreateOfferingRequest request) {
        validateOfferingTimes(request.times());
        validateRatio(request.usualRatio(), request.examRatio());
        adminMapper.updateOffering(offeringId, request);
        adminMapper.deleteOfferingTimes(offeringId);
        adminMapper.insertOfferingTimes(offeringId, request.times());
        clearTeachingCaches();
        return message("课程信息已更新");
    }

    @Transactional
    public Map<String, Object> deleteOffering(Long offeringId) {
        adminMapper.deleteGradesByOffering(offeringId);
        adminMapper.deleteEnrollmentsByOffering(offeringId);
        adminMapper.deleteOffering(offeringId);
        clearTeachingCaches();
        return message("课程班已删除，相关选课和成绩记录已一并清除");
    }

    @Transactional
    public Map<String, Object> setCurrentSemester(Long semesterId) {
        Map<String, Object> semester = adminMapper.semesterById(semesterId);
        if (semester == null) {
            throw new ApiException(404, "学期不存在");
        }
        adminMapper.clearCurrentSemester();
        adminMapper.setCurrentSemester(semesterId);
        clearTeachingCaches();
        return message("当前学期已切换");
    }

    @Transactional
    public Map<String, Object> createSemester(SemesterRequest request) {
        adminMapper.clearCurrentSemester();
        adminMapper.insertSemester(request);
        clearTeachingCaches();
        return message("学期已新建，可开始维护该学期课程");
    }

    @Transactional
    public Map<String, Object> updateSemester(Long semesterId, SemesterRequest request) {
        int updated = adminMapper.updateSemester(semesterId, request);
        if (updated == 0) {
            throw new ApiException(400, "学期不存在");
        }
        clearTeachingCaches();
        return message("学期信息已更新");
    }

    public List<Map<String, Object>> enrollmentReport() {
        return cache.get("admin:enrollment-report", LIST_TYPE, adminMapper::enrollmentReport);
    }

    public List<Map<String, Object>> courseRoster(Long offeringId) {
        return cache.get("admin:course-roster:" + offeringId, LIST_TYPE,
                () -> adminMapper.courseRoster(offeringId));
    }

    @Transactional
    public Map<String, Object> adminSelectCourse(String studentNo, Long offeringId) {
        Long studentId = adminMapper.studentIdByNoOrUsername(studentNo);
        if (studentId == null) {
            throw new ApiException(404, "学生不存在");
        }
        adminMapper.adminSelectCourse(studentId, offeringId);
        clearTeachingCaches();
        return message("已为学生选课");
    }

    @Transactional
    public Map<String, Object> adminDropCourse(String studentNo, Long offeringId) {
        Long studentId = adminMapper.studentIdByNoOrUsername(studentNo);
        if (studentId == null) {
            throw new ApiException(404, "学生不存在");
        }
        adminMapper.adminDropCourse(studentId, offeringId);
        clearTeachingCaches();
        return message("已为学生退课");
    }

    @Transactional
    public Map<String, Object> adminDropEnrollment(Long enrollmentId) {
        int updated = adminMapper.adminDropEnrollment(enrollmentId);
        clearTeachingCaches();
        if (updated == 0) {
            throw new ApiException(400, "未找到有效选课记录");
        }
        return message("已退选");
    }

    public Map<String, Object> studentTeachingInfo(String studentNo) {
        Long studentId = adminMapper.studentIdByNoOrUsername(studentNo);
        if (studentId == null) {
            throw new ApiException(404, "学生不存在");
        }
        return cache.get("admin:student-teaching:" + studentId, MAP_TYPE, () -> mapOf(
                "enrollments", withOfferingTimes(adminMapper.studentEnrollments(studentId), "offeringId"),
                "transcript", adminMapper.studentTranscript(studentId)
        ));
    }

    public Map<String, Object> courseGradeStats(Long offeringId) {
        return cache.get("admin:course-grade-stats:" + offeringId, MAP_TYPE,
                () -> nullToEmpty(adminMapper.courseGradeStats(offeringId)));
    }

    @Transactional
    public Map<String, Object> createNotice(SessionUser user, NoticeRequest request) {
        adminMapper.insertNotice(request, user.id());
        clearTeachingCaches();
        return message("通知已发布");
    }

    @Transactional
    public Map<String, Object> updateNotice(Long noticeId, NoticeRequest request) {
        int updated = adminMapper.updateNotice(noticeId, request);
        if (updated == 0) {
            throw new ApiException(404, "通知不存在");
        }
        clearTeachingCaches();
        return message("通知已更新");
    }

    @Transactional
    public Map<String, Object> deleteNotice(Long noticeId) {
        int deleted = adminMapper.deleteNotice(noticeId);
        if (deleted == 0) {
            throw new ApiException(404, "通知不存在");
        }
        clearTeachingCaches();
        return message("通知已删除");
    }

    static Map<String, Object> item(String label, Object value) {
        return mapOf("label", label, "value", value == null ? "-" : value);
    }

    static Map<String, Object> message(String message) {
        return mapOf("message", message);
    }

    static Map<String, Object> nullToEmpty(Map<String, Object> map) {
        return map == null ? Map.of() : map;
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private void clearTeachingCaches() {
        cache.evictByPrefix("admin:", "student:", "teacher:");
    }

    private String defaultEmail(String username, String role) {
        String domain = switch (role) {
            case "teacher" -> "teacher.school.edu.cn";
            case "student" -> "student.school.edu.cn";
            default -> "school.edu.cn";
        };
        return username + "@" + domain;
    }

    private Map<String, Object> systemStatus() {
        java.lang.management.OperatingSystemMXBean baseOs = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = -1;
        long totalMemory = -1;
        long freeMemory = -1;
        if (baseOs instanceof com.sun.management.OperatingSystemMXBean os) {
            cpuLoad = os.getSystemCpuLoad();
            totalMemory = os.getTotalPhysicalMemorySize();
            freeMemory = os.getFreePhysicalMemorySize();
        }
        if (totalMemory <= 0) {
            Runtime runtime = Runtime.getRuntime();
            totalMemory = runtime.maxMemory();
            freeMemory = runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory());
        }
        long usedMemory = Math.max(totalMemory - freeMemory, 0);

        long totalDisk = 0;
        long freeDisk = 0;
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File root : roots) {
                totalDisk += Math.max(root.getTotalSpace(), 0);
                freeDisk += Math.max(root.getFreeSpace(), 0);
            }
        }
        long usedDisk = Math.max(totalDisk - freeDisk, 0);

        return mapOf(
                "cpu", usageMetric("cpu", percentFromLoad(cpuLoad), baseOs.getAvailableProcessors() + " cores"),
                "memory", usageMetric("memory", percent(usedMemory, totalMemory), formatCapacity(usedMemory, totalMemory)),
                "disk", usageMetric("disk", percent(usedDisk, totalDisk), formatCapacity(usedDisk, totalDisk)),
                "network", networkStatus()
        );
    }

    private Map<String, Object> usageMetric(String label, double value, String detail) {
        return mapOf("label", label, "value", value, "detail", detail);
    }

    private Map<String, Object> networkStatus() {
        int activeAdapters = 0;
        int availableAdapters = 0;
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                availableAdapters++;
                if (networkInterface.isUp()) {
                    activeAdapters++;
                }
            }
        } catch (SocketException ignored) {
            return mapOf(
                    "label", "network",
                    "value", 0.0,
                    "status", "unknown",
                    "activeAdapters", 0,
                    "availableAdapters", 0
            );
        }
        boolean online = activeAdapters > 0;
        return mapOf(
                "label", "network",
                "value", online ? 100.0 : 0.0,
                "status", online ? "online" : "offline",
                "activeAdapters", activeAdapters,
                "availableAdapters", Math.max(availableAdapters, activeAdapters)
        );
    }

    private double percentFromLoad(double load) {
        if (load < 0) {
            return 0.0;
        }
        return round(load * 100);
    }

    private double percent(long used, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return round((double) used / (double) total * 100.0);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatCapacity(long used, long total) {
        if (total <= 0) {
            return "无法读取";
        }
        double gib = 1024.0 * 1024.0 * 1024.0;
        return String.format(Locale.ROOT, "%.1f / %.1f GB", used / gib, total / gib);
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

    public List<Map<String, Object>> teacherCurrentOfferings(Long teacherId) {
        return cache.get("admin:teacher-offerings:" + teacherId, LIST_TYPE,
                () -> withOfferingTimes(adminMapper.teacherCurrentOfferings(teacherId), "id"));
    }

    public List<Map<String, Object>> studentCurrentEnrollments(Long studentId) {
        return cache.get("admin:student-enrollments:" + studentId, LIST_TYPE,
                () -> withOfferingTimes(adminMapper.studentCurrentEnrollments(studentId), "offeringId"));
    }

    private List<Map<String, Object>> withOfferingTimes(List<Map<String, Object>> rows, String idKey) {
        List<Long> offeringIds = offeringIds(rows, idKey);
        List<Map<String, Object>> times = offeringIds.isEmpty() ? List.of() : commonMapper.offeringTimes(offeringIds);
        return attachOfferingTimes(rows, times, idKey);
    }

    static List<Long> offeringIds(List<Map<String, Object>> rows, String idKey) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object value = row.get(idKey);
            if (value == null) {
                continue;
            }
            Long id = value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
            if (!ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    static List<Map<String, Object>> attachOfferingTimes(List<Map<String, Object>> rows,
                                                          List<Map<String, Object>> times,
                                                          String idKey) {
        Map<Long, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> time : times) {
            Long offeringId = MapUtil.longValue(time, "offeringId");
            grouped.computeIfAbsent(offeringId, ignored -> new ArrayList<>()).add(time);
        }
        for (Map<String, Object> row : rows) {
            Object value = row.get(idKey);
            if (value == null) {
                continue;
            }
            Long offeringId = value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
            List<Map<String, Object>> rowTimes = grouped.getOrDefault(offeringId, List.of());
            row.put("times", rowTimes);
            if (!rowTimes.isEmpty()) {
                Map<String, Object> first = rowTimes.get(0);
                row.put("dayOfWeek", first.get("dayOfWeek"));
                row.put("startSection", first.get("startSection"));
                row.put("endSection", first.get("endSection"));
                row.put("startWeek", first.get("startWeek"));
                row.put("endWeek", first.get("endWeek"));
                row.put("weekType", first.get("weekType"));
            }
        }
        return rows;
    }

    private void validateRatio(Double usualRatio, Double examRatio) {
        if (usualRatio == null && examRatio == null) {
            return;
        }
        double usual = usualRatio == null ? 0.4 : usualRatio;
        double exam = examRatio == null ? 0.6 : examRatio;
        if (usual < 0 || exam < 0 || Math.abs(usual + exam - 1.0) > 0.001) {
            throw new ApiException(400, "平时分和考试分比例之和必须为 1");
        }
    }

    private void validateOfferingTimes(List<CreateOfferingRequest.OfferingTimeRequest> times) {
        if (times == null || times.isEmpty()) {
            throw new ApiException(400, "至少需要一个上课时间段");
        }
        for (int i = 0; i < times.size(); i += 1) {
            CreateOfferingRequest.OfferingTimeRequest time = times.get(i);
            if (time.startSection() > time.endSection()) {
                throw new ApiException(400, "开始节次不能大于结束节次");
            }
            if (time.startWeek() > time.endWeek()) {
                throw new ApiException(400, "起始周不能大于结束周");
            }
            for (int j = i + 1; j < times.size(); j += 1) {
                if (timeConflict(time, times.get(j))) {
                    throw new ApiException(400, "同一课程班内的上课时间段不能互相冲突");
                }
            }
        }
    }

    private boolean timeConflict(CreateOfferingRequest.OfferingTimeRequest left,
                                 CreateOfferingRequest.OfferingTimeRequest right) {
        if (!left.dayOfWeek().equals(right.dayOfWeek())) {
            return false;
        }
        boolean sectionOverlap = !(left.endSection() < right.startSection()
                || left.startSection() > right.endSection());
        boolean weekOverlap = !(left.endWeek() < right.startWeek()
                || left.startWeek() > right.endWeek());
        boolean weekTypeOverlap = "all".equals(left.weekType())
                || "all".equals(right.weekType())
                || left.weekType().equals(right.weekType());
        return sectionOverlap && weekOverlap && weekTypeOverlap;
    }

    private void validateCourseForNewOffering(Long courseId) {
        String status = adminMapper.courseStatus(courseId);
        if (status == null) {
            throw new ApiException(404, "课程不存在");
        }
        if (!"enabled".equals(status)) {
            throw new ApiException(400, "弃用课程不能新建课程班");
        }
    }
}
