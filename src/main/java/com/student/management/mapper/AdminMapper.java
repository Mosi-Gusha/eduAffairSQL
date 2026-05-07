package com.student.management.mapper;

import java.util.List;
import java.util.Map;

import com.student.management.dto.CourseRequest;
import com.student.management.dto.CreateOfferingRequest;
import com.student.management.dto.NoticeRequest;
import com.student.management.dto.SemesterRequest;
import com.student.management.dto.StudentProfileRequest;
import com.student.management.dto.TeacherRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.StatementType;

@Mapper
public interface AdminMapper {
    @Select("SELECT COUNT(*) FROM users")
    long countUsers();

    @Select("SELECT COUNT(*) FROM courses WHERE status = 'enabled'")
    long countEnabledCourses();

    @Select("SELECT COUNT(*) FROM course_offerings")
    long countOfferings();

    @Select("SELECT COUNT(*) FROM enrollments WHERE status = 'selected'")
    long countSelectedEnrollments();

    @Select("""
            SELECT SUM(g.final_score >= 90) AS excellent,
                   SUM(g.final_score >= 80 AND g.final_score < 90) AS good,
                   SUM(g.final_score >= 60 AND g.final_score < 80) AS passed,
                   SUM(g.final_score < 60) AS failed
              FROM grades g
             WHERE g.final_score IS NOT NULL
            """)
    Map<String, Object> gradeDistribution();

    @Select("""
            SELECT u.id, u.username, u.display_name AS displayName, u.status,
                   r.code AS role, r.name AS roleName, u.created_at AS createdAt
              FROM users u
              JOIN roles r ON r.id = u.role_id
             ORDER BY u.id
            """)
    List<Map<String, Object>> listUsers();

    @Select("SELECT id FROM roles WHERE code = #{code}")
    Long roleIdByCode(@Param("code") String code);

    @Select("SELECT r.code FROM users u JOIN roles r ON r.id = u.role_id WHERE u.id = #{userId}")
    String roleCodeByUserId(@Param("userId") Long userId);

    @Select("SELECT id FROM users WHERE username = #{username}")
    Long userIdByUsername(@Param("username") String username);

