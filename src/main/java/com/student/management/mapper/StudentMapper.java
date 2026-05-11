package com.student.management.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.StatementType;

@Mapper
public interface StudentMapper {
    String CURRENT_SEMESTER_ID_SQL = CommonMapper.CURRENT_SEMESTER_ID_SQL;

    @Select("""
            <script>
            SELECT co.id, c.code AS courseCode, c.name AS courseName, c.credit,
                   u.display_name AS teacherName,
                   co.capacity, co.selected_count AS selectedCount, co.status,
                   e.id AS enrollmentId, e.status AS enrollmentStatus,
                   EXISTS (
                       SELECT 1
                         FROM enrollments pe
                         JOIN course_offerings pco ON pco.id = pe.offering_id
                         JOIN grade_results pg ON pg.enrollment_id = pe.id
                        WHERE pe.student_id = #{studentId}
                          AND pe.status = 'selected'
                          AND pco.course_id = co.course_id
                          AND pco.semester_id != co.semester_id
                          AND pg.final_score >= 60
                   ) AS passedBefore
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
              LEFT JOIN enrollments e ON e.offering_id = co.id AND e.student_id = #{studentId}
             WHERE co.semester_id = #{semesterId}
               AND co.status = 'selecting'
             <if test="keyword != null and keyword != ''">
               AND (c.code LIKE CONCAT('%', #{keyword}, '%')
                   OR c.name LIKE CONCAT('%', #{keyword}, '%')
                   OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                   OR EXISTS (
                        SELECT 1
                          FROM course_offering_times cot
                          JOIN classrooms cr ON cr.id = cot.classroom_id
                         WHERE cot.offering_id = co.id
                           AND CONCAT(cr.building, cr.room_no) LIKE CONCAT('%', #{keyword}, '%')
                   ))
             </if>
             ORDER BY co.id
            </script>
            """)
    List<Map<String, Object>> listCurrentOfferings(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId,
                                                    @Param("keyword") String keyword);

    @Select("{ CALL sp_select_course(#{studentId, mode=IN, jdbcType=BIGINT}, #{offeringId, mode=IN, jdbcType=BIGINT}) }")
    @Options(statementType = StatementType.CALLABLE)
    void callSelectCourse(@Param("studentId") Long studentId, @Param("offeringId") Long offeringId);

    @Update("""
            UPDATE enrollments
               SET status = 'dropped'
             WHERE id = #{enrollmentId} AND student_id = #{studentId} AND status = 'selected'
               AND EXISTS (
                    SELECT 1
                      FROM course_offerings co
                      JOIN semester_active_phases sap
                        ON sap.semester_id = co.semester_id
                       AND sap.phase = 'selection'
                     WHERE co.id = enrollments.offering_id
               )
            """)
    int dropCourse(@Param("studentId") Long studentId, @Param("enrollmentId") Long enrollmentId);

    @Select("""
            SELECT COUNT(*) > 0
              FROM grade_results
             WHERE enrollment_id = #{enrollmentId}
               AND final_score IS NOT NULL
            """)
    boolean isEnrollmentGraded(@Param("enrollmentId") Long enrollmentId);

    @Select("""
            <script>
            SELECT e.id AS enrollmentId, c.code AS courseCode, c.name AS courseName, c.credit,
                   co.id AS offeringId, u.display_name AS teacherName,
                   s.id AS semesterId, s.name AS semesterName
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
              JOIN semesters s ON s.id = co.semester_id
             WHERE e.student_id = #{studentId} AND e.status = 'selected'
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
    List<Map<String, Object>> schedule(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    @Select("""
            SELECT c.code AS courseCode, c.name AS courseName, c.credit,
                   s.name AS semesterName, g.usual_score AS usualScore,
                   g.exam_score AS examScore, g.final_score AS finalScore,
                   g.grade_point AS gradePoint
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.student_id = #{studentId}
               AND e.status = 'selected'
               AND g.final_score IS NOT NULL
             ORDER BY s.start_date DESC, c.code
            """)
    List<Map<String, Object>> grades(@Param("studentId") Long studentId);

    @Select("""
            SELECT c.code AS courseCode, c.name AS courseName, c.credit,
                   s.id AS semesterId, s.name AS semesterName, g.usual_score AS usualScore,
                   g.exam_score AS examScore, g.final_score AS finalScore,
                   g.grade_point AS gradePoint
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.student_id = #{studentId}
               AND e.status = 'selected'
               AND (#{semesterId} IS NULL OR s.id = #{semesterId})
               AND g.final_score IS NOT NULL
             ORDER BY s.start_date DESC, c.code
            """)
    List<Map<String, Object>> transcript(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);

    @Select("""
            SELECT COALESCE(SUM(CASE WHEN co.semester_id = current_semester.id THEN c.credit ELSE 0 END), 0) AS selectedCredits,
                   COALESCE(SUM(CASE WHEN co.semester_id = current_semester.id THEN 1 ELSE 0 END), 0) AS selectedCourses,
                   ROUND(SUM(c.credit * g.grade_point) / NULLIF(SUM(CASE WHEN g.grade_point IS NOT NULL THEN c.credit ELSE 0 END), 0), 2) AS gpa
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN courses c ON c.id = co.course_id
              LEFT JOIN (
            """ + CURRENT_SEMESTER_ID_SQL + """
              ) current_semester ON 1 = 1
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.student_id = #{studentId} AND e.status = 'selected'
            """)
    Map<String, Object> dashboard(@Param("studentId") Long studentId);
}
