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
    String CURRENT_SEMESTER_ID_SQL = CommonMapper.CURRENT_SEMESTER_ID_SQL;

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
              FROM grade_results g
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

    @Insert("""
            INSERT INTO users(username, password_hash, display_name, email, role_id)
            VALUES(#{username}, #{passwordHash}, #{displayName}, #{email}, #{roleId})
            """)
    int insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash,
                   @Param("displayName") String displayName, @Param("email") String email,
                   @Param("roleId") Long roleId);

    @Update("UPDATE users SET status = #{status} WHERE id = #{userId}")
    int updateUserStatus(@Param("userId") Long userId, @Param("status") String status);

    @Update("UPDATE users SET display_name = #{displayName} WHERE id = #{userId}")
    int updateUserDisplayName(@Param("userId") Long userId, @Param("displayName") String displayName);

    @Update("UPDATE users SET username = #{username}, display_name = #{displayName} WHERE id = #{userId}")
    int updateUserIdentity(@Param("userId") Long userId, @Param("username") String username,
                           @Param("displayName") String displayName);

    @Update("UPDATE users SET email = #{email} WHERE id = #{userId}")
    int updateUserEmail(@Param("userId") Long userId, @Param("email") String email);

    @Select("""
            WITH current_semester AS (
            """ + CURRENT_SEMESTER_ID_SQL + """
            )
            SELECT s.id, s.name, s.start_date AS startDate, s.end_date AS endDate,
                   s.max_credit AS maxCredit,
                   selection_phase.semester_id IS NOT NULL AS selectionOpen,
                   grading_phase.semester_id IS NOT NULL AS gradingOpen,
                   CASE
                     WHEN s.start_date > CURDATE() THEN 'not_started'
                     WHEN CURDATE() BETWEEN s.start_date AND s.end_date THEN 'active'
                     ELSE 'archived'
                   END AS status,
                   s.id = (SELECT id FROM current_semester) AS isCurrent
              FROM semesters s
              LEFT JOIN semester_active_phases selection_phase
                ON selection_phase.semester_id = s.id AND selection_phase.phase = 'selection'
              LEFT JOIN semester_active_phases grading_phase
                ON grading_phase.semester_id = s.id AND grading_phase.phase = 'grading'
             ORDER BY s.start_date DESC
            """)
    List<Map<String, Object>> semesters();

    @Insert("""
            INSERT INTO semesters(name, start_date, end_date, max_credit)
            VALUES(#{name}, #{startDate}, #{endDate}, #{maxCredit})
            """)
    int insertSemester(SemesterRequest request);

    @Select("SELECT id FROM semesters WHERE name = #{name}")
    Long semesterIdByName(@Param("name") String name);

    @Select("SELECT COUNT(*) FROM semesters WHERE id = #{semesterId}")
    int countSemesterById(@Param("semesterId") Long semesterId);

    @Select("""
            SELECT CASE
                     WHEN start_date > CURDATE() THEN 'not_started'
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 'active'
                     ELSE 'archived'
                   END
              FROM semesters
             WHERE id = #{semesterId}
            """)
    String semesterStatusById(@Param("semesterId") Long semesterId);

    @Select("""
            SELECT COUNT(*)
              FROM semesters
             WHERE (#{semesterId} IS NULL OR id <> #{semesterId})
               AND start_date <= #{endDate}
               AND end_date >= #{startDate}
            """)
    int countOverlappingSemesters(@Param("semesterId") Long semesterId,
                                  @Param("startDate") String startDate,
                                  @Param("endDate") String endDate);

    @Update("""
            UPDATE semesters
               SET name = #{request.name},
                   start_date = #{request.startDate},
                   end_date = #{request.endDate},
                   max_credit = #{request.maxCredit}
             WHERE id = #{semesterId}
            """)
    int updateSemester(@Param("semesterId") Long semesterId, @Param("request") SemesterRequest request);

    @Select("SELECT COUNT(*) FROM semester_active_phases WHERE semester_id = #{semesterId} AND phase = 'selection'")
    int countOpenSelectionBySemester(@Param("semesterId") Long semesterId);

    @Select("SELECT COUNT(*) FROM semester_active_phases WHERE semester_id = #{semesterId} AND phase = 'grading'")
    int countOpenGradingBySemester(@Param("semesterId") Long semesterId);

    @Update("DELETE FROM semester_active_phases WHERE phase = 'selection'")
    int closeAllSelectionSemesters();

    @Update("DELETE FROM semester_active_phases WHERE phase = 'grading'")
    int closeAllGradingSemesters();

    @Update("""
            <script>
            <choose>
              <when test="open">
                INSERT INTO semester_active_phases(phase, semester_id)
                VALUES('selection', #{semesterId})
                ON DUPLICATE KEY UPDATE semester_id = VALUES(semester_id)
              </when>
              <otherwise>
                DELETE FROM semester_active_phases
                 WHERE phase = 'selection' AND semester_id = #{semesterId}
              </otherwise>
            </choose>
            </script>
            """)
    int updateSemesterSelectionOpen(@Param("semesterId") Long semesterId, @Param("open") boolean open);

    @Update("""
            <script>
            <choose>
              <when test="open">
                INSERT INTO semester_active_phases(phase, semester_id)
                VALUES('grading', #{semesterId})
                ON DUPLICATE KEY UPDATE semester_id = VALUES(semester_id)
              </when>
              <otherwise>
                DELETE FROM semester_active_phases
                 WHERE phase = 'grading' AND semester_id = #{semesterId}
              </otherwise>
            </choose>
            </script>
            """)
    int updateSemesterGradingOpen(@Param("semesterId") Long semesterId, @Param("open") boolean open);

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

    @Select("SELECT id, building, room_no AS roomNo FROM classrooms ORDER BY id")
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
            SELECT co.id, co.capacity, co.selected_count AS selectedCount,
                   co.status, co.exam_ratio AS examRatio,
                   c.id AS courseId, c.code AS courseCode, c.name AS courseName, c.credit,
                   d.name AS departmentName,
                   s.id AS semesterId, s.name AS semesterName,
                   u.display_name AS teacherName,
                   t.id AS teacherId, t.teacher_no AS teacherNo
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN departments d ON d.id = c.department_id
              JOIN semesters s ON s.id = co.semester_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
             WHERE 1 = 1
             <if test="keyword != null and keyword != ''">
               AND (c.code LIKE CONCAT('%', #{keyword}, '%')
                    OR c.name LIKE CONCAT('%', #{keyword}, '%')
                    OR t.teacher_no LIKE CONCAT('%', #{keyword}, '%')
                    OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                    OR EXISTS (
                         SELECT 1
                           FROM course_offering_times cot
                           JOIN classrooms cr ON cr.id = cot.classroom_id
                          WHERE cot.offering_id = co.id
                            AND CONCAT(cr.building, cr.room_no) LIKE CONCAT('%', #{keyword}, '%')
                    ))
             </if>
             <choose>
             <when test="semesterId != null">
               AND s.id = #{semesterId}
             </when>
             <when test="currentOnly">
               AND s.id = (
            """ + CURRENT_SEMESTER_ID_SQL + """
               )
             </when>
             </choose>
             ORDER BY s.start_date DESC, co.id
            </script>
            """)
    List<Map<String, Object>> listOfferings(@Param("keyword") String keyword,
                                            @Param("currentOnly") boolean currentOnly,
                                            @Param("semesterId") Long semesterId);

    @Insert("""
            INSERT INTO course_offerings(course_id, semester_id, teacher_id,
                                         capacity, status, exam_ratio)
            VALUES(#{courseId}, #{semesterId}, #{teacherId}, #{capacity}, 'selecting',
                   COALESCE(#{examRatio}, 0.6))
            """)
    int insertOffering(CreateOfferingRequest request);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();

    @Insert("""
            <script>
            INSERT INTO course_offering_times(offering_id, classroom_id, day_of_week, start_section, end_section,
                                              start_week, end_week, week_type)
            VALUES
            <foreach collection="times" item="time" separator=",">
              (#{offeringId}, #{time.classroomId}, #{time.dayOfWeek}, #{time.startSection}, #{time.endSection},
               #{time.startWeek}, #{time.endWeek}, #{time.weekType})
            </foreach>
            </script>
            """)
    int insertOfferingTimes(@Param("offeringId") Long offeringId,
                            @Param("times") List<CreateOfferingRequest.OfferingTimeRequest> times);

    @Select("""
            <script>
            SELECT COUNT(*)
              FROM course_offerings co
              JOIN course_offering_times existing_time ON existing_time.offering_id = co.id
              JOIN (
                <foreach collection="times" item="time" separator=" UNION ALL ">
                  SELECT #{time.dayOfWeek} AS day_of_week,
                         #{time.classroomId} AS classroom_id,
                         #{time.startSection} AS start_section,
                         #{time.endSection} AS end_section,
                         #{time.startWeek} AS start_week,
                         #{time.endWeek} AS end_week,
                         #{time.weekType} AS week_type
                </foreach>
              ) request_time
             WHERE co.semester_id = #{semesterId}
               AND (#{offeringId} IS NULL OR co.id != #{offeringId})
               AND (co.teacher_id = #{teacherId} OR existing_time.classroom_id = request_time.classroom_id)
               AND existing_time.day_of_week = request_time.day_of_week
               AND NOT (existing_time.end_section &lt; request_time.start_section
                        OR existing_time.start_section &gt; request_time.end_section)
               AND NOT (existing_time.end_week &lt; request_time.start_week
                        OR existing_time.start_week &gt; request_time.end_week)
               AND (existing_time.week_type = 'all'
                    OR request_time.week_type = 'all'
                    OR existing_time.week_type = request_time.week_type)
            </script>
            """)
    int countOfferingResourceConflicts(@Param("offeringId") Long offeringId,
                                       @Param("semesterId") Long semesterId,
                                       @Param("teacherId") Long teacherId,
                                       @Param("times") List<CreateOfferingRequest.OfferingTimeRequest> times);

    @Update("""
            UPDATE course_offerings
               SET course_id = #{request.courseId},
                   semester_id = #{request.semesterId},
                   teacher_id = #{request.teacherId},
                   capacity = #{request.capacity},
                   status = #{request.status},
                   exam_ratio = COALESCE(#{request.examRatio}, 0.6)
             WHERE id = #{offeringId}
            """)
    int updateOffering(@Param("offeringId") Long offeringId, @Param("request") CreateOfferingRequest request);

    @org.apache.ibatis.annotations.Delete("DELETE FROM course_offering_times WHERE offering_id = #{offeringId}")
    int deleteOfferingTimes(@Param("offeringId") Long offeringId);

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
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
              JOIN semesters s ON s.id = co.semester_id
             WHERE s.id = (
            """ + CURRENT_SEMESTER_ID_SQL + """
                   )
               AND co.status = 'selecting'
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
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
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
               SET status = 'dropped'
             WHERE student_id = #{studentId} AND offering_id = #{offeringId} AND status = 'selected'
            """)
    int adminDropCourse(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    @Update("""
            UPDATE enrollments
               SET status = 'dropped'
             WHERE id = #{enrollmentId} AND status = 'selected'
            """)
    int adminDropEnrollment(@Param("enrollmentId") Long enrollmentId);

    @Select("""
            SELECT e.id AS enrollmentId, co.id AS offeringId, c.code AS courseCode, c.name AS courseName,
                   c.credit, sem.name AS semesterName, u.display_name AS teacherName, e.status
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN semesters sem ON sem.id = co.semester_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
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
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
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
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
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
            <script>
            SELECT co.id, c.code AS courseCode, c.name AS courseName,
                   co.capacity, co.selected_count AS selectedCount,
                   co.exam_ratio AS examRatio,
                   s.id AS semesterId, s.name AS semesterName
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
             WHERE co.teacher_id = #{teacherId}
             <choose>
             <when test="semesterId != null">
               AND s.id = #{semesterId}
             </when>
             <otherwise>
               AND s.id = (
            """ + CURRENT_SEMESTER_ID_SQL + """
                   )
             </otherwise>
             </choose>
             ORDER BY co.id
            </script>
            """)
    List<Map<String, Object>> teacherOfferings(@Param("teacherId") Long teacherId,
                                               @Param("semesterId") Long semesterId);

    @Select("""
            <script>
            SELECT co.id AS offeringId, c.code AS courseCode, c.name AS courseName,
                   co.capacity, co.selected_count AS selectedCount,
                   co.exam_ratio AS examRatio,
                   u.display_name AS teacherName,
                   s.id AS semesterId, s.name AS semesterName
              FROM enrollments e
              JOIN course_offering_stats co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
             WHERE e.student_id = #{studentId}
               AND e.status = 'selected'
             <choose>
             <when test="semesterId != null">
               AND s.id = #{semesterId}
             </when>
             <otherwise>
               AND s.id = (
            """ + CURRENT_SEMESTER_ID_SQL + """
                   )
             </otherwise>
             </choose>
             ORDER BY co.id
            </script>
            """)
    List<Map<String, Object>> studentEnrollmentsBySemester(@Param("studentId") Long studentId,
                                                           @Param("semesterId") Long semesterId);
}