    @Select("SELECT username FROM users WHERE id = #{userId}")
    String usernameById(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO users(username, password_hash, display_name, email, role_id)
            VALUES(#{username}, #{passwordHash}, #{displayName}, #{email}, #{roleId})
            """)
    int insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash,
                   @Param("displayName") String displayName, @Param("email") String email,
                   @Param("roleId") Long roleId);

    @Update("UPDATE users SET status = 'disabled' WHERE id = #{userId}")
    int disableUser(@Param("userId") Long userId);

    @Update("UPDATE users SET display_name = #{displayName} WHERE id = #{userId}")
    int updateUserDisplayName(@Param("userId") Long userId, @Param("displayName") String displayName);

    @Update("UPDATE users SET username = #{username}, display_name = #{displayName} WHERE id = #{userId}")
    int updateUserIdentity(@Param("userId") Long userId, @Param("username") String username,
                           @Param("displayName") String displayName);

    @Update("UPDATE users SET email = #{email} WHERE id = #{userId}")
    int updateUserEmail(@Param("userId") Long userId, @Param("email") String email);

    @Update("UPDATE users SET password_hash = SHA2(username, 256), status = 'enabled' WHERE id = #{userId}")
    int resetPassword(@Param("userId") Long userId);

    @Select("""
            SELECT id, name, start_date AS startDate, end_date AS endDate,
                   max_credit AS maxCredit,
                   CASE
                     WHEN CURDATE() < start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 'active'
                     ELSE 'archived'
                   END AS status,
                   is_current AS isCurrent
              FROM semesters
             ORDER BY start_date DESC
            """)
    List<Map<String, Object>> semesters();

    @Select("SELECT COUNT(*) FROM semesters WHERE is_current = 1 AND CURDATE() <= end_date")
    int activeSemesterCount();

    @Select("""
            SELECT id, name, start_date AS startDate, end_date AS endDate,
                   max_credit AS maxCredit,
                   CASE
                     WHEN CURDATE() < start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 'active'
                     ELSE 'archived'
                   END AS status,
                   is_current AS isCurrent
              FROM semesters
             WHERE id = #{semesterId}
            """)
    Map<String, Object> semesterById(@Param("semesterId") Long semesterId);

    @Insert("""
            INSERT INTO semesters(name, start_date, end_date, max_credit, is_current)
            VALUES(#{name}, #{startDate}, #{endDate}, #{maxCredit}, 1)
            """)
    int insertSemester(SemesterRequest request);

    @Update("""
            UPDATE semesters
               SET name = #{request.name},
                   start_date = #{request.startDate},
                   end_date = #{request.endDate},
                   max_credit = #{request.maxCredit}
             WHERE id = #{semesterId}
            """)
    int updateSemester(@Param("semesterId") Long semesterId, @Param("request") SemesterRequest request);

    @Update("UPDATE semesters SET is_current = 0")
    int clearCurrentSemester();

    @Update("UPDATE semesters SET is_current = 1 WHERE id = #{semesterId}")
    int setCurrentSemester(@Param("semesterId") Long semesterId);

    @Select("SELECT id, name, phone FROM departments ORDER BY id")
    List<Map<String, Object>> departments();

    @Select("""
            SELECT m.id, m.name, m.department_id AS departmentId,
                   m.duration_years AS durationYears, d.name AS departmentName
              FROM majors m
              JOIN departments d ON d.id = m.department_id
             ORDER BY d.id, m.id
            """)
    List<Map<String, Object>> majors();

    @Select("""
            SELECT t.id, t.teacher_no AS teacherNo, u.display_name AS name, u.email,
                   d.name AS departmentName, t.title
              FROM teachers t
              JOIN users u ON u.id = t.user_id
              JOIN departments d ON d.id = t.department_id
             ORDER BY t.id
            """)
    List<Map<String, Object>> teachers();

    @Select("""
            SELECT c.id, c.code, c.name, c.credit, c.status,
                   d.id AS departmentId, d.name AS departmentName
              FROM courses c
              JOIN departments d ON d.id = c.department_id
             WHERE c.status = 'enabled'
             ORDER BY c.code
            """)
    List<Map<String, Object>> courses();

    @Select("""
            <script>
            SELECT c.id, c.code, c.name, c.credit, c.status,
                   d.id AS departmentId, d.name AS departmentName
              FROM courses c
              JOIN departments d ON d.id = c.department_id
             WHERE 1 = 1
             <if test="keyword != null and keyword != ''">
               AND (c.code LIKE CONCAT('%', #{keyword}, '%')
                    OR c.name LIKE CONCAT('%', #{keyword}, '%')
                    OR d.name LIKE CONCAT('%', #{keyword}, '%'))
             </if>
             ORDER BY c.code
            </script>
            """)
    List<Map<String, Object>> listCourses(@Param("keyword") String keyword);

    @Select("SELECT status FROM courses WHERE id = #{courseId}")
    String courseStatus(@Param("courseId") Long courseId);

    @Insert("""
            INSERT INTO courses(code, name, department_id, credit, status)
            VALUES(#{code}, #{name}, #{departmentId}, #{credit}, 'enabled')
            """)
    int insertCourse(CourseRequest request);

    @Update("UPDATE courses SET status = #{status} WHERE id = #{courseId}")
    int updateCourseStatus(@Param("courseId") Long courseId, @Param("status") String status);

    @Select("SELECT id, building, room_no AS roomNo, capacity FROM classrooms ORDER BY id")
    List<Map<String, Object>> classrooms();

    @Select("""
            <script>
            SELECT t.id AS teacherId, t.teacher_no AS teacherNo, u.id AS userId, u.username,
                   u.display_name AS name, u.email, u.status, t.title,
                   d.id AS departmentId, d.name AS departmentName,
                   GROUP_CONCAT(DISTINCT c.name ORDER BY c.name SEPARATOR '、') AS courseNames
              FROM teachers t
              JOIN users u ON u.id = t.user_id
              JOIN departments d ON d.id = t.department_id
              LEFT JOIN course_offerings co ON co.teacher_id = t.id
              LEFT JOIN courses c ON c.id = co.course_id
             WHERE 1 = 1
             <if test="keyword != null and keyword != ''">
               AND (t.teacher_no LIKE CONCAT('%', #{keyword}, '%')
                    OR u.username LIKE CONCAT('%', #{keyword}, '%')
                    OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                    OR u.email LIKE CONCAT('%', #{keyword}, '%')
                    OR d.name LIKE CONCAT('%', #{keyword}, '%')
                    OR c.name LIKE CONCAT('%', #{keyword}, '%'))
             </if>
             GROUP BY t.id, t.teacher_no, u.id, u.username, u.display_name, u.email, u.status, t.title, d.id, d.name
             ORDER BY t.teacher_no
            </script>
            """)
    List<Map<String, Object>> listTeachers(@Param("keyword") String keyword);

    @Insert("""
            INSERT INTO teachers(user_id, teacher_no, department_id, title)
            VALUES(#{userId}, #{request.teacherNo}, #{request.departmentId}, #{request.title})
            """)
    int insertTeacher(@Param("userId") Long userId, @Param("request") TeacherRequest request);

    @Update("""
            UPDATE teachers
               SET teacher_no = #{request.teacherNo},
                   department_id = #{request.departmentId},
                   title = #{request.title}
             WHERE id = #{teacherId}
            """)
    int updateTeacher(@Param("teacherId") Long teacherId, @Param("request") TeacherRequest request);

    @Select("SELECT user_id FROM teachers WHERE id = #{teacherId}")
    Long teacherUserId(@Param("teacherId") Long teacherId);

    @Select("""
            <script>
            SELECT s.id AS studentId, s.student_no AS studentNo, u.id AS userId, u.username,
                   u.display_name AS name, u.email, u.status, s.admission_year AS admissionYear,
                   m.id AS majorId, m.name AS majorName, d.name AS departmentName,
                   GROUP_CONCAT(DISTINCT c.name ORDER BY c.name SEPARATOR '、') AS courseNames
              FROM students s
              JOIN users u ON u.id = s.user_id
              JOIN majors m ON m.id = s.major_id
              JOIN departments d ON d.id = m.department_id
              LEFT JOIN enrollments e ON e.student_id = s.id AND e.status = 'selected'
              LEFT JOIN course_offerings co ON co.id = e.offering_id
              LEFT JOIN courses c ON c.id = co.course_id
             WHERE 1 = 1
             <if test="keyword != null and keyword != ''">
               AND (s.student_no LIKE CONCAT('%', #{keyword}, '%')
                    OR u.username LIKE CONCAT('%', #{keyword}, '%')
                    OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                    OR u.email LIKE CONCAT('%', #{keyword}, '%')
                    OR m.name LIKE CONCAT('%', #{keyword}, '%')
                    OR d.name LIKE CONCAT('%', #{keyword}, '%')
                    OR c.name LIKE CONCAT('%', #{keyword}, '%'))
             </if>
             GROUP BY s.id, s.student_no, u.id, u.username, u.display_name, u.email, u.status,
                      s.admission_year, m.id, m.name, d.name
             ORDER BY s.student_no
            </script>
            """)
    List<Map<String, Object>> listStudents(@Param("keyword") String keyword);

    @Insert("""
            INSERT INTO students(user_id, student_no, major_id, admission_year)
            VALUES(#{userId}, #{request.studentNo}, #{request.majorId}, #{request.admissionYear})
            """)
    int insertStudent(@Param("userId") Long userId, @Param("request") StudentProfileRequest request);

    @Update("""
            UPDATE students
               SET student_no = #{request.studentNo},
                   major_id = #{request.majorId},
                   admission_year = #{request.admissionYear}
             WHERE id = #{studentId}
            """)
    int updateStudent(@Param("studentId") Long studentId, @Param("request") StudentProfileRequest request);

    @Select("SELECT user_id FROM students WHERE id = #{studentId}")
    Long studentUserId(@Param("studentId") Long studentId);

    @Select("""
            <script>
            SELECT co.id, co.day_of_week AS dayOfWeek, co.start_section AS startSection,
                   co.end_section AS endSection, co.week_type AS weekType, co.capacity, co.selected_count AS selectedCount,
                   co.status, co.usual_ratio AS usualRatio, co.exam_ratio AS examRatio,
                   c.id AS courseId, c.code AS courseCode, c.name AS courseName, c.credit,
                   d.name AS departmentName,
                   s.id AS semesterId, s.name AS semesterName,
                   u.display_name AS teacherName,
                   t.id AS teacherId, t.teacher_no AS teacherNo,
                   cr.id AS classroomId, CONCAT(cr.building, cr.room_no) AS classroom
              FROM course_offerings co
              JOIN courses c ON c.id = co.course_id
              JOIN departments d ON d.id = c.department_id
              JOIN semesters s ON s.id = co.semester_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
              JOIN classrooms cr ON cr.id = co.classroom_id
             WHERE co.status = 'selecting'
             <if test="keyword != null and keyword != ''">
               AND (c.code LIKE CONCAT('%', #{keyword}, '%')
                    OR c.name LIKE CONCAT('%', #{keyword}, '%')
                    OR t.teacher_no LIKE CONCAT('%', #{keyword}, '%')
                    OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                    OR CONCAT(cr.building, cr.room_no) LIKE CONCAT('%', #{keyword}, '%'))
             </if>
             <if test="currentOnly">
               AND s.is_current = 1
             </if>
             ORDER BY s.start_date DESC, co.day_of_week, co.start_section
            </script>
            """)
    List<Map<String, Object>> listOfferings(@Param("keyword") String keyword, @Param("currentOnly") boolean currentOnly);

    @Insert("""
            INSERT INTO course_offerings(course_id, semester_id, teacher_id, classroom_id, day_of_week,
                                         start_section, end_section, week_type, capacity, status, usual_ratio, exam_ratio)
            VALUES(#{courseId}, #{semesterId}, #{teacherId}, #{classroomId}, #{dayOfWeek},
                   #{startSection}, #{endSection}, #{weekType}, #{capacity}, 'selecting',
                   COALESCE(#{usualRatio}, 0.4), COALESCE(#{examRatio}, 0.6))
            """)
    int insertOffering(CreateOfferingRequest request);

    @Update("""
            UPDATE course_offerings
               SET course_id = #{request.courseId},
                   semester_id = #{request.semesterId},
                   teacher_id = #{request.teacherId},
                   classroom_id = #{request.classroomId},
                   day_of_week = #{request.dayOfWeek},
                   start_section = #{request.startSection},
                   end_section = #{request.endSection},
                   week_type = #{request.weekType},
                   capacity = #{request.capacity},
                   status = #{request.status},
                   usual_ratio = COALESCE(#{request.usualRatio}, 0.4),
                   exam_ratio = COALESCE(#{request.examRatio}, 0.6)
             WHERE id = #{offeringId}
            """)
    int updateOffering(@Param("offeringId") Long offeringId, @Param("request") CreateOfferingRequest request);

    @Update("UPDATE course_offerings SET status = 'closed' WHERE id = #{offeringId}")
    int closeOffering(@Param("offeringId") Long offeringId);

    @org.apache.ibatis.annotations.Delete("""
            DELETE FROM grades WHERE enrollment_id IN (
                SELECT id FROM enrollments WHERE offering_id = #{offeringId}
            )
            """)
    int deleteGradesByOffering(@Param("offeringId") Long offeringId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM enrollments WHERE offering_id = #{offeringId}")
    int deleteEnrollmentsByOffering(@Param("offeringId") Long offeringId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM course_offerings WHERE id = #{offeringId}")
    int deleteOffering(@Param("offeringId") Long offeringId);

    @Select("""
            SELECT co.id AS offeringId, c.code AS courseCode, c.name AS courseName,
                   u.display_name AS teacherName, co.capacity, co.selected_count AS selectedCount,
                   ROUND(co.selected_count / NULLIF(co.capacity, 0) * 100, 1) AS fillRate
              FROM course_offerings co
              JOIN courses c ON c.id = co.course_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
              JOIN semesters s ON s.id = co.semester_id
             WHERE s.is_current = 1 AND co.status = 'selecting'
             ORDER BY fillRate DESC, co.id
            """)
    List<Map<String, Object>> enrollmentReport();

    @Select("""
            SELECT e.id AS enrollmentId, s.student_no AS studentNo, u.display_name AS studentName,
                   u.email, m.name AS majorName, d.name AS departmentName,
                   g.usual_score AS usualScore, g.exam_score AS examScore,
                   g.final_score AS finalScore, g.grade_point AS gradePoint
              FROM enrollments e
              JOIN students s ON s.id = e.student_id
              JOIN users u ON u.id = s.user_id
              JOIN majors m ON m.id = s.major_id
              JOIN departments d ON d.id = m.department_id
              LEFT JOIN grades g ON g.enrollment_id = e.id
             WHERE e.offering_id = #{offeringId} AND e.status = 'selected'
             ORDER BY s.student_no
            """)
    List<Map<String, Object>> courseRoster(@Param("offeringId") Long offeringId);

    @Select("""
            SELECT s.id
              FROM students s
              JOIN users u ON u.id = s.user_id
             WHERE s.student_no = #{keyword} OR u.username = #{keyword}
             LIMIT 1
            """)
    Long studentIdByNoOrUsername(@Param("keyword") String keyword);

    @Select("{ CALL sp_select_course(#{studentId, mode=IN, jdbcType=BIGINT}, #{offeringId, mode=IN, jdbcType=BIGINT}) }")
    @Options(statementType = StatementType.CALLABLE)
    void adminSelectCourse(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    @Update("""
            UPDATE enrollments
               SET status = 'dropped', dropped_at = CURRENT_TIMESTAMP
             WHERE student_id = #{studentId} AND offering_id = #{offeringId} AND status = 'selected'
            """)
    int adminDropCourse(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    @Update("""
            UPDATE enrollments
               SET status = 'dropped', dropped_at = CURRENT_TIMESTAMP
             WHERE id = #{enrollmentId} AND status = 'selected'
            """)
    int adminDropEnrollment(@Param("enrollmentId") Long enrollmentId);

    @Select("""
            SELECT e.id AS enrollmentId, co.id AS offeringId, c.code AS courseCode, c.name AS courseName,
                   c.credit, sem.name AS semesterName, u.display_name AS teacherName,
                   co.day_of_week AS dayOfWeek, co.start_section AS startSection, co.end_section AS endSection,
                   CONCAT(cr.building, cr.room_no) AS classroom, e.status
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN semesters sem ON sem.id = co.semester_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
              JOIN classrooms cr ON cr.id = co.classroom_id
             WHERE e.student_id = #{studentId}
             ORDER BY sem.start_date DESC, c.code
            """)
    List<Map<String, Object>> studentEnrollments(@Param("studentId") Long studentId);

    @Select("""
            SELECT c.code AS courseCode, c.name AS courseName, c.credit,
                   sem.id AS semesterId, sem.name AS semesterName,
                   g.usual_score AS usualScore, g.exam_score AS examScore,
                   g.final_score AS finalScore, g.grade_point AS gradePoint
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN semesters sem ON sem.id = co.semester_id
              LEFT JOIN grades g ON g.enrollment_id = e.id
             WHERE e.student_id = #{studentId} AND e.status = 'selected'
               AND g.final_score IS NOT NULL
             ORDER BY sem.start_date DESC, c.code
            """)
    List<Map<String, Object>> studentTranscript(@Param("studentId") Long studentId);

    @Select("""
            SELECT ROUND(AVG(g.final_score), 2) AS avgScore,
                   ROUND(SUM(g.final_score < 60) / NULLIF(COUNT(g.id), 0) * 100, 1) AS failRate,
                   SUM(g.final_score >= 90) AS excellent,
                   SUM(g.final_score >= 80 AND g.final_score < 90) AS good,
                   SUM(g.final_score >= 60 AND g.final_score < 80) AS passed,
                   SUM(g.final_score < 60) AS failed,
                   COUNT(g.id) AS gradedCount
              FROM enrollments e
              LEFT JOIN grades g ON g.enrollment_id = e.id
             WHERE e.offering_id = #{offeringId} AND e.status = 'selected'
            """)
    Map<String, Object> courseGradeStats(@Param("offeringId") Long offeringId);

    @Insert("""
            INSERT INTO notices(title, content, audience, created_by)
            VALUES(#{request.title}, #{request.content}, #{request.audience}, #{userId})
            """)
    int insertNotice(@Param("request") NoticeRequest request, @Param("userId") Long userId);

    @Update("""
            UPDATE notices
               SET title = #{request.title},
                   content = #{request.content},
                   audience = #{request.audience}
             WHERE id = #{noticeId}
            """)
    int updateNotice(@Param("noticeId") Long noticeId, @Param("request") NoticeRequest request);

    @org.apache.ibatis.annotations.Delete("DELETE FROM notices WHERE id = #{noticeId}")
    int deleteNotice(@Param("noticeId") Long noticeId);

    @Select("""
            SELECT co.id, c.code AS courseCode, c.name AS courseName,
                   co.day_of_week AS dayOfWeek, co.start_section AS startSection,
                   co.end_section AS endSection, co.week_type AS weekType,
                   CONCAT(cr.building, cr.room_no) AS classroom,
                   co.capacity, co.selected_count AS selectedCount,
                   co.usual_ratio AS usualRatio, co.exam_ratio AS examRatio
              FROM course_offerings co
              JOIN courses c ON c.id = co.course_id
              JOIN classrooms cr ON cr.id = co.classroom_id
              JOIN semesters s ON s.id = co.semester_id
             WHERE co.teacher_id = #{teacherId} AND s.is_current = 1 AND co.status = 'selecting'
             ORDER BY co.day_of_week, co.start_section
            """)
    List<Map<String, Object>> teacherCurrentOfferings(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT c.code AS courseCode, c.name AS courseName,
                   co.day_of_week AS dayOfWeek, co.start_section AS startSection,
                   co.end_section AS endSection, co.week_type AS weekType,
                   CONCAT(cr.building, cr.room_no) AS classroom,
                   co.capacity, co.selected_count AS selectedCount,
                   co.usual_ratio AS usualRatio, co.exam_ratio AS examRatio,
                   u.display_name AS teacherName
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN classrooms cr ON cr.id = co.classroom_id
              JOIN semesters s ON s.id = co.semester_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
             WHERE e.student_id = #{studentId} AND e.status = 'selected' AND s.is_current = 1
             ORDER BY co.day_of_week, co.start_section
            """)
    List<Map<String, Object>> studentCurrentEnrollments(@Param("studentId") Long studentId);
}
