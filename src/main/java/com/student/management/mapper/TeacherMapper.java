package com.student.management.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TeacherMapper {
    @Select("""
            SELECT co.id, co.capacity, co.selected_count AS selectedCount,
                   co.status, co.exam_ratio AS examRatio,
                   c.code AS courseCode, c.name AS courseName, c.credit,
                   s.id AS semesterId, s.name AS semesterName,
                   grading_phase.semester_id IS NOT NULL AS gradingOpen,
                   CASE
                     WHEN CURDATE() < s.start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN s.start_date AND s.end_date THEN 'active'
                     ELSE 'archived'
                   END AS semesterStatus,
                   u.display_name AS teacherName
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
              LEFT JOIN semester_active_phases grading_phase
                ON grading_phase.semester_id = s.id AND grading_phase.phase = 'grading'
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
             WHERE co.teacher_id = #{teacherId}
             ORDER BY s.start_date DESC, co.id
            """)
    List<Map<String, Object>> courses(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT co.id, co.capacity, co.selected_count AS selectedCount,
                   co.status, co.exam_ratio AS examRatio,
                   c.code AS courseCode, c.name AS courseName, c.credit,
                   s.id AS semesterId, s.name AS semesterName,
                   grading_phase.semester_id IS NOT NULL AS gradingOpen,
                   CASE
                     WHEN CURDATE() < s.start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN s.start_date AND s.end_date THEN 'active'
                     ELSE 'archived'
                   END AS semesterStatus,
                   u.display_name AS teacherName
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
              LEFT JOIN semester_active_phases grading_phase
                ON grading_phase.semester_id = s.id AND grading_phase.phase = 'grading'
              JOIN teachers t ON t.id = co.teacher_id
              JOIN users u ON u.id = t.user_id
             WHERE co.teacher_id = #{teacherId}
               AND s.id = (
            """ + CommonMapper.CURRENT_SEMESTER_ID_SQL + """
                   )
             ORDER BY co.id
            """)
    List<Map<String, Object>> schedule(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT co.id, co.capacity, co.selected_count AS selectedCount,
                   co.status, c.code AS courseCode, c.name AS courseName, c.credit,
                   s.id AS semesterId, s.name AS semesterName,
                   TRUE AS gradingOpen,
                   CASE
                     WHEN CURDATE() < s.start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN s.start_date AND s.end_date THEN 'active'
                     ELSE 'archived'
                   END AS semesterStatus,
                   COUNT(e.id) AS studentCount,
                   COUNT(g.final_score) AS gradedCount,
                   COALESCE(SUM(CASE WHEN e.id IS NOT NULL AND g.final_score IS NULL THEN 1 ELSE 0 END), 0) AS missingGradeCount
              FROM course_offering_stats co
              JOIN courses c ON c.id = co.course_id
              JOIN semesters s ON s.id = co.semester_id
              JOIN semester_active_phases grading_phase
                ON grading_phase.semester_id = s.id AND grading_phase.phase = 'grading'
              LEFT JOIN enrollments e ON e.offering_id = co.id AND e.status = 'selected'
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE co.teacher_id = #{teacherId}
             GROUP BY co.id, co.capacity, co.selected_count, co.status, c.code, c.name, c.credit,
                      s.id, s.name, s.start_date, s.end_date
             ORDER BY s.start_date DESC, co.id
            """)
    List<Map<String, Object>> gradeCourses(@Param("teacherId") Long teacherId);

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
              JOIN course_offerings co ON co.id = e.offering_id
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.offering_id = #{offeringId} AND co.teacher_id = #{teacherId} AND e.status = 'selected'
             ORDER BY s.student_no
            """)
    List<Map<String, Object>> roster(@Param("teacherId") Long teacherId, @Param("offeringId") Long offeringId);

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
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN semesters sem ON sem.id = co.semester_id
              JOIN semester_active_phases grading_phase
                ON grading_phase.semester_id = sem.id AND grading_phase.phase = 'grading'
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.offering_id = #{offeringId}
               AND co.teacher_id = #{teacherId}
               AND e.status = 'selected'
             ORDER BY s.student_no
            """)
    List<Map<String, Object>> gradeRoster(@Param("teacherId") Long teacherId, @Param("offeringId") Long offeringId);

    @Select("""
            SELECT co.teacher_id AS teacherId,
                   grading_phase.semester_id IS NOT NULL AS gradingOpen,
                   CASE
                     WHEN CURDATE() < sem.start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN sem.start_date AND sem.end_date THEN 'active'
                     ELSE 'archived'
                   END AS semesterStatus,
                   g.final_score AS finalScore
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              JOIN semesters sem ON sem.id = co.semester_id
              LEFT JOIN semester_active_phases grading_phase
                ON grading_phase.semester_id = sem.id AND grading_phase.phase = 'grading'
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.id = #{enrollmentId}
               AND e.status = 'selected'
            """)
    Map<String, Object> enrollmentOwner(@Param("enrollmentId") Long enrollmentId);

    @Insert("""
            INSERT INTO grades(enrollment_id, usual_score, exam_score, updated_by)
            VALUES(#{enrollmentId}, #{usualScore}, #{examScore}, #{userId})
            ON DUPLICATE KEY UPDATE usual_score = VALUES(usual_score),
                                    exam_score = VALUES(exam_score),
                                    updated_by = VALUES(updated_by)
            """)
    int upsertGrade(@Param("enrollmentId") Long enrollmentId, @Param("usualScore") Double usualScore,
                    @Param("examScore") Double examScore, @Param("userId") Long userId);

    @Select("""
            SELECT COUNT(DISTINCT co.id) AS offeringCount,
                   COUNT(e.id) AS studentCount,
                   ROUND(AVG(g.final_score), 2) AS avgScore
              FROM course_offerings co
              LEFT JOIN enrollments e ON e.offering_id = co.id AND e.status = 'selected'
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE co.teacher_id = #{teacherId}
            """)
    Map<String, Object> dashboard(@Param("teacherId") Long teacherId);

    @Select("""
            SELECT ROUND(AVG(g.final_score), 2) AS avgScore,
                   ROUND(SUM(g.final_score < 60) / NULLIF(COUNT(g.id), 0) * 100, 1) AS failRate,
                   SUM(g.final_score >= 90) AS excellent,
                   SUM(g.final_score >= 80 AND g.final_score < 90) AS good,
                   SUM(g.final_score >= 60 AND g.final_score < 80) AS passed,
                   SUM(g.final_score < 60) AS failed,
                   COUNT(g.id) AS gradedCount
              FROM enrollments e
              JOIN course_offerings co ON co.id = e.offering_id
              LEFT JOIN grade_results g ON g.enrollment_id = e.id
             WHERE e.offering_id = #{offeringId}
               AND co.teacher_id = #{teacherId}
               AND e.status = 'selected'
            """)
    Map<String, Object> courseGradeStats(@Param("teacherId") Long teacherId, @Param("offeringId") Long offeringId);

}
