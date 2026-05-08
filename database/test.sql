CREATE DATABASE IF NOT EXISTS test
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE test;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS notices;
DROP TABLE IF EXISTS grades;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS course_offering_times;
DROP TABLE IF EXISTS course_offerings;
DROP TABLE IF EXISTS classrooms;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS semesters;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS majors;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE roles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  email VARCHAR(120) NOT NULL UNIQUE,
  role_id BIGINT NOT NULL,
  status ENUM('enabled','disabled') NOT NULL DEFAULT 'enabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE departments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL UNIQUE,
  phone VARCHAR(32) NULL
) ENGINE=InnoDB;

CREATE TABLE majors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  duration_years TINYINT NOT NULL DEFAULT 4,
  CONSTRAINT fk_majors_department FOREIGN KEY (department_id) REFERENCES departments(id),
  UNIQUE KEY uk_major_department_name (department_id, name)
) ENGINE=InnoDB;

CREATE TABLE students (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  student_no VARCHAR(32) NOT NULL UNIQUE,
  major_id BIGINT NOT NULL,
  admission_year SMALLINT NOT NULL,
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_students_major FOREIGN KEY (major_id) REFERENCES majors(id)
) ENGINE=InnoDB;

CREATE TABLE teachers (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL UNIQUE,
  teacher_no VARCHAR(32) NOT NULL UNIQUE,
  department_id BIGINT NOT NULL,
  title VARCHAR(64) NOT NULL DEFAULT '讲师',
  CONSTRAINT fk_teachers_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_teachers_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB;

CREATE TABLE semesters (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL UNIQUE,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  max_credit DECIMAL(5,1) NOT NULL DEFAULT 30.0,
  is_current TINYINT(1) NOT NULL DEFAULT 0,
  CHECK (max_credit > 0)
) ENGINE=InnoDB;

CREATE TABLE courses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  department_id BIGINT NOT NULL,
  credit DECIMAL(3,1) NOT NULL,
  status ENUM('enabled','disabled') NOT NULL DEFAULT 'enabled',
  CONSTRAINT fk_courses_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB;

CREATE TABLE classrooms (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  building VARCHAR(64) NOT NULL,
  room_no VARCHAR(32) NOT NULL,
  capacity SMALLINT NOT NULL,
  UNIQUE KEY uk_room (building, room_no)
) ENGINE=InnoDB;

CREATE TABLE course_offerings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  semester_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  classroom_id BIGINT NOT NULL,
  capacity SMALLINT NOT NULL,
  selected_count SMALLINT NOT NULL DEFAULT 0,
  usual_ratio DECIMAL(4,2) NOT NULL DEFAULT 0.40,
  exam_ratio DECIMAL(4,2) NOT NULL DEFAULT 0.60,
  status ENUM('selecting','closed') NOT NULL DEFAULT 'selecting',
  CONSTRAINT fk_offerings_course FOREIGN KEY (course_id) REFERENCES courses(id),
  CONSTRAINT fk_offerings_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
  CONSTRAINT fk_offerings_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id),
  CONSTRAINT fk_offerings_room FOREIGN KEY (classroom_id) REFERENCES classrooms(id)
) ENGINE=InnoDB;

CREATE TABLE course_offering_times (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  offering_id BIGINT NOT NULL,
  day_of_week TINYINT NOT NULL COMMENT '1-7, Monday is 1',
  start_section TINYINT NOT NULL,
  end_section TINYINT NOT NULL,
  start_week TINYINT NOT NULL DEFAULT 1,
  end_week TINYINT NOT NULL DEFAULT 16,
  week_type ENUM('all','odd','even') NOT NULL DEFAULT 'all' COMMENT 'all/odd/even',
  CONSTRAINT fk_offering_times_offering FOREIGN KEY (offering_id) REFERENCES course_offerings(id) ON DELETE CASCADE,
  CHECK (day_of_week BETWEEN 1 AND 7),
  CHECK (start_section BETWEEN 1 AND 12),
  CHECK (end_section BETWEEN 1 AND 12),
  CHECK (start_section <= end_section),
  CHECK (start_week BETWEEN 1 AND 30),
  CHECK (end_week BETWEEN 1 AND 30),
  CHECK (start_week <= end_week),
  INDEX idx_offering_times_lookup (offering_id, day_of_week, start_section, end_section)
) ENGINE=InnoDB;

CREATE TABLE enrollments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  offering_id BIGINT NOT NULL,
  status ENUM('selected','dropped') NOT NULL DEFAULT 'selected',
  selected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dropped_at DATETIME NULL,
  CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students(id),
  CONSTRAINT fk_enrollments_offering FOREIGN KEY (offering_id) REFERENCES course_offerings(id),
  UNIQUE KEY uk_student_offering (student_id, offering_id)
) ENGINE=InnoDB;

