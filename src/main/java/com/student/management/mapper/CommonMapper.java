package com.student.management.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommonMapper {
    String CURRENT_SEMESTER_ID_SQL = """
            SELECT id
              FROM semesters
             ORDER BY
                   CASE
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 0
                     WHEN start_date > CURDATE() THEN 1
                     ELSE 2
                   END,
                   CASE WHEN start_date > CURDATE() THEN start_date END ASC,
                   CASE WHEN CURDATE() BETWEEN start_date AND end_date THEN start_date END DESC,
                   CASE WHEN CURDATE() > end_date THEN end_date END DESC,
                   id DESC
             LIMIT 1
            """;

    @Select("""
            SELECT id, title, content, audience, created_at AS createdAt
              FROM notices
             WHERE #{role} = 'admin' OR audience IN ('all', #{role})
             ORDER BY created_at DESC
             LIMIT #{limit}
            """)
    List<Map<String, Object>> listRecentNotices(@Param("role") String role, @Param("limit") int limit);

    @Select("""
            SELECT id, title, content, audience, created_at AS createdAt
              FROM notices
             WHERE #{role} = 'admin' OR audience IN ('all', #{role})
             ORDER BY created_at DESC
            """)
    List<Map<String, Object>> listNotices(@Param("role") String role);

    @Select("""
            WITH current_semester AS (
            """ + CURRENT_SEMESTER_ID_SQL + """
            )
            SELECT s.id, s.name, s.start_date AS startDate, s.end_date AS endDate,
                   s.max_credit AS maxCredit,
                   CASE
                     WHEN s.start_date > CURDATE() THEN 'not_started'
                     WHEN CURDATE() BETWEEN s.start_date AND s.end_date THEN 'active'
                     ELSE 'archived'
                   END AS status,
                   s.id = (SELECT id FROM current_semester) AS isCurrent
              FROM semesters s
             ORDER BY s.start_date DESC
            """)
    List<Map<String, Object>> semesters();

    @Select("""
            SELECT id, name, start_date AS startDate, end_date AS endDate,
                   max_credit AS maxCredit,
                   CASE
                     WHEN start_date > CURDATE() THEN 'not_started'
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 'active'
                     ELSE 'archived'
                   END AS status,
                   1 AS isCurrent
              FROM semesters
             ORDER BY
                   CASE
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 0
                     WHEN start_date > CURDATE() THEN 1
                     ELSE 2
                   END,
                   CASE WHEN start_date > CURDATE() THEN start_date END ASC,
                   CASE WHEN CURDATE() BETWEEN start_date AND end_date THEN start_date END DESC,
                   CASE WHEN CURDATE() > end_date THEN end_date END DESC,
                   id DESC
             LIMIT 1
            """)
    Map<String, Object> currentSemester();

    @Select("""
            <script>
            SELECT cot.id, cot.offering_id AS offeringId, cot.day_of_week AS dayOfWeek,
                   cot.start_section AS startSection, cot.end_section AS endSection,
                   cot.start_week AS startWeek, cot.end_week AS endWeek, cot.week_type AS weekType,
                   cot.classroom_id AS classroomId, CONCAT(cr.building, cr.room_no) AS classroom
              FROM course_offering_times cot
              JOIN classrooms cr ON cr.id = cot.classroom_id
             WHERE cot.offering_id IN
             <foreach collection="offeringIds" item="offeringId" open="(" separator="," close=")">
               #{offeringId}
             </foreach>
             ORDER BY cot.offering_id, cot.day_of_week, cot.start_section, cot.start_week
            </script>
            """)
    List<Map<String, Object>> offeringTimes(@Param("offeringIds") List<Long> offeringIds);
}
