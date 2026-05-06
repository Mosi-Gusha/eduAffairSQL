package com.student.management.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommonMapper {
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
            SELECT id, name, start_date AS startDate, end_date AS endDate,
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

    @Select("""
            SELECT id, name, start_date AS startDate, end_date AS endDate,
                   CASE
                     WHEN CURDATE() < start_date THEN 'not_started'
                     WHEN CURDATE() BETWEEN start_date AND end_date THEN 'active'
                     ELSE 'archived'
                   END AS status,
                   is_current AS isCurrent
              FROM semesters
             WHERE is_current = 1
             LIMIT 1
            """)
    Map<String, Object> currentSemester();
}