CREATE TABLE grades (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  enrollment_id BIGINT NOT NULL UNIQUE,
  usual_score DECIMAL(5,2) NULL,
  exam_score DECIMAL(5,2) NULL,
  final_score DECIMAL(5,0) NULL,
  grade_point DECIMAL(3,2) NULL,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_grades_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
  CONSTRAINT fk_grades_user FOREIGN KEY (updated_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE notices (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  audience ENUM('all','teacher','student') NOT NULL DEFAULT 'all',
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notices_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

DELIMITER $$

CREATE TRIGGER trg_enrollments_after_insert
AFTER INSERT ON enrollments
FOR EACH ROW
BEGIN
  IF NEW.status = 'selected' THEN
    UPDATE course_offerings
       SET selected_count = selected_count + 1
     WHERE id = NEW.offering_id;
  END IF;
END$$

CREATE TRIGGER trg_enrollments_after_update
AFTER UPDATE ON enrollments
FOR EACH ROW
BEGIN
  IF OLD.status <> NEW.status THEN
    IF OLD.status = 'selected' AND NEW.status = 'dropped' THEN
      UPDATE course_offerings
         SET selected_count = GREATEST(selected_count - 1, 0)
       WHERE id = NEW.offering_id;
    ELSEIF OLD.status = 'dropped' AND NEW.status = 'selected' THEN
      UPDATE course_offerings
         SET selected_count = selected_count + 1
       WHERE id = NEW.offering_id;
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_grades_before_insert
BEFORE INSERT ON grades
FOR EACH ROW
BEGIN
  DECLARE v_usual_ratio DECIMAL(4,2) DEFAULT 0.40;
  DECLARE v_exam_ratio DECIMAL(4,2) DEFAULT 0.60;
  IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
    SELECT co.usual_ratio, co.exam_ratio
      INTO v_usual_ratio, v_exam_ratio
      FROM enrollments e
      JOIN course_offerings co ON co.id = e.offering_id
     WHERE e.id = NEW.enrollment_id;
    SET NEW.final_score = ROUND(NEW.usual_score * v_usual_ratio + NEW.exam_score * v_exam_ratio, 0);
    SET NEW.grade_point = CASE
      WHEN NEW.final_score >= 90 THEN 4.0
      WHEN NEW.final_score >= 85 THEN 3.7
      WHEN NEW.final_score >= 82 THEN 3.3
      WHEN NEW.final_score >= 78 THEN 3.0
      WHEN NEW.final_score >= 75 THEN 2.7
      WHEN NEW.final_score >= 72 THEN 2.3
      WHEN NEW.final_score >= 68 THEN 2.0
      WHEN NEW.final_score >= 66 THEN 1.7
      WHEN NEW.final_score >= 64 THEN 1.5
      WHEN NEW.final_score >= 60 THEN 1.0
      ELSE 0.0
    END;
  END IF;
END$$

CREATE TRIGGER trg_grades_before_update
BEFORE UPDATE ON grades
FOR EACH ROW
BEGIN
  DECLARE v_usual_ratio DECIMAL(4,2) DEFAULT 0.40;
  DECLARE v_exam_ratio DECIMAL(4,2) DEFAULT 0.60;
  IF NEW.usual_score IS NOT NULL AND NEW.exam_score IS NOT NULL THEN
    SELECT co.usual_ratio, co.exam_ratio
      INTO v_usual_ratio, v_exam_ratio
      FROM enrollments e
      JOIN course_offerings co ON co.id = e.offering_id
     WHERE e.id = NEW.enrollment_id;
    SET NEW.final_score = ROUND(NEW.usual_score * v_usual_ratio + NEW.exam_score * v_exam_ratio, 0);
    SET NEW.grade_point = CASE
      WHEN NEW.final_score >= 90 THEN 4.0
      WHEN NEW.final_score >= 85 THEN 3.7
      WHEN NEW.final_score >= 82 THEN 3.3
      WHEN NEW.final_score >= 78 THEN 3.0
      WHEN NEW.final_score >= 75 THEN 2.7
      WHEN NEW.final_score >= 72 THEN 2.3
      WHEN NEW.final_score >= 68 THEN 2.0
      WHEN NEW.final_score >= 66 THEN 1.7
      WHEN NEW.final_score >= 64 THEN 1.5
      WHEN NEW.final_score >= 60 THEN 1.0
      ELSE 0.0
    END;
  ELSE
    SET NEW.final_score = NULL;
    SET NEW.grade_point = NULL;
  END IF;
END$$

CREATE PROCEDURE sp_select_course(
  IN p_student_id BIGINT,
  IN p_offering_id BIGINT
)
BEGIN
  DECLARE v_capacity SMALLINT;
  DECLARE v_selected SMALLINT;
  DECLARE v_status VARCHAR(16);
  DECLARE v_is_current TINYINT;
  DECLARE v_semester BIGINT;
  DECLARE v_course BIGINT;
  DECLARE v_credit DECIMAL(3,1);
  DECLARE v_max_credit DECIMAL(5,1);
  DECLARE v_current_credit DECIMAL(5,1);
  DECLARE v_duplicate INT DEFAULT 0;
  DECLARE v_passed_before INT DEFAULT 0;
  DECLARE v_conflict INT DEFAULT 0;
  DECLARE v_sem_start DATE;
  DECLARE v_sem_end DATE;

  SELECT co.capacity, co.selected_count, co.status, co.semester_id, co.course_id,
         c.credit, s.is_current, s.start_date, s.end_date, s.max_credit
    INTO v_capacity, v_selected, v_status, v_semester, v_course,
         v_credit, v_is_current, v_sem_start, v_sem_end, v_max_credit
    FROM course_offerings co
    JOIN courses c ON c.id = co.course_id
    JOIN semesters s ON s.id = co.semester_id
   WHERE co.id = p_offering_id;

  SELECT COALESCE(SUM(c.credit), 0)
    INTO v_current_credit
    FROM enrollments e
    JOIN course_offerings co ON co.id = e.offering_id
    JOIN courses c ON c.id = co.course_id
   WHERE e.student_id = p_student_id
     AND e.status = 'selected'
     AND co.semester_id = v_semester;

  SELECT COUNT(*)
    INTO v_duplicate
    FROM enrollments e
    JOIN course_offerings co ON co.id = e.offering_id
   WHERE e.student_id = p_student_id
     AND e.status = 'selected'
     AND co.semester_id = v_semester
     AND co.course_id = v_course;

  SELECT COUNT(*)
    INTO v_passed_before
    FROM enrollments e
    JOIN course_offerings co ON co.id = e.offering_id
    JOIN grades g ON g.enrollment_id = e.id
   WHERE e.student_id = p_student_id
     AND e.status = 'selected'
     AND co.course_id = v_course
     AND co.semester_id <> v_semester
     AND g.final_score >= 60;

  SELECT COUNT(*)
    INTO v_conflict
    FROM enrollments e
    JOIN course_offerings co ON co.id = e.offering_id
    JOIN course_offering_times selected_time ON selected_time.offering_id = p_offering_id
    JOIN course_offering_times existing_time ON existing_time.offering_id = co.id
   WHERE e.student_id = p_student_id
     AND e.status = 'selected'
     AND co.semester_id = v_semester
     AND existing_time.day_of_week = selected_time.day_of_week
     AND NOT (existing_time.end_section < selected_time.start_section
              OR existing_time.start_section > selected_time.end_section)
     AND NOT (existing_time.end_week < selected_time.start_week
              OR existing_time.start_week > selected_time.end_week)
     AND (existing_time.week_type = 'all'
          OR selected_time.week_type = 'all'
          OR existing_time.week_type = selected_time.week_type);

  IF v_is_current <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '当前学期未设置';
  ELSEIF CURDATE() < v_sem_start OR CURDATE() > v_sem_end THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '当前日期不在学期起止日期内，不能选课';
  ELSEIF v_status <> 'selecting' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '当前课程未开放选课';
  ELSEIF v_selected >= v_capacity THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '课程容量已满';
  ELSEIF v_duplicate > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已选择同一门课程';
  ELSEIF v_passed_before > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '已通过的历史课程不能重复选修';
  ELSEIF v_conflict > 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '上课时间冲突';
  ELSEIF v_current_credit + v_credit > v_max_credit THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '超过本学期最大学分限制';
  ELSE
    INSERT INTO enrollments(student_id, offering_id, status, selected_at, dropped_at)
    VALUES(p_student_id, p_offering_id, 'selected', CURRENT_TIMESTAMP, NULL)
    ON DUPLICATE KEY UPDATE status = 'selected', selected_at = CURRENT_TIMESTAMP, dropped_at = NULL;
  END IF;
END$$

DELIMITER ;

INSERT INTO roles(id, code, name) VALUES
(1, 'admin', '系统管理员'),
(2, 'teacher', '教师'),
(3, 'student', '学生');

INSERT INTO users(id, username, password_hash, display_name, email, role_id, status, created_at) VALUES
(1, 'admin', '$2a$12$VCRhAfYLLRORBayYZJxP/e1hh3LsxdBzJtxNo3c4pzq05F8EGbsK6', '教务管理员', 'admin@school.edu.cn', 1, 'enabled', CURRENT_TIMESTAMP),
(2, 't1', '$2a$12$JcQvNUJeR4KOoMeMC6Go5utTRDvBU1Cu1AJYwQC5GSW8RWlf0/3qG', '张明', 't1@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(3, 't2', '$2a$12$gyjuA1wIPVl1gScz04c3R.8BC6e7bAzSyESxh5ztax48CRMsYZqBi', '李华', 't2@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(4, 't3', '$2a$12$EDNSQY4H62HPyXGshlt3b.2LaugU3AldtMXQaQPInVialQjbLsEnu', '王敏', 't3@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(5, 't4', '$2a$12$WmcuNtifWd9YMiXjR9JYB.VTfiMPK9dvi4/VFKc0DayI8pymwEUBu', '陈伟', 't4@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(6, 't5', '$2a$12$uS.2mKCC6sHFIc/7ui6dPetAmI.yp1fgGBlqf.l1bqyud8rgQZw7i', '刘芳', 't5@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(7, 't6', '$2a$12$ZQzueIC3QSiTpGJyvj3sCu4Zcs8yonf4P.RwT6VRTfzhRvgtauA7q', '赵强', 't6@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(8, 't7', '$2a$12$O9IPnv8//nRjw8eY5K5jtuIGOkxOoB5hq/Ky1pj4OuRyHTDclJjBO', '周琳', 't7@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(9, 't8', '$2a$12$Raqhwjv42HqLchMR8gMqRu3IsgxBGDCknQeY3fbyAalFZeR4HQD52', '孙杰', 't8@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(10, 't9', '$2a$12$l3ZvW6C8M3x2OJv5E2izUOfiGSIgFVt0V.SsEIFSp6RLQDtEJiMnu', '黄磊', 't9@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(11, 't10', '$2a$12$fN1Ud15iGbXxZ.J1U1pR1.8N5Wo3shPFezMzkXSSc6ildEGJTcslu', '吴倩', 't10@teacher.school.edu.cn', 2, 'enabled', CURRENT_TIMESTAMP),
(12, 's1', '$2a$12$9Woiqim64Cgq0dGBddqRres5qONZgA40TjTljK19butKnliYkOCiu', '周襦', 's1@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(13, 's2', '$2a$12$EhUG6P.0iOk2Sw0DtPzQ7.BIyTBGg/6Ukesq/tZHAZJ.4drfbypQe', '陈晨', 's2@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(14, 's3', '$2a$12$yjcCsi45b/Z.Cd9jixzpeONFBvOD1rMq8LMWowl26KK5uOOqAJleq', '周雨', 's3@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(15, 's4', '$2a$12$EVRSOXvncH4LEjv9BVsN/eSWaKQtytlixa9F1i2Ubk3ceqqSUcwVW', '王浩', 's4@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(16, 's5', '$2a$12$cbDJ2ewIk56L1xTQrOVnSOxaAkN9nSD51peE3rEBAVYTwhdVHfxRO', '刘欣', 's5@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(17, 's6', '$2a$12$VU0rCFMhKPHdfY5WA3LyVexQUzXnZl4I8ukUM7WRdlsWI3eHGftPq', '赵阳', 's6@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(18, 's7', '$2a$12$Ktig2oSsFCZeXZcer36zbOt/N6.l.sWJ6fUsfe6mKxfVISoUPi6Nu', '孙悦', 's7@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(19, 's8', '$2a$12$5yF0OHUWCokre0UmsqrU2OMiyRT8Eo.wjsWHJ03YQ6WsnIy/eUkDC', '黄宇', 's8@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(20, 's9', '$2a$12$/tmspDk3ESi1iVtvTPUuOOpTXsY0TSOQVhgaeFd5xKaLTbBtkHjtO', '吴迪', 's9@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(21, 's10', '$2a$12$pva6S9YNhXUhaiSJ3D9b.eM/oJh2hrJFKhatUkFPY8cTAXSdHhChq', '郑雪', 's10@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(22, 's11', '$2a$12$x9JpMjqVuFwrIkUiH3LvIuKVaGfaqLUff1Q5QX57H9EqocXeAkWOG', '康榕', 's11@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(23, 's12', '$2a$12$k5dGIClfw7/OiRJDCo3ZmOOKRjhlM7Afac6rGSVHOo57WBSNhvr9S', '谢昪', 's12@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(24, 's13', '$2a$12$GeijYl9LG/0MTEg8BQ3spO/3lQE/KPXM7UEZcuL0k9knZ4axwbbCa', '广律', 's13@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(25, 's14', '$2a$12$TXIGM41ow87l06NOGJqPAeBfZhSGKBT4sNXaukNeVZuqOvgMGXihy', '詹贤', 's14@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(26, 's15', '$2a$12$IcaI5lh4/llI6sxqi9QIKOkIkF02Jtq18Qn8OoDeNQTBMVtxyKHT6', '濮佳', 's15@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(27, 's16', '$2a$12$y7pP6fNGMkIU6xP7n1kdR.aMqyAHV6VMfKEon4rvXEcQ1keZsPLU2', '阚文', 's16@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(28, 's17', '$2a$12$yLObE27idzeBlGmFcmdTce7GZIPKDkiE2CVcCfKU4IW7oURcRbq2C', '牧昱', 's17@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(29, 's18', '$2a$12$VOUzegNK95bB.028bB71/uqVwOO0kOxPt1yMTv7iqC2W0r1qE69.S', '盖虚', 's18@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(30, 's19', '$2a$12$170umxvOFE/Vnyh61f1PAuDtiWkMKeCagC4Woslqi9dnWj037n02q', '权孜', 's19@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP),
(31, 's20', '$2a$12$Jteqfn2fObnIwIaXj0l4muze9TvHIYdJrDaM.gpYkmI9GQFYFDAsa', '魏亨', 's20@student.school.edu.cn', 3, 'enabled', CURRENT_TIMESTAMP);

INSERT INTO departments(id, name, phone) VALUES
(1, '计算机学院', '010-6001001'),
(2, '经济管理学院', '010-6001002'),
(3, '外国语学院', '010-6001003'),
(4, '数学学院', '010-6001004'),
(5, '电子信息学院', '010-6001005'),
(6, '艺术设计学院', '010-6001006'),
(7, '马克思主义学院', '010-6001007'),
(8, '体育学院', '010-6001008');

INSERT INTO majors(id, department_id, name, duration_years) VALUES
(1, 1, '软件工程', 4),
(2, 1, '人工智能', 4),
(3, 1, '网络工程', 4),
(4, 1, '数据科学与大数据技术', 4),
(5, 2, '工商管理', 4),
(6, 2, '会计学', 4),
(7, 3, '英语', 4),
(8, 3, '商务英语', 4),
(9, 4, '应用数学', 4),
(10, 5, '通信工程', 4),
(11, 5, '电子信息工程', 4),
(12, 6, '数字媒体艺术', 4),
(13, 7, '思政教育', 4),
(14, 8, '公共体育', 4);

INSERT INTO teachers(id, user_id, teacher_no, department_id, title) VALUES
(1, 2, 't1', 1, '教授'),
(2, 3, 't2', 1, '副教授'),
(3, 4, 't3', 4, '教授'),
(4, 5, 't4', 3, '讲师'),
(5, 6, 't5', 2, '副教授'),
(6, 7, 't6', 5, '教授'),
(7, 8, 't7', 7, '讲师'),
(8, 9, 't8', 8, '讲师'),
(9, 10, 't9', 1, '副教授'),
(10, 11, 't10', 6, '讲师');

INSERT INTO students(id, user_id, student_no, major_id, admission_year) VALUES
(1, 12, 's1', 1, 2023),
(2, 13, 's2', 1, 2023),
(3, 14, 's3', 2, 2023),
(4, 15, 's4', 9, 2022),
(5, 16, 's5', 7, 2024),
(6, 17, 's6', 5, 2023),
(7, 18, 's7', 10, 2022),
(8, 19, 's8', 3, 2023),
(9, 20, 's9', 12, 2024),
(10, 21, 's10', 13, 2023),
(11, 22, 's11', 11, 2024),
(12, 23, 's12', 12, 2022),
(13, 24, 's13', 13, 2023),
(14, 25, 's14', 14, 2024),
(15, 26, 's15', 1, 2022),
(16, 27, 's16', 2, 2023),
(17, 28, 's17', 3, 2024),
(18, 29, 's18', 4, 2022),
(19, 30, 's19', 5, 2023),
(20, 31, 's20', 6, 2024);

INSERT INTO semesters(id, name, start_date, end_date, max_credit, is_current) VALUES
(1, '2025-2026 学年第二学期', '2026-02-23', '2026-07-05', 30.0, 1),
(2, '2025-2026 学年第一学期', '2025-09-01', '2026-01-10', 30.0, 0),
(3, '2024-2025 学年第二学期', '2025-02-24', '2025-07-06', 30.0, 0);

INSERT INTO courses(id, code, name, department_id, credit, status) VALUES
(1, 'C001', '数据结构', 1, 3.0, 'enabled'),
(2, 'C002', '操作系统', 1, 4.0, 'enabled'),
(3, 'C003', '高等数学A', 4, 5.0, 'enabled'),
(4, 'C004', '大学英语II', 3, 2.0, 'enabled'),
(5, 'C005', '管理学原理', 2, 3.0, 'enabled'),
(6, 'C006', '通信原理', 5, 4.0, 'enabled'),
(7, 'C007', '思想道德与法治', 7, 2.0, 'enabled'),
(8, 'C008', '大学体育', 8, 1.0, 'enabled'),
(9, 'C009', '计算机网络', 1, 3.0, 'enabled'),
(10, 'C010', '交互设计', 6, 3.0, 'enabled'),
(11, 'C011', '数据库系统', 1, 3.0, 'enabled'),
(12, 'C012', '人工智能导论', 1, 3.0, 'enabled'),
(13, 'C013', '概率论与数理统计', 4, 3.0, 'enabled'),
(14, 'C014', '英语口语实践', 3, 2.0, 'enabled'),
(15, 'C015', '财务管理', 2, 3.0, 'enabled'),
(16, 'C016', '数字信号处理', 5, 4.0, 'enabled'),
(17, 'C017', '马克思主义基本原理', 7, 3.0, 'enabled'),
(18, 'C018', '篮球专项', 8, 1.0, 'enabled'),
(19, 'C019', '网络安全基础', 1, 3.0, 'enabled'),
(20, 'C020', '数字影像基础', 6, 2.0, 'enabled'),
(21, 'C021', '软件工程实践', 1, 2.0, 'enabled'),
(22, 'C022', '机器学习', 1, 3.0, 'enabled'),
(23, 'C023', '离散数学', 4, 3.0, 'enabled'),
(24, 'C024', '跨文化沟通', 3, 2.0, 'enabled'),
(25, 'C025', '市场营销', 2, 3.0, 'enabled'),
(26, 'C026', '嵌入式系统', 5, 3.0, 'enabled'),
(27, 'C027', '中国近现代史纲要', 7, 2.0, 'enabled'),
(28, 'C028', '形体训练', 8, 1.0, 'enabled'),
(29, 'C029', '云计算平台', 1, 3.0, 'enabled'),
(30, 'C030', '用户体验研究', 6, 2.0, 'enabled'),
(31, 'C031', '编译原理', 1, 3.0, 'enabled'),
(32, 'C032', '深度学习', 1, 3.0, 'enabled'),
(33, 'C033', '数学建模', 4, 2.0, 'enabled'),
(34, 'C034', '翻译理论与实践', 3, 2.0, 'enabled'),
(35, 'C035', '组织行为学', 2, 2.0, 'enabled'),
(36, 'C036', '移动通信', 5, 3.0, 'enabled'),
(37, 'C037', '伦理学导论', 7, 2.0, 'enabled'),
(38, 'C038', '羽毛球专项', 8, 1.0, 'enabled'),
(39, 'C039', '信息系统项目管理', 1, 2.0, 'enabled'),
(40, 'C040', '三维动画设计', 6, 3.0, 'enabled');

INSERT INTO classrooms(id, building, room_no, capacity) VALUES
(1, 'A', '101', 80),
(2, 'B', '203', 70),
(3, 'C', '301', 120),
(4, 'D', '201', 100),
(5, 'E', '102', 90),
(6, 'B', '305', 60),
(7, 'F', '101', 150),
(8, '体育馆', '1', 40),
(9, 'A', '302', 75),
(10, 'G', '204', 45),
(11, '实验楼', '305', 45),
(12, 'A', '501', 55);

INSERT INTO course_offerings(id, course_id, semester_id, teacher_id, classroom_id, capacity, selected_count, usual_ratio, exam_ratio, status) VALUES
(1, 1, 1, 1, 1, 80, 0, 0.40, 0.60, 'selecting'),
(2, 2, 1, 2, 2, 70, 0, 0.30, 0.70, 'selecting'),
(3, 3, 1, 3, 3, 120, 0, 0.40, 0.60, 'selecting'),
(4, 4, 1, 4, 4, 100, 0, 0.50, 0.50, 'selecting'),
(5, 5, 1, 5, 5, 90, 0, 0.40, 0.60, 'selecting'),
(6, 6, 1, 6, 6, 60, 0, 0.30, 0.70, 'selecting'),
(7, 7, 1, 7, 7, 150, 0, 0.50, 0.50, 'selecting'),
(8, 8, 1, 8, 8, 40, 0, 0.70, 0.30, 'selecting'),
(9, 9, 1, 9, 9, 75, 0, 0.40, 0.60, 'selecting'),
(10, 10, 1, 10, 10, 45, 0, 0.50, 0.50, 'selecting'),
(11, 11, 1, 1, 1, 80, 0, 0.40, 0.60, 'selecting'),
(12, 12, 1, 2, 2, 70, 0, 0.40, 0.60, 'selecting'),
(13, 13, 1, 3, 3, 110, 0, 0.40, 0.60, 'selecting'),
(14, 14, 1, 4, 4, 55, 0, 0.60, 0.40, 'selecting'),
(15, 15, 1, 5, 5, 80, 0, 0.40, 0.60, 'selecting'),
(16, 16, 1, 6, 6, 60, 0, 0.30, 0.70, 'selecting'),
(17, 17, 1, 7, 7, 120, 0, 0.50, 0.50, 'selecting'),
(18, 18, 1, 8, 8, 36, 0, 0.70, 0.30, 'selecting'),
(19, 19, 1, 9, 9, 60, 0, 0.40, 0.60, 'selecting'),
(20, 20, 1, 10, 10, 48, 0, 0.50, 0.50, 'selecting'),
(21, 21, 1, 1, 11, 45, 0, 0.60, 0.40, 'selecting'),
(22, 22, 1, 2, 2, 50, 0, 0.40, 0.60, 'selecting'),
(23, 23, 1, 3, 3, 100, 0, 0.40, 0.60, 'selecting'),
(24, 24, 1, 4, 4, 60, 0, 0.50, 0.50, 'selecting'),
(25, 25, 1, 5, 5, 75, 0, 0.40, 0.60, 'selecting'),
(26, 26, 1, 6, 6, 45, 0, 0.40, 0.60, 'selecting'),
(27, 27, 1, 7, 7, 140, 0, 0.50, 0.50, 'selecting'),
(28, 28, 1, 8, 8, 35, 0, 0.70, 0.30, 'selecting'),
(29, 29, 1, 9, 12, 55, 0, 0.40, 0.60, 'selecting'),
(30, 30, 1, 10, 10, 40, 0, 0.50, 0.50, 'selecting'),
(31, 31, 1, 1, 1, 65, 0, 0.40, 0.60, 'selecting'),
(32, 32, 1, 2, 2, 50, 0, 0.40, 0.60, 'selecting'),
(33, 33, 1, 3, 3, 80, 0, 0.60, 0.40, 'selecting'),
(34, 34, 1, 4, 4, 50, 0, 0.50, 0.50, 'selecting'),
(35, 35, 1, 5, 5, 70, 0, 0.50, 0.50, 'selecting'),
(36, 36, 1, 6, 6, 55, 0, 0.40, 0.60, 'selecting'),
(37, 37, 1, 7, 7, 95, 0, 0.50, 0.50, 'selecting'),
(38, 38, 1, 8, 8, 32, 0, 0.70, 0.30, 'selecting'),
(39, 39, 1, 9, 9, 60, 0, 0.50, 0.50, 'selecting'),
(40, 40, 1, 10, 10, 42, 0, 0.50, 0.50, 'closed');

INSERT INTO course_offering_times(id, offering_id, day_of_week, start_section, end_section, start_week, end_week, week_type) VALUES
(1, 1, 1, 1, 2, 1, 16, 'all'),
(2, 2, 2, 3, 4, 1, 16, 'all'),
(3, 3, 3, 1, 2, 1, 16, 'all'),
(4, 4, 4, 3, 4, 1, 16, 'all'),
(5, 5, 5, 1, 2, 1, 16, 'all'),
(6, 6, 1, 5, 6, 1, 16, 'all'),
(7, 7, 2, 7, 8, 1, 16, 'all'),
(8, 8, 3, 7, 8, 1, 16, 'all'),
(9, 9, 4, 5, 6, 1, 16, 'all'),
(10, 10, 5, 3, 4, 1, 16, 'all'),
(11, 11, 2, 1, 2, 1, 16, 'all'),
(12, 12, 3, 3, 4, 1, 16, 'all'),
(13, 13, 1, 3, 4, 1, 16, 'all'),
(14, 14, 2, 5, 6, 1, 16, 'all'),
(15, 15, 3, 5, 6, 1, 16, 'all'),
(16, 16, 4, 1, 2, 1, 16, 'all'),
(17, 17, 5, 5, 6, 1, 16, 'all'),
(18, 18, 1, 7, 8, 1, 16, 'all'),
(19, 19, 2, 3, 4, 1, 16, 'all'),
(20, 20, 4, 7, 8, 1, 16, 'all'),
(21, 21, 5, 7, 8, 1, 16, 'all'),
(22, 22, 1, 5, 6, 1, 16, 'all'),
(23, 23, 2, 1, 2, 1, 16, 'all'),
(24, 24, 3, 7, 8, 1, 16, 'all'),
(25, 25, 4, 3, 4, 1, 16, 'all'),
(26, 26, 5, 1, 2, 1, 16, 'all'),
(27, 27, 1, 1, 2, 1, 16, 'all'),
(28, 28, 2, 7, 8, 1, 16, 'all'),
(29, 29, 3, 5, 6, 1, 16, 'all'),
(30, 30, 5, 3, 4, 1, 16, 'all'),
(31, 31, 1, 3, 4, 1, 16, 'all'),
(32, 32, 2, 5, 6, 1, 16, 'all'),
(33, 33, 4, 5, 6, 1, 16, 'all'),
(34, 34, 5, 5, 6, 1, 16, 'all'),
(35, 35, 1, 7, 8, 1, 16, 'all'),
(36, 36, 2, 1, 2, 1, 16, 'all'),
(37, 37, 3, 3, 4, 1, 16, 'all'),
(38, 38, 4, 7, 8, 1, 16, 'all'),
(39, 39, 5, 1, 2, 1, 16, 'all'),
(40, 40, 3, 1, 2, 1, 16, 'all');

INSERT INTO course_offerings(id, course_id, semester_id, teacher_id, classroom_id, capacity, selected_count, usual_ratio, exam_ratio, status) VALUES
(41, 1, 2, 1, 1, 80, 0, 0.40, 0.60, 'closed'),
(42, 2, 2, 2, 2, 70, 0, 0.30, 0.70, 'closed'),
(43, 3, 2, 3, 3, 120, 0, 0.40, 0.60, 'closed'),
(44, 4, 2, 4, 4, 100, 0, 0.50, 0.50, 'closed'),
(45, 5, 2, 5, 5, 90, 0, 0.40, 0.60, 'closed'),
(46, 6, 2, 6, 6, 60, 0, 0.30, 0.70, 'closed'),
(47, 7, 2, 7, 7, 150, 0, 0.50, 0.50, 'closed'),
(48, 8, 2, 8, 8, 40, 0, 0.70, 0.30, 'closed'),
(49, 9, 2, 9, 9, 75, 0, 0.40, 0.60, 'closed'),
(50, 10, 2, 10, 10, 45, 0, 0.50, 0.50, 'closed'),
(51, 11, 2, 1, 1, 80, 0, 0.40, 0.60, 'closed'),
(52, 12, 2, 2, 2, 70, 0, 0.40, 0.60, 'closed'),
(53, 13, 2, 3, 3, 110, 0, 0.40, 0.60, 'closed'),
(54, 14, 2, 4, 4, 55, 0, 0.60, 0.40, 'closed'),
(55, 15, 2, 5, 5, 80, 0, 0.40, 0.60, 'closed'),
(56, 16, 2, 6, 6, 60, 0, 0.30, 0.70, 'closed'),
(57, 17, 2, 7, 7, 120, 0, 0.50, 0.50, 'closed'),
(58, 18, 2, 8, 8, 36, 0, 0.70, 0.30, 'closed'),
(59, 19, 2, 9, 9, 60, 0, 0.40, 0.60, 'closed'),
(60, 20, 2, 10, 10, 48, 0, 0.50, 0.50, 'closed'),
(61, 1, 3, 1, 1, 80, 0, 0.40, 0.60, 'closed'),
(62, 2, 3, 2, 2, 70, 0, 0.30, 0.70, 'closed'),
(63, 3, 3, 3, 3, 120, 0, 0.40, 0.60, 'closed'),
(64, 4, 3, 4, 4, 100, 0, 0.50, 0.50, 'closed'),
(65, 5, 3, 5, 5, 90, 0, 0.40, 0.60, 'closed'),
(66, 6, 3, 6, 6, 60, 0, 0.30, 0.70, 'closed'),
(67, 7, 3, 7, 7, 150, 0, 0.50, 0.50, 'closed'),
(68, 8, 3, 8, 8, 40, 0, 0.70, 0.30, 'closed'),
(69, 9, 3, 9, 9, 75, 0, 0.40, 0.60, 'closed'),
(70, 10, 3, 10, 10, 45, 0, 0.50, 0.50, 'closed');

INSERT INTO course_offering_times(id, offering_id, day_of_week, start_section, end_section, start_week, end_week, week_type) VALUES
(41, 41, 1, 1, 2, 1, 16, 'all'),
(42, 42, 2, 3, 4, 1, 16, 'all'),
(43, 43, 3, 1, 2, 1, 16, 'all'),
(44, 44, 4, 3, 4, 1, 16, 'all'),
(45, 45, 5, 1, 2, 1, 16, 'all'),
(46, 46, 1, 5, 6, 1, 16, 'all'),
(47, 47, 2, 7, 8, 1, 16, 'all'),
(48, 48, 3, 7, 8, 1, 16, 'all'),
(49, 49, 4, 5, 6, 1, 16, 'all'),
(50, 50, 5, 3, 4, 1, 16, 'all'),
(51, 51, 2, 1, 2, 1, 16, 'all'),
(52, 52, 3, 3, 4, 1, 16, 'all'),
(53, 53, 1, 3, 4, 1, 16, 'all'),
(54, 54, 2, 5, 6, 1, 16, 'all'),
(55, 55, 3, 5, 6, 1, 16, 'all'),
(56, 56, 4, 1, 2, 1, 16, 'all'),
(57, 57, 5, 5, 6, 1, 16, 'all'),
(58, 58, 1, 7, 8, 1, 16, 'all'),
(59, 59, 2, 3, 4, 1, 16, 'all'),
(60, 60, 4, 7, 8, 1, 16, 'all'),
(61, 61, 1, 1, 2, 1, 16, 'all'),
(62, 62, 2, 3, 4, 1, 16, 'all'),
(63, 63, 3, 1, 2, 1, 16, 'all'),
(64, 64, 4, 3, 4, 1, 16, 'all'),
(65, 65, 5, 1, 2, 1, 16, 'all'),
(66, 66, 1, 5, 6, 1, 16, 'all'),
(67, 67, 2, 7, 8, 1, 16, 'all'),
(68, 68, 3, 7, 8, 1, 16, 'all'),
(69, 69, 4, 5, 6, 1, 16, 'all'),
(70, 70, 5, 3, 4, 1, 16, 'all');

INSERT INTO enrollments(id, student_id, offering_id, status, selected_at, dropped_at) VALUES
(1, 1, 21, 'selected', CURRENT_TIMESTAMP, NULL),
(2, 1, 25, 'selected', CURRENT_TIMESTAMP, NULL),
(3, 1, 29, 'selected', CURRENT_TIMESTAMP, NULL),
(4, 1, 33, 'selected', CURRENT_TIMESTAMP, NULL),
(5, 1, 37, 'selected', CURRENT_TIMESTAMP, NULL),
(6, 1, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(7, 1, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(8, 1, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(9, 1, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(10, 1, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(11, 1, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(12, 1, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(13, 1, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(14, 1, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(15, 1, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(16, 2, 22, 'selected', CURRENT_TIMESTAMP, NULL),
(17, 2, 26, 'selected', CURRENT_TIMESTAMP, NULL),
(18, 2, 30, 'selected', CURRENT_TIMESTAMP, NULL),
(19, 2, 34, 'selected', CURRENT_TIMESTAMP, NULL),
(20, 2, 38, 'selected', CURRENT_TIMESTAMP, NULL),
(21, 2, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(22, 2, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(23, 2, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(24, 2, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(25, 2, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(26, 2, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(27, 2, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(28, 2, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(29, 2, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(30, 2, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(31, 3, 23, 'selected', CURRENT_TIMESTAMP, NULL),
(32, 3, 27, 'selected', CURRENT_TIMESTAMP, NULL),
(33, 3, 31, 'selected', CURRENT_TIMESTAMP, NULL),
(34, 3, 35, 'selected', CURRENT_TIMESTAMP, NULL),
(35, 3, 39, 'selected', CURRENT_TIMESTAMP, NULL),
(36, 3, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(37, 3, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(38, 3, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(39, 3, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(40, 3, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(41, 3, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(42, 3, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(43, 3, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(44, 3, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(45, 3, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(46, 4, 24, 'selected', CURRENT_TIMESTAMP, NULL),
(47, 4, 28, 'selected', CURRENT_TIMESTAMP, NULL),
(48, 4, 32, 'selected', CURRENT_TIMESTAMP, NULL),
(49, 4, 36, 'selected', CURRENT_TIMESTAMP, NULL),
(50, 4, 21, 'selected', CURRENT_TIMESTAMP, NULL),
(51, 4, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(52, 4, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(53, 4, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(54, 4, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(55, 4, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(56, 4, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(57, 4, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(58, 4, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(59, 4, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(60, 4, 62, 'selected', CURRENT_TIMESTAMP, NULL);

INSERT INTO enrollments(id, student_id, offering_id, status, selected_at, dropped_at) VALUES
(61, 5, 25, 'selected', CURRENT_TIMESTAMP, NULL),
(62, 5, 29, 'selected', CURRENT_TIMESTAMP, NULL),
(63, 5, 33, 'selected', CURRENT_TIMESTAMP, NULL),
(64, 5, 37, 'selected', CURRENT_TIMESTAMP, NULL),
(65, 5, 22, 'selected', CURRENT_TIMESTAMP, NULL),
(66, 5, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(67, 5, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(68, 5, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(69, 5, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(70, 5, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(71, 5, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(72, 5, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(73, 5, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(74, 5, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(75, 5, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(76, 6, 26, 'selected', CURRENT_TIMESTAMP, NULL),
(77, 6, 30, 'selected', CURRENT_TIMESTAMP, NULL),
(78, 6, 34, 'selected', CURRENT_TIMESTAMP, NULL),
(79, 6, 38, 'selected', CURRENT_TIMESTAMP, NULL),
(80, 6, 23, 'selected', CURRENT_TIMESTAMP, NULL),
(81, 6, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(82, 6, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(83, 6, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(84, 6, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(85, 6, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(86, 6, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(87, 6, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(88, 6, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(89, 6, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(90, 6, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(91, 7, 27, 'selected', CURRENT_TIMESTAMP, NULL),
(92, 7, 31, 'selected', CURRENT_TIMESTAMP, NULL),
(93, 7, 35, 'selected', CURRENT_TIMESTAMP, NULL),
(94, 7, 39, 'selected', CURRENT_TIMESTAMP, NULL),
(95, 7, 24, 'selected', CURRENT_TIMESTAMP, NULL),
(96, 7, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(97, 7, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(98, 7, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(99, 7, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(100, 7, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(101, 7, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(102, 7, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(103, 7, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(104, 7, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(105, 7, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(106, 8, 28, 'selected', CURRENT_TIMESTAMP, NULL),
(107, 8, 32, 'selected', CURRENT_TIMESTAMP, NULL),
(108, 8, 36, 'selected', CURRENT_TIMESTAMP, NULL),
(109, 8, 21, 'selected', CURRENT_TIMESTAMP, NULL),
(110, 8, 25, 'selected', CURRENT_TIMESTAMP, NULL),
(111, 8, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(112, 8, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(113, 8, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(114, 8, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(115, 8, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(116, 8, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(117, 8, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(118, 8, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(119, 8, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(120, 8, 66, 'selected', CURRENT_TIMESTAMP, NULL);

INSERT INTO enrollments(id, student_id, offering_id, status, selected_at, dropped_at) VALUES
(121, 9, 29, 'selected', CURRENT_TIMESTAMP, NULL),
(122, 9, 33, 'selected', CURRENT_TIMESTAMP, NULL),
(123, 9, 37, 'selected', CURRENT_TIMESTAMP, NULL),
(124, 9, 22, 'selected', CURRENT_TIMESTAMP, NULL),
(125, 9, 26, 'selected', CURRENT_TIMESTAMP, NULL),
(126, 9, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(127, 9, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(128, 9, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(129, 9, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(130, 9, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(131, 9, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(132, 9, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(133, 9, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(134, 9, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(135, 9, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(136, 10, 30, 'selected', CURRENT_TIMESTAMP, NULL),
(137, 10, 34, 'selected', CURRENT_TIMESTAMP, NULL),
(138, 10, 38, 'selected', CURRENT_TIMESTAMP, NULL),
(139, 10, 23, 'selected', CURRENT_TIMESTAMP, NULL),
(140, 10, 27, 'selected', CURRENT_TIMESTAMP, NULL),
(141, 10, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(142, 10, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(143, 10, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(144, 10, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(145, 10, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(146, 10, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(147, 10, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(148, 10, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(149, 10, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(150, 10, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(151, 11, 31, 'selected', CURRENT_TIMESTAMP, NULL),
(152, 11, 35, 'selected', CURRENT_TIMESTAMP, NULL),
(153, 11, 39, 'selected', CURRENT_TIMESTAMP, NULL),
(154, 11, 24, 'selected', CURRENT_TIMESTAMP, NULL),
(155, 11, 28, 'selected', CURRENT_TIMESTAMP, NULL),
(156, 11, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(157, 11, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(158, 11, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(159, 11, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(160, 11, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(161, 11, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(162, 11, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(163, 11, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(164, 11, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(165, 11, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(166, 12, 32, 'selected', CURRENT_TIMESTAMP, NULL),
(167, 12, 36, 'selected', CURRENT_TIMESTAMP, NULL),
(168, 12, 21, 'selected', CURRENT_TIMESTAMP, NULL),
(169, 12, 25, 'selected', CURRENT_TIMESTAMP, NULL),
(170, 12, 29, 'selected', CURRENT_TIMESTAMP, NULL),
(171, 12, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(172, 12, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(173, 12, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(174, 12, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(175, 12, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(176, 12, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(177, 12, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(178, 12, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(179, 12, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(180, 12, 70, 'selected', CURRENT_TIMESTAMP, NULL);

INSERT INTO enrollments(id, student_id, offering_id, status, selected_at, dropped_at) VALUES
(181, 13, 33, 'selected', CURRENT_TIMESTAMP, NULL),
(182, 13, 37, 'selected', CURRENT_TIMESTAMP, NULL),
(183, 13, 22, 'selected', CURRENT_TIMESTAMP, NULL),
(184, 13, 26, 'selected', CURRENT_TIMESTAMP, NULL),
(185, 13, 30, 'selected', CURRENT_TIMESTAMP, NULL),
(186, 13, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(187, 13, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(188, 13, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(189, 13, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(190, 13, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(191, 13, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(192, 13, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(193, 13, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(194, 13, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(195, 13, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(196, 14, 34, 'selected', CURRENT_TIMESTAMP, NULL),
(197, 14, 38, 'selected', CURRENT_TIMESTAMP, NULL),
(198, 14, 23, 'selected', CURRENT_TIMESTAMP, NULL),
(199, 14, 27, 'selected', CURRENT_TIMESTAMP, NULL),
(200, 14, 31, 'selected', CURRENT_TIMESTAMP, NULL),
(201, 14, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(202, 14, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(203, 14, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(204, 14, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(205, 14, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(206, 14, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(207, 14, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(208, 14, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(209, 14, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(210, 14, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(211, 15, 35, 'selected', CURRENT_TIMESTAMP, NULL),
(212, 15, 39, 'selected', CURRENT_TIMESTAMP, NULL),
(213, 15, 24, 'selected', CURRENT_TIMESTAMP, NULL),
(214, 15, 28, 'selected', CURRENT_TIMESTAMP, NULL),
(215, 15, 32, 'selected', CURRENT_TIMESTAMP, NULL),
(216, 15, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(217, 15, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(218, 15, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(219, 15, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(220, 15, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(221, 15, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(222, 15, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(223, 15, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(224, 15, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(225, 15, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(226, 16, 36, 'selected', CURRENT_TIMESTAMP, NULL),
(227, 16, 21, 'selected', CURRENT_TIMESTAMP, NULL),
(228, 16, 25, 'selected', CURRENT_TIMESTAMP, NULL),
(229, 16, 29, 'selected', CURRENT_TIMESTAMP, NULL),
(230, 16, 33, 'selected', CURRENT_TIMESTAMP, NULL),
(231, 16, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(232, 16, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(233, 16, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(234, 16, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(235, 16, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(236, 16, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(237, 16, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(238, 16, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(239, 16, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(240, 16, 64, 'selected', CURRENT_TIMESTAMP, NULL);

INSERT INTO enrollments(id, student_id, offering_id, status, selected_at, dropped_at) VALUES
(241, 17, 37, 'selected', CURRENT_TIMESTAMP, NULL),
(242, 17, 22, 'selected', CURRENT_TIMESTAMP, NULL),
(243, 17, 26, 'selected', CURRENT_TIMESTAMP, NULL),
(244, 17, 30, 'selected', CURRENT_TIMESTAMP, NULL),
(245, 17, 34, 'selected', CURRENT_TIMESTAMP, NULL),
(246, 17, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(247, 17, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(248, 17, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(249, 17, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(250, 17, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(251, 17, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(252, 17, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(253, 17, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(254, 17, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(255, 17, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(256, 18, 38, 'selected', CURRENT_TIMESTAMP, NULL),
(257, 18, 23, 'selected', CURRENT_TIMESTAMP, NULL),
(258, 18, 27, 'selected', CURRENT_TIMESTAMP, NULL),
(259, 18, 31, 'selected', CURRENT_TIMESTAMP, NULL),
(260, 18, 35, 'selected', CURRENT_TIMESTAMP, NULL),
(261, 18, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(262, 18, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(263, 18, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(264, 18, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(265, 18, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(266, 18, 68, 'selected', CURRENT_TIMESTAMP, NULL),
(267, 18, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(268, 18, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(269, 18, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(270, 18, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(271, 19, 39, 'selected', CURRENT_TIMESTAMP, NULL),
(272, 19, 24, 'selected', CURRENT_TIMESTAMP, NULL),
(273, 19, 28, 'selected', CURRENT_TIMESTAMP, NULL),
(274, 19, 32, 'selected', CURRENT_TIMESTAMP, NULL),
(275, 19, 36, 'selected', CURRENT_TIMESTAMP, NULL),
(276, 19, 59, 'selected', CURRENT_TIMESTAMP, NULL),
(277, 19, 51, 'selected', CURRENT_TIMESTAMP, NULL),
(278, 19, 53, 'selected', CURRENT_TIMESTAMP, NULL),
(279, 19, 55, 'selected', CURRENT_TIMESTAMP, NULL),
(280, 19, 57, 'selected', CURRENT_TIMESTAMP, NULL),
(281, 19, 69, 'selected', CURRENT_TIMESTAMP, NULL),
(282, 19, 61, 'selected', CURRENT_TIMESTAMP, NULL),
(283, 19, 63, 'selected', CURRENT_TIMESTAMP, NULL),
(284, 19, 65, 'selected', CURRENT_TIMESTAMP, NULL),
(285, 19, 67, 'selected', CURRENT_TIMESTAMP, NULL),
(286, 20, 21, 'selected', CURRENT_TIMESTAMP, NULL),
(287, 20, 25, 'selected', CURRENT_TIMESTAMP, NULL),
(288, 20, 29, 'selected', CURRENT_TIMESTAMP, NULL),
(289, 20, 33, 'selected', CURRENT_TIMESTAMP, NULL),
(290, 20, 37, 'selected', CURRENT_TIMESTAMP, NULL),
(291, 20, 60, 'selected', CURRENT_TIMESTAMP, NULL),
(292, 20, 52, 'selected', CURRENT_TIMESTAMP, NULL),
(293, 20, 54, 'selected', CURRENT_TIMESTAMP, NULL),
(294, 20, 56, 'selected', CURRENT_TIMESTAMP, NULL),
(295, 20, 58, 'selected', CURRENT_TIMESTAMP, NULL),
(296, 20, 70, 'selected', CURRENT_TIMESTAMP, NULL),
(297, 20, 62, 'selected', CURRENT_TIMESTAMP, NULL),
(298, 20, 64, 'selected', CURRENT_TIMESTAMP, NULL),
(299, 20, 66, 'selected', CURRENT_TIMESTAMP, NULL),
(300, 20, 68, 'selected', CURRENT_TIMESTAMP, NULL);

INSERT INTO grades(id, enrollment_id, usual_score, exam_score, final_score, grade_point, updated_by, updated_at) VALUES
(1, 6, 64.00, 81.00, 74.20, 2.30, 8, CURRENT_TIMESTAMP),
(2, 7, 71.00, 92.00, 83.60, 3.30, 9, CURRENT_TIMESTAMP),
(3, 8, 78.00, 63.00, 69.00, 2.00, 10, CURRENT_TIMESTAMP),
(4, 9, 85.00, 74.00, 79.50, 3.00, 11, CURRENT_TIMESTAMP),
(5, 10, 92.00, 85.00, 87.80, 3.70, 2, CURRENT_TIMESTAMP),
(6, 11, 61.00, 56.00, 58.00, 0.00, 3, CURRENT_TIMESTAMP),
(7, 12, 68.00, 67.00, 67.40, 1.70, 4, CURRENT_TIMESTAMP),
(8, 13, 75.00, 78.00, 76.80, 2.70, 5, CURRENT_TIMESTAMP),
(9, 14, 82.00, 89.00, 85.50, 3.70, 6, CURRENT_TIMESTAMP),
(10, 15, 89.00, 60.00, 71.60, 2.00, 7, CURRENT_TIMESTAMP),
(11, 21, 93.00, 86.00, 88.80, 3.70, 3, CURRENT_TIMESTAMP),
(12, 22, 62.00, 57.00, 60.00, 1.00, 4, CURRENT_TIMESTAMP),
(13, 23, 69.00, 68.00, 68.30, 2.00, 5, CURRENT_TIMESTAMP),
(14, 24, 76.00, 79.00, 76.90, 2.70, 6, CURRENT_TIMESTAMP),
(15, 25, 83.00, 90.00, 86.50, 3.70, 7, CURRENT_TIMESTAMP),
(16, 26, 90.00, 61.00, 69.70, 2.00, 8, CURRENT_TIMESTAMP),
(17, 27, 97.00, 72.00, 84.50, 3.30, 9, CURRENT_TIMESTAMP),
(18, 28, 66.00, 83.00, 77.90, 2.70, 10, CURRENT_TIMESTAMP),
(19, 29, 73.00, 94.00, 79.30, 3.00, 11, CURRENT_TIMESTAMP),
(20, 30, 80.00, 65.00, 72.50, 2.30, 2, CURRENT_TIMESTAMP),
(21, 36, 84.00, 91.00, 88.20, 3.70, 8, CURRENT_TIMESTAMP),
(22, 37, 91.00, 62.00, 73.60, 2.30, 9, CURRENT_TIMESTAMP),
(23, 38, 60.00, 73.00, 66.50, 1.70, 10, CURRENT_TIMESTAMP),
(24, 39, 67.00, 84.00, 77.20, 2.70, 11, CURRENT_TIMESTAMP),
(25, 40, 74.00, 55.00, 62.60, 1.00, 2, CURRENT_TIMESTAMP),
(26, 41, 81.00, 66.00, 72.00, 2.30, 3, CURRENT_TIMESTAMP),
(27, 42, 88.00, 77.00, 81.40, 3.00, 4, CURRENT_TIMESTAMP),
(28, 43, 95.00, 88.00, 91.50, 4.00, 5, CURRENT_TIMESTAMP),
(29, 44, 64.00, 59.00, 61.00, 1.00, 6, CURRENT_TIMESTAMP),
(30, 45, 71.00, 70.00, 70.40, 2.00, 7, CURRENT_TIMESTAMP),
(31, 51, 75.00, 56.00, 67.40, 1.70, 3, CURRENT_TIMESTAMP),
(32, 52, 82.00, 67.00, 71.50, 2.00, 4, CURRENT_TIMESTAMP),
(33, 53, 89.00, 78.00, 85.70, 3.70, 5, CURRENT_TIMESTAMP),
(34, 54, 96.00, 89.00, 92.50, 4.00, 6, CURRENT_TIMESTAMP),
(35, 55, 65.00, 60.00, 62.00, 1.00, 7, CURRENT_TIMESTAMP),
(36, 56, 72.00, 71.00, 71.50, 2.00, 8, CURRENT_TIMESTAMP),
(37, 57, 79.00, 82.00, 81.10, 3.00, 9, CURRENT_TIMESTAMP),
(38, 58, 86.00, 93.00, 88.10, 3.70, 10, CURRENT_TIMESTAMP),
(39, 59, 93.00, 64.00, 78.50, 3.00, 11, CURRENT_TIMESTAMP),
(40, 60, 62.00, 75.00, 71.10, 2.00, 2, CURRENT_TIMESTAMP),
(41, 66, 66.00, 61.00, 63.00, 1.00, 8, CURRENT_TIMESTAMP),
(42, 67, 73.00, 72.00, 72.50, 2.30, 9, CURRENT_TIMESTAMP),
(43, 68, 80.00, 83.00, 81.80, 3.00, 10, CURRENT_TIMESTAMP),
(44, 69, 87.00, 94.00, 91.20, 4.00, 11, CURRENT_TIMESTAMP),
(45, 70, 94.00, 65.00, 76.60, 2.70, 2, CURRENT_TIMESTAMP),
(46, 71, 63.00, 76.00, 70.80, 2.00, 3, CURRENT_TIMESTAMP),
(47, 72, 70.00, 87.00, 78.50, 3.00, 4, CURRENT_TIMESTAMP),
(48, 73, 77.00, 58.00, 65.60, 1.50, 5, CURRENT_TIMESTAMP),
(49, 74, 84.00, 69.00, 75.00, 2.70, 6, CURRENT_TIMESTAMP),
(50, 75, 91.00, 80.00, 84.40, 3.30, 7, CURRENT_TIMESTAMP),
(51, 81, 95.00, 66.00, 74.70, 2.30, 3, CURRENT_TIMESTAMP),
(52, 82, 64.00, 77.00, 67.90, 1.70, 4, CURRENT_TIMESTAMP),
(53, 83, 71.00, 88.00, 79.50, 3.00, 5, CURRENT_TIMESTAMP),
(54, 84, 78.00, 59.00, 66.60, 1.70, 6, CURRENT_TIMESTAMP),
(55, 85, 85.00, 70.00, 79.00, 3.00, 7, CURRENT_TIMESTAMP),
(56, 86, 92.00, 81.00, 84.30, 3.30, 8, CURRENT_TIMESTAMP),
(57, 87, 61.00, 92.00, 70.30, 2.00, 9, CURRENT_TIMESTAMP),
(58, 88, 68.00, 63.00, 65.50, 1.50, 10, CURRENT_TIMESTAMP),
(59, 89, 75.00, 74.00, 74.30, 2.30, 11, CURRENT_TIMESTAMP),
(60, 90, 82.00, 85.00, 83.50, 3.30, 2, CURRENT_TIMESTAMP);

INSERT INTO grades(id, enrollment_id, usual_score, exam_score, final_score, grade_point, updated_by, updated_at) VALUES
(61, 96, 86.00, 71.00, 78.50, 3.00, 8, CURRENT_TIMESTAMP),
(62, 97, 93.00, 82.00, 86.40, 3.70, 9, CURRENT_TIMESTAMP),
(63, 98, 62.00, 93.00, 80.60, 3.00, 10, CURRENT_TIMESTAMP),
(64, 99, 69.00, 64.00, 66.00, 1.70, 11, CURRENT_TIMESTAMP),
(65, 100, 76.00, 75.00, 75.40, 2.70, 2, CURRENT_TIMESTAMP),
(66, 101, 83.00, 86.00, 84.50, 3.30, 3, CURRENT_TIMESTAMP),
(67, 102, 90.00, 57.00, 70.20, 2.00, 4, CURRENT_TIMESTAMP),
(68, 103, 97.00, 68.00, 79.60, 3.00, 5, CURRENT_TIMESTAMP),
(69, 104, 66.00, 79.00, 73.80, 2.30, 6, CURRENT_TIMESTAMP),
(70, 105, 73.00, 90.00, 83.20, 3.30, 7, CURRENT_TIMESTAMP),
(71, 111, 77.00, 76.00, 76.70, 2.70, 3, CURRENT_TIMESTAMP),
(72, 112, 84.00, 87.00, 85.50, 3.70, 4, CURRENT_TIMESTAMP),
(73, 113, 91.00, 58.00, 71.20, 2.00, 5, CURRENT_TIMESTAMP),
(74, 114, 60.00, 69.00, 63.60, 1.00, 6, CURRENT_TIMESTAMP),
(75, 115, 67.00, 80.00, 76.10, 2.70, 7, CURRENT_TIMESTAMP),
(76, 116, 74.00, 91.00, 79.10, 3.00, 8, CURRENT_TIMESTAMP),
(77, 117, 81.00, 62.00, 71.50, 2.00, 9, CURRENT_TIMESTAMP),
(78, 118, 88.00, 73.00, 77.50, 2.70, 10, CURRENT_TIMESTAMP),
(79, 119, 95.00, 84.00, 89.50, 3.70, 11, CURRENT_TIMESTAMP),
(80, 120, 64.00, 55.00, 57.70, 0.00, 2, CURRENT_TIMESTAMP),
(81, 126, 68.00, 81.00, 75.80, 2.70, 8, CURRENT_TIMESTAMP),
(82, 127, 75.00, 92.00, 85.20, 3.70, 9, CURRENT_TIMESTAMP),
(83, 128, 82.00, 63.00, 70.60, 2.00, 10, CURRENT_TIMESTAMP),
(84, 129, 89.00, 74.00, 80.00, 3.00, 11, CURRENT_TIMESTAMP),
(85, 130, 96.00, 85.00, 90.50, 4.00, 2, CURRENT_TIMESTAMP),
(86, 131, 65.00, 56.00, 59.60, 0.00, 3, CURRENT_TIMESTAMP),
(87, 132, 72.00, 67.00, 69.00, 2.00, 4, CURRENT_TIMESTAMP),
(88, 133, 79.00, 78.00, 78.40, 3.00, 5, CURRENT_TIMESTAMP),
(89, 134, 86.00, 89.00, 87.80, 3.70, 6, CURRENT_TIMESTAMP),
(90, 135, 93.00, 60.00, 76.50, 2.70, 7, CURRENT_TIMESTAMP),
(91, 141, 97.00, 86.00, 91.50, 4.00, 3, CURRENT_TIMESTAMP),
(92, 142, 66.00, 57.00, 60.60, 1.00, 4, CURRENT_TIMESTAMP),
(93, 143, 73.00, 68.00, 71.00, 2.00, 5, CURRENT_TIMESTAMP),
(94, 144, 80.00, 79.00, 79.30, 3.00, 6, CURRENT_TIMESTAMP),
(95, 145, 87.00, 90.00, 87.90, 3.70, 7, CURRENT_TIMESTAMP),
(96, 146, 94.00, 61.00, 77.50, 2.70, 8, CURRENT_TIMESTAMP),
(97, 147, 63.00, 72.00, 69.30, 2.00, 9, CURRENT_TIMESTAMP),
(98, 148, 70.00, 83.00, 76.50, 2.70, 10, CURRENT_TIMESTAMP),
(99, 149, 77.00, 94.00, 88.90, 3.70, 11, CURRENT_TIMESTAMP),
(100, 150, 84.00, 65.00, 78.30, 3.00, 2, CURRENT_TIMESTAMP),
(101, 156, 88.00, 91.00, 89.80, 3.70, 8, CURRENT_TIMESTAMP),
(102, 157, 95.00, 62.00, 75.20, 2.70, 9, CURRENT_TIMESTAMP),
(103, 158, 64.00, 73.00, 69.40, 2.00, 10, CURRENT_TIMESTAMP),
(104, 159, 71.00, 84.00, 77.50, 2.70, 11, CURRENT_TIMESTAMP),
(105, 160, 78.00, 55.00, 64.20, 1.50, 2, CURRENT_TIMESTAMP),
(106, 161, 85.00, 66.00, 73.60, 2.30, 3, CURRENT_TIMESTAMP),
(107, 162, 92.00, 77.00, 83.00, 3.30, 4, CURRENT_TIMESTAMP),
(108, 163, 61.00, 88.00, 77.20, 2.70, 5, CURRENT_TIMESTAMP),
(109, 164, 68.00, 59.00, 63.50, 1.00, 6, CURRENT_TIMESTAMP),
(110, 165, 75.00, 70.00, 72.00, 2.30, 7, CURRENT_TIMESTAMP),
(111, 171, 79.00, 56.00, 65.20, 1.50, 3, CURRENT_TIMESTAMP),
(112, 172, 86.00, 67.00, 78.40, 3.00, 4, CURRENT_TIMESTAMP),
(113, 173, 93.00, 78.00, 82.50, 3.30, 5, CURRENT_TIMESTAMP),
(114, 174, 62.00, 89.00, 70.10, 2.00, 6, CURRENT_TIMESTAMP),
(115, 175, 69.00, 60.00, 64.50, 1.50, 7, CURRENT_TIMESTAMP),
(116, 176, 76.00, 71.00, 72.50, 2.30, 8, CURRENT_TIMESTAMP),
(117, 177, 83.00, 82.00, 82.50, 3.30, 9, CURRENT_TIMESTAMP),
(118, 178, 90.00, 93.00, 92.10, 4.00, 10, CURRENT_TIMESTAMP),
(119, 179, 97.00, 64.00, 87.10, 3.70, 11, CURRENT_TIMESTAMP),
(120, 180, 66.00, 75.00, 70.50, 2.00, 2, CURRENT_TIMESTAMP);

INSERT INTO grades(id, enrollment_id, usual_score, exam_score, final_score, grade_point, updated_by, updated_at) VALUES
(121, 186, 70.00, 61.00, 64.60, 1.50, 8, CURRENT_TIMESTAMP),
(122, 187, 77.00, 72.00, 74.00, 2.30, 9, CURRENT_TIMESTAMP),
(123, 188, 84.00, 83.00, 83.50, 3.30, 10, CURRENT_TIMESTAMP),
(124, 189, 91.00, 94.00, 92.80, 4.00, 11, CURRENT_TIMESTAMP),
(125, 190, 60.00, 65.00, 63.00, 1.00, 2, CURRENT_TIMESTAMP),
(126, 191, 67.00, 76.00, 72.40, 2.30, 3, CURRENT_TIMESTAMP),
(127, 192, 74.00, 87.00, 81.80, 3.00, 4, CURRENT_TIMESTAMP),
(128, 193, 81.00, 58.00, 69.50, 2.00, 5, CURRENT_TIMESTAMP),
(129, 194, 88.00, 69.00, 76.60, 2.70, 6, CURRENT_TIMESTAMP),
(130, 195, 95.00, 80.00, 86.00, 3.70, 7, CURRENT_TIMESTAMP),
(131, 201, 61.00, 66.00, 63.00, 1.00, 3, CURRENT_TIMESTAMP),
(132, 202, 68.00, 77.00, 74.30, 2.30, 4, CURRENT_TIMESTAMP),
(133, 203, 75.00, 88.00, 78.90, 3.00, 5, CURRENT_TIMESTAMP),
(134, 204, 82.00, 59.00, 70.50, 2.00, 6, CURRENT_TIMESTAMP),
(135, 205, 89.00, 70.00, 77.60, 2.70, 7, CURRENT_TIMESTAMP),
(136, 206, 96.00, 81.00, 88.50, 3.70, 8, CURRENT_TIMESTAMP),
(137, 207, 65.00, 92.00, 83.90, 3.30, 9, CURRENT_TIMESTAMP),
(138, 208, 72.00, 63.00, 69.30, 2.00, 10, CURRENT_TIMESTAMP),
(139, 209, 79.00, 74.00, 76.50, 2.70, 11, CURRENT_TIMESTAMP),
(140, 210, 86.00, 85.00, 85.30, 3.70, 2, CURRENT_TIMESTAMP),
(141, 216, 90.00, 71.00, 78.60, 3.00, 8, CURRENT_TIMESTAMP),
(142, 217, 97.00, 82.00, 89.50, 3.70, 9, CURRENT_TIMESTAMP),
(143, 218, 66.00, 93.00, 82.20, 3.30, 10, CURRENT_TIMESTAMP),
(144, 219, 73.00, 64.00, 67.60, 1.70, 11, CURRENT_TIMESTAMP),
(145, 220, 80.00, 75.00, 77.00, 2.70, 2, CURRENT_TIMESTAMP),
(146, 221, 87.00, 86.00, 86.40, 3.70, 3, CURRENT_TIMESTAMP),
(147, 222, 94.00, 57.00, 75.50, 2.70, 4, CURRENT_TIMESTAMP),
(148, 223, 63.00, 68.00, 66.00, 1.70, 5, CURRENT_TIMESTAMP),
(149, 224, 70.00, 79.00, 75.40, 2.70, 6, CURRENT_TIMESTAMP),
(150, 225, 77.00, 90.00, 84.80, 3.30, 7, CURRENT_TIMESTAMP),
(151, 231, 81.00, 76.00, 77.50, 2.70, 3, CURRENT_TIMESTAMP),
(152, 232, 88.00, 87.00, 87.70, 3.70, 4, CURRENT_TIMESTAMP),
(153, 233, 95.00, 58.00, 76.50, 2.70, 5, CURRENT_TIMESTAMP),
(154, 234, 64.00, 69.00, 67.00, 1.70, 6, CURRENT_TIMESTAMP),
(155, 235, 71.00, 80.00, 74.60, 2.30, 7, CURRENT_TIMESTAMP),
(156, 236, 78.00, 91.00, 87.10, 3.70, 8, CURRENT_TIMESTAMP),
(157, 237, 85.00, 62.00, 78.10, 3.00, 9, CURRENT_TIMESTAMP),
(158, 238, 92.00, 73.00, 82.50, 3.30, 10, CURRENT_TIMESTAMP),
(159, 239, 61.00, 84.00, 77.10, 2.70, 11, CURRENT_TIMESTAMP),
(160, 240, 68.00, 55.00, 61.50, 1.00, 2, CURRENT_TIMESTAMP),
(161, 246, 72.00, 81.00, 76.50, 2.70, 8, CURRENT_TIMESTAMP),
(162, 247, 79.00, 92.00, 86.80, 3.70, 9, CURRENT_TIMESTAMP),
(163, 248, 86.00, 63.00, 72.20, 2.30, 10, CURRENT_TIMESTAMP),
(164, 249, 93.00, 74.00, 81.60, 3.00, 11, CURRENT_TIMESTAMP),
(165, 250, 62.00, 85.00, 75.80, 2.70, 2, CURRENT_TIMESTAMP),
(166, 251, 69.00, 56.00, 62.50, 1.00, 3, CURRENT_TIMESTAMP),
(167, 252, 76.00, 67.00, 70.60, 2.00, 4, CURRENT_TIMESTAMP),
(168, 253, 83.00, 78.00, 80.00, 3.00, 5, CURRENT_TIMESTAMP),
(169, 254, 90.00, 89.00, 89.40, 3.70, 6, CURRENT_TIMESTAMP),
(170, 255, 97.00, 60.00, 74.80, 2.30, 7, CURRENT_TIMESTAMP),
(171, 261, 63.00, 86.00, 69.90, 2.00, 3, CURRENT_TIMESTAMP),
(172, 262, 70.00, 57.00, 63.50, 1.00, 4, CURRENT_TIMESTAMP),
(173, 263, 77.00, 68.00, 71.60, 2.00, 5, CURRENT_TIMESTAMP),
(174, 264, 84.00, 79.00, 82.00, 3.30, 6, CURRENT_TIMESTAMP),
(175, 265, 91.00, 90.00, 90.30, 4.00, 7, CURRENT_TIMESTAMP),
(176, 266, 60.00, 61.00, 60.30, 1.00, 8, CURRENT_TIMESTAMP),
(177, 267, 67.00, 72.00, 69.50, 2.00, 9, CURRENT_TIMESTAMP),
(178, 268, 74.00, 83.00, 80.30, 3.00, 10, CURRENT_TIMESTAMP),
(179, 269, 81.00, 94.00, 87.50, 3.70, 11, CURRENT_TIMESTAMP),
(180, 270, 88.00, 65.00, 71.90, 2.00, 2, CURRENT_TIMESTAMP);

INSERT INTO grades(id, enrollment_id, usual_score, exam_score, final_score, grade_point, updated_by, updated_at) VALUES
(181, 276, 92.00, 91.00, 91.40, 4.00, 8, CURRENT_TIMESTAMP),
(182, 277, 61.00, 62.00, 61.60, 1.00, 9, CURRENT_TIMESTAMP),
(183, 278, 68.00, 73.00, 71.00, 2.00, 10, CURRENT_TIMESTAMP),
(184, 279, 75.00, 84.00, 80.40, 3.00, 11, CURRENT_TIMESTAMP),
(185, 280, 82.00, 55.00, 68.50, 2.00, 2, CURRENT_TIMESTAMP),
(186, 281, 89.00, 66.00, 75.20, 2.70, 3, CURRENT_TIMESTAMP),
(187, 282, 96.00, 77.00, 84.60, 3.30, 4, CURRENT_TIMESTAMP),
(188, 283, 65.00, 88.00, 78.80, 3.00, 5, CURRENT_TIMESTAMP),
(189, 284, 72.00, 59.00, 64.20, 1.50, 6, CURRENT_TIMESTAMP),
(190, 285, 79.00, 70.00, 74.50, 2.30, 7, CURRENT_TIMESTAMP),
(191, 291, 83.00, 56.00, 69.50, 2.00, 3, CURRENT_TIMESTAMP),
(192, 292, 90.00, 67.00, 76.20, 2.70, 4, CURRENT_TIMESTAMP),
(193, 293, 97.00, 78.00, 89.40, 3.70, 5, CURRENT_TIMESTAMP),
(194, 294, 66.00, 89.00, 82.10, 3.30, 6, CURRENT_TIMESTAMP),
(195, 295, 73.00, 60.00, 69.10, 2.00, 7, CURRENT_TIMESTAMP),
(196, 296, 80.00, 71.00, 75.50, 2.70, 8, CURRENT_TIMESTAMP),
(197, 297, 87.00, 82.00, 83.50, 3.30, 9, CURRENT_TIMESTAMP),
(198, 298, 94.00, 93.00, 93.50, 4.00, 10, CURRENT_TIMESTAMP),
(199, 299, 63.00, 64.00, 63.70, 1.00, 11, CURRENT_TIMESTAMP),
(200, 300, 70.00, 75.00, 71.50, 2.00, 2, CURRENT_TIMESTAMP);

INSERT INTO notices(id, title, content, audience, created_by, created_at) VALUES
(1, '本学期选课开放', '请同学们在规定时间内完成选课，教师及时维护课程成绩。', 'all', 1, CURRENT_TIMESTAMP),
(2, '成绩录入提醒', '任课教师需在选课结束后完成平时成绩和考试成绩录入。', 'teacher', 1, CURRENT_TIMESTAMP),
(3, '课堂资料通知', '请选课学生提前下载课程资料并按时参加课堂学习。', 'student', 1, CURRENT_TIMESTAMP),
(4, '五一假期教学安排调整', '假期前后课程按学校校历执行，调停课信息以教务系统通知为准。', 'all', 1, CURRENT_TIMESTAMP),
(5, '课程容量复核通知', '请各学院于本周五前复核热门课程容量。', 'teacher', 1, CURRENT_TIMESTAMP),
(6, '毕业班学分核验', '请毕业班同学查看已修学分与未通过课程情况。', 'student', 1, CURRENT_TIMESTAMP),
(7, '大学英语口语测试安排', '大学英语 II 口语测试安排在第 10 周。', 'student', 1, CURRENT_TIMESTAMP),
(8, '网络安全基础补充实验', '网络安全基础将增加一次综合攻防实验。', 'student', 1, CURRENT_TIMESTAMP),
(9, '教师成绩提交窗口', '成绩提交窗口将在选课关闭后开启。', 'teacher', 1, CURRENT_TIMESTAMP),
(10, '交互设计工作坊', '交互设计课程将举办作品点评工作坊。', 'student', 1, CURRENT_TIMESTAMP),
(11, '体育课程场馆调整', '大学体育部分班级临时调整至东区体育馆。', 'student', 1, CURRENT_TIMESTAMP),
(12, '课程信息维护提醒', '请核对教师、地点、学分与容量。', 'teacher', 1, CURRENT_TIMESTAMP),
(13, '高等数学辅导答疑', '高等数学 A 将在每周三晚开放集中答疑。', 'student', 1, CURRENT_TIMESTAMP),
(14, '通信原理实验安全要求', '请通信原理选课学生按实验室安全规范完成签到。', 'student', 1, CURRENT_TIMESTAMP),
(15, '管理学原理案例赛', '管理学原理课程将开展企业案例分析赛。', 'student', 1, CURRENT_TIMESTAMP),
(16, '思政课程实践周通知', '思想道德与法治实践周安排已发布。', 'student', 1, CURRENT_TIMESTAMP),
(17, '学生信息核验', '请各学院辅导员协助学生核对个人基础信息。', 'all', 1, CURRENT_TIMESTAMP),
(18, '数据库维护窗口', '系统将于周日 23:00-24:00 进行维护。', 'all', 1, CURRENT_TIMESTAMP),
(19, '课程表导出功能上线', '学生已选课程页新增周课表视图。', 'student', 1, CURRENT_TIMESTAMP),
(20, '选课冲突处理说明', '系统已启用上课时间冲突校验，冲突课程不可同时选择。', 'all', 1, CURRENT_TIMESTAMP);

DELIMITER $$
CREATE TRIGGER trg_notices_admin_insert
BEFORE INSERT ON notices
FOR EACH ROW
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM users u
      JOIN roles r ON r.id = u.role_id
     WHERE u.id = NEW.created_by
       AND r.code = 'admin'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only administrators can publish notices';
  END IF;
END$$

CREATE TRIGGER trg_notices_admin_update
BEFORE UPDATE ON notices
FOR EACH ROW
BEGIN
  IF NOT EXISTS (
    SELECT 1
      FROM users u
      JOIN roles r ON r.id = u.role_id
     WHERE u.id = NEW.created_by
       AND r.code = 'admin'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Only administrators can publish notices';
  END IF;
END$$
DELIMITER ;
