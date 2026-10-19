-- ============================================================
--  University Course Management System (UCMS) - MySQL Schema
--  Based on the project ER diagram.
--
--  Run this ONCE on the machine that hosts MySQL:
--      mysql -u root -p < schema.sql
--  or open it in MySQL Workbench and press the lightning icon.
--
--  WARNING: the first statement drops any old copy of ucms_db so the
--  tables always match the Java code. Do not run it on a database
--  whose data you want to keep.
-- ============================================================

DROP DATABASE IF EXISTS ucms_db;
CREATE DATABASE ucms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ucms_db;

-- 1. Department
CREATE TABLE department (
    dept_id    INT AUTO_INCREMENT PRIMARY KEY,
    dept_name  VARCHAR(100) NOT NULL,
    dept_code  VARCHAR(10)  NOT NULL UNIQUE
);

-- 2. Course
CREATE TABLE course (
    course_id   INT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(15)  NOT NULL UNIQUE,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    dept_id     INT NOT NULL,
    CONSTRAINT fk_course_dept FOREIGN KEY (dept_id) REFERENCES department(dept_id)
);

-- 3. Course Component (Theory / Lab) - a course is split into components
CREATE TABLE course_component (
    component_id    INT AUTO_INCREMENT PRIMARY KEY,
    course_id       INT NOT NULL,
    component_type  ENUM('Theory','Lab') NOT NULL,
    credits         DECIMAL(3,1) NOT NULL,
    evaluation_type VARCHAR(60),
    CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES course(course_id) ON DELETE CASCADE
);

-- 4. Prerequisite (course -> course that must be passed first)
CREATE TABLE prerequisite (
    prereq_id          INT AUTO_INCREMENT PRIMARY KEY,
    course_id          INT NOT NULL,
    required_course_id INT NOT NULL,
    min_grade          VARCHAR(2) NOT NULL DEFAULT 'D',
    is_mandatory       TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_pre_course   FOREIGN KEY (course_id)          REFERENCES course(course_id) ON DELETE CASCADE,
    CONSTRAINT fk_pre_required FOREIGN KEY (required_course_id) REFERENCES course(course_id) ON DELETE CASCADE,
    CONSTRAINT uq_prereq UNIQUE (course_id, required_course_id)
);

-- 5. Semester
CREATE TABLE semester (
    semester_id     INT AUTO_INCREMENT PRIMARY KEY,
    semester_number VARCHAR(20) NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    instruction_end DATE        NOT NULL,
    drop_deadline   DATE        NOT NULL,
    is_current      TINYINT(1)  NOT NULL DEFAULT 0
);

-- 6. Classroom
CREATE TABLE classroom (
    room_id       INT AUTO_INCREMENT PRIMARY KEY,
    room_number   VARCHAR(15) NOT NULL UNIQUE,
    room_type     ENUM('Theory','Lab') NOT NULL,
    room_capacity INT         NOT NULL
);

-- 7. Faculty (Person subtype)
CREATE TABLE faculty (
    faculty_id  INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(120) NOT NULL UNIQUE,
    phone       VARCHAR(15),
    designation VARCHAR(60),
    dept_id     INT NOT NULL,
    CONSTRAINT fk_faculty_dept FOREIGN KEY (dept_id) REFERENCES department(dept_id)
);

-- 8. Student (Person subtype) - contact details are verified before enrolling
CREATE TABLE student (
    student_id          INT AUTO_INCREMENT PRIMARY KEY,
    prn                 VARCHAR(20)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    email               VARCHAR(120) NOT NULL UNIQUE,
    phone               VARCHAR(15),
    address             VARCHAR(200),
    year_of_study       INT NOT NULL DEFAULT 1,
    dept_id             INT,
    contact_verified_at TIMESTAMP NULL DEFAULT NULL,
    CONSTRAINT fk_student_dept FOREIGN KEY (dept_id) REFERENCES department(dept_id)
);

-- 9. Section (a scheduled offering of one course component)
CREATE TABLE section (
    section_id     INT AUTO_INCREMENT PRIMARY KEY,
    course_id      INT         NOT NULL,
    component_id   INT         NOT NULL,
    faculty_id     INT         NOT NULL,
    room_id        INT         NOT NULL,
    semester_id    INT         NOT NULL,
    section_number VARCHAR(5)  NOT NULL,
    max_capacity   INT         NOT NULL DEFAULT 30,
    slot           VARCHAR(50) NOT NULL,   -- e.g. "Mon/Wed 09:00-10:30"
    CONSTRAINT fk_sec_course   FOREIGN KEY (course_id)    REFERENCES course(course_id),
    CONSTRAINT fk_sec_comp     FOREIGN KEY (component_id) REFERENCES course_component(component_id),
    CONSTRAINT fk_sec_faculty  FOREIGN KEY (faculty_id)   REFERENCES faculty(faculty_id),
    CONSTRAINT fk_sec_room     FOREIGN KEY (room_id)      REFERENCES classroom(room_id),
    CONSTRAINT fk_sec_semester FOREIGN KEY (semester_id)  REFERENCES semester(semester_id)
);

-- 10. Enrollment (student <-> section)
CREATE TABLE enrollment (
    enrollment_id   INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT        NOT NULL,
    section_id      INT        NOT NULL,
    status          ENUM('Enrolled','Dropped','Completed') NOT NULL DEFAULT 'Enrolled',
    grade           VARCHAR(2) DEFAULT NULL,
    enrollment_date DATE       NOT NULL,
    is_overridden   TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_enr_section FOREIGN KEY (section_id) REFERENCES section(section_id),
    CONSTRAINT uq_enr UNIQUE (student_id, section_id)
);

-- 11. Waitlist (used when a section is full)
CREATE TABLE waitlist (
    waitlist_id     INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT  NOT NULL,
    section_id      INT  NOT NULL,
    queue_position  INT  NOT NULL,
    waitlist_status ENUM('Waiting','Promoted','Removed') NOT NULL DEFAULT 'Waiting',
    waitlist_date   DATE NOT NULL,
    CONSTRAINT fk_wl_student FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_wl_section FOREIGN KEY (section_id) REFERENCES section(section_id),
    CONSTRAINT uq_wl UNIQUE (student_id, section_id)
);

-- 12. Attendance (one row per enrollment per class date)
CREATE TABLE attendance (
    attendance_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT  NOT NULL,
    class_date    DATE NOT NULL,
    status        ENUM('Present','Absent','Late') NOT NULL DEFAULT 'Present',
    CONSTRAINT fk_att_enr FOREIGN KEY (enrollment_id) REFERENCES enrollment(enrollment_id) ON DELETE CASCADE,
    CONSTRAINT uq_att UNIQUE (enrollment_id, class_date)
);

-- 13. Prerequisite Task - something a student must finish before enrolling:
--     an Assignment to submit or a basic Video to watch.
--     applies_to says whether it is needed for the Theory sections, the Lab sections, or Both.
CREATE TABLE prerequisite_task (
    task_id     INT AUTO_INCREMENT PRIMARY KEY,
    course_id   INT          NOT NULL,
    task_type   ENUM('Assignment','Video') NOT NULL,
    applies_to  ENUM('Both','Theory','Lab') NOT NULL DEFAULT 'Both',
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    video_url   VARCHAR(300),
    CONSTRAINT fk_task_course FOREIGN KEY (course_id) REFERENCES course(course_id) ON DELETE CASCADE
);

-- 14. Task Submission - an assignment answer (reviewed by faculty) or a "video watched" record
CREATE TABLE task_submission (
    submission_id INT AUTO_INCREMENT PRIMARY KEY,
    task_id       INT       NOT NULL,
    student_id    INT       NOT NULL,
    content       TEXT,
    submitted_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        ENUM('Submitted','Approved','Rejected','Watched') NOT NULL DEFAULT 'Submitted',
    remarks       VARCHAR(255),
    CONSTRAINT fk_sub_task    FOREIGN KEY (task_id)    REFERENCES prerequisite_task(task_id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_student FOREIGN KEY (student_id) REFERENCES student(student_id) ON DELETE CASCADE,
    CONSTRAINT uq_sub UNIQUE (task_id, student_id)
);

-- 15. User Account (login credentials; role_ref_id -> student/faculty id)
CREATE TABLE user_account (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(120) NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,   -- SHA-256 hex
    role          ENUM('STUDENT','FACULTY','ADMIN') NOT NULL,
    role_ref_id   INT          NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 16. Audit Log (grade changes)
CREATE TABLE audit_log (
    audit_id    INT AUTO_INCREMENT PRIMARY KEY,
    changed_by  VARCHAR(120) NOT NULL,
    student_prn VARCHAR(20)  NOT NULL,
    section_key VARCHAR(40)  NOT NULL,
    old_grade   VARCHAR(2)   DEFAULT NULL,
    new_grade   VARCHAR(2)   NOT NULL,
    changed_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 17. Notification (waitlist promotion, grade posted, registration result, ...)
CREATE TABLE notification (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT          NOT NULL,
    message         VARCHAR(300) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read         TINYINT(1)   NOT NULL DEFAULT 0,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES user_account(user_id) ON DELETE CASCADE
);

-- 18. System Setting (configurable values, e.g. the probation CGPA threshold)
CREATE TABLE system_setting (
    setting_key   VARCHAR(50)  PRIMARY KEY,
    setting_value VARCHAR(100) NOT NULL
);

-- ============================================================
--  STORED FUNCTIONS - semester GPA (SGPA) and cumulative GPA (CGPA)
--  Called from Java with a JDBC CallableStatement.
-- ============================================================
DELIMITER $$

CREATE FUNCTION fn_grade_points(g VARCHAR(2)) RETURNS INT
    DETERMINISTIC NO SQL
BEGIN
    RETURN CASE g WHEN 'A+' THEN 10 WHEN 'A' THEN 9 WHEN 'B+' THEN 8 WHEN 'B' THEN 7
                  WHEN 'C+' THEN 6 WHEN 'C' THEN 5 WHEN 'D' THEN 4 ELSE 0 END;
END$$

-- Credit-weighted GPA of one student's completed courses in one semester
CREATE FUNCTION fn_sgpa(p_student INT, p_semester INT) RETURNS DECIMAL(4,2)
    READS SQL DATA
BEGIN
    DECLARE result DECIMAL(4,2);
    SELECT ROUND(SUM(fn_grade_points(e.grade) * cc.credits) / SUM(cc.credits), 2) INTO result
      FROM enrollment e
      JOIN section sec          ON sec.section_id = e.section_id
      JOIN course_component cc  ON cc.component_id = sec.component_id
     WHERE e.student_id = p_student AND sec.semester_id = p_semester
       AND e.status = 'Completed' AND e.grade IS NOT NULL;
    RETURN COALESCE(result, 0);
END$$

-- Credit-weighted GPA over all semesters
CREATE FUNCTION fn_cgpa(p_student INT) RETURNS DECIMAL(4,2)
    READS SQL DATA
BEGIN
    DECLARE result DECIMAL(4,2);
    SELECT ROUND(SUM(fn_grade_points(e.grade) * cc.credits) / SUM(cc.credits), 2) INTO result
      FROM enrollment e
      JOIN section sec          ON sec.section_id = e.section_id
      JOIN course_component cc  ON cc.component_id = sec.component_id
     WHERE e.student_id = p_student AND e.status = 'Completed' AND e.grade IS NOT NULL;
    RETURN COALESCE(result, 0);
END$$

-- ============================================================
--  TRIGGERS
-- ============================================================

-- Audit trail + "grade posted" notification whenever a grade is entered or changed.
-- The Java app sets @app_user to the logged-in user's email before updating.
CREATE TRIGGER trg_enrollment_grade AFTER UPDATE ON enrollment
FOR EACH ROW
BEGIN
    DECLARE v_key VARCHAR(40);
    DECLARE v_prn VARCHAR(20);
    IF NEW.grade IS NOT NULL AND NOT (OLD.grade <=> NEW.grade) THEN
        SELECT CONCAT(c.course_code, '-', cc.component_type, '-', sec.section_number) INTO v_key
          FROM section sec
          JOIN course c            ON c.course_id = sec.course_id
          JOIN course_component cc ON cc.component_id = sec.component_id
         WHERE sec.section_id = NEW.section_id;
        SELECT prn INTO v_prn FROM student WHERE student_id = NEW.student_id;
        INSERT INTO audit_log (changed_by, student_prn, section_key, old_grade, new_grade)
             VALUES (COALESCE(@app_user, CURRENT_USER()), v_prn, v_key, OLD.grade, NEW.grade);
        INSERT INTO notification (user_id, message)
             SELECT user_id, CONCAT('Grade posted: ', NEW.grade, ' in ', v_key)
               FROM user_account WHERE role = 'STUDENT' AND role_ref_id = NEW.student_id;
    END IF;
END$$

-- FR-09: a student may not hold two active enrollments in different sections of the
-- same course (same Theory/Lab component) within the same semester.
-- The Java service checks this first; these triggers are the last line of defence.
CREATE TRIGGER trg_fr09_insert BEFORE INSERT ON enrollment
FOR EACH ROW
BEGIN
    IF NEW.status = 'Enrolled' AND EXISTS (
        SELECT 1 FROM enrollment e
          JOIN section s_old ON s_old.section_id = e.section_id
          JOIN section s_new ON s_new.section_id = NEW.section_id
         WHERE e.student_id = NEW.student_id AND e.status = 'Enrolled'
           AND s_old.course_id = s_new.course_id AND s_old.component_id = s_new.component_id
           AND s_old.semester_id = s_new.semester_id AND e.section_id <> NEW.section_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FR-09: already enrolled in another section of this course';
    END IF;
END$$

CREATE TRIGGER trg_fr09_update BEFORE UPDATE ON enrollment
FOR EACH ROW
BEGIN
    IF NEW.status = 'Enrolled' AND OLD.status <> 'Enrolled' AND EXISTS (
        SELECT 1 FROM enrollment e
          JOIN section s_old ON s_old.section_id = e.section_id
          JOIN section s_new ON s_new.section_id = NEW.section_id
         WHERE e.student_id = NEW.student_id AND e.status = 'Enrolled'
           AND s_old.course_id = s_new.course_id AND s_old.component_id = s_new.component_id
           AND s_old.semester_id = s_new.semester_id AND e.section_id <> NEW.section_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FR-09: already enrolled in another section of this course';
    END IF;
END$$

-- Notify a student when the waitlist moves them into a section.
CREATE TRIGGER trg_waitlist_promoted AFTER UPDATE ON waitlist
FOR EACH ROW
BEGIN
    IF NEW.waitlist_status = 'Promoted' AND OLD.waitlist_status <> 'Promoted' THEN
        INSERT INTO notification (user_id, message)
             SELECT u.user_id, CONCAT('A seat opened! You were moved from the waitlist into ',
                    c.course_code, ' ', cc.component_type, ' section ', sec.section_number)
               FROM user_account u
               JOIN section sec          ON sec.section_id = NEW.section_id
               JOIN course c             ON c.course_id = sec.course_id
               JOIN course_component cc  ON cc.component_id = sec.component_id
              WHERE u.role = 'STUDENT' AND u.role_ref_id = NEW.student_id;
    END IF;
END$$

DELIMITER ;

-- ============================================================
--  SEED DATA
-- ============================================================

INSERT INTO department (dept_id, dept_name, dept_code) VALUES
    (1, 'Computer Science & Engineering', 'CSE'),
    (2, 'Information Technology',         'IT'),
    (3, 'Electronics & Communication',    'ECE');

INSERT INTO course (course_id, course_code, title, description, dept_id) VALUES
    (1, 'CS101', 'Programming Fundamentals',    'Intro to C/Java programming',        1),
    (2, 'CS201', 'Data Structures',             'Arrays, Lists, Trees, Graphs',       1),
    (3, 'CS301', 'Database Management Systems', 'SQL, normalization, transactions',   1),
    (4, 'CS401', 'Operating Systems',           'Process, memory, file management',   1),
    (5, 'CS501', 'Software Engineering',        'SDLC, Agile, design patterns',       1),
    (6, 'IT101', 'Web Technologies',            'HTML, CSS, JavaScript, PHP',         2),
    (7, 'IT201', 'Network Security',            'Cryptography, firewalls, protocols', 2);

INSERT INTO course_component (component_id, course_id, component_type, credits, evaluation_type) VALUES
    (1,  1, 'Theory', 3.0, 'Internal+External'),
    (2,  1, 'Lab',    1.0, 'Practical'),
    (3,  2, 'Theory', 3.0, 'Internal+External'),
    (4,  2, 'Lab',    1.0, 'Practical'),
    (5,  3, 'Theory', 3.0, 'Internal+External'),
    (6,  3, 'Lab',    1.0, 'Practical'),
    (7,  4, 'Theory', 3.0, 'Internal+External'),
    (8,  5, 'Theory', 3.0, 'Internal+External'),
    (9,  6, 'Theory', 3.0, 'Internal+External'),
    (10, 6, 'Lab',    1.0, 'Practical'),
    (11, 7, 'Theory', 3.0, 'Internal+External');

INSERT INTO prerequisite (course_id, required_course_id, min_grade, is_mandatory) VALUES
    (2, 1, 'D', 1),   -- Data Structures   <- Programming Fundamentals
    (3, 2, 'D', 1),   -- DBMS              <- Data Structures
    (4, 2, 'D', 1),   -- Operating Systems <- Data Structures
    (5, 3, 'C', 1),   -- Software Engg     <- DBMS (min grade C)
    (5, 4, 'D', 0),   -- Software Engg     <- OS (recommended only)
    (7, 6, 'D', 1);   -- Network Security  <- Web Technologies

INSERT INTO semester (semester_id, semester_number, start_date, end_date, instruction_end, drop_deadline, is_current) VALUES
    (1, 'SEM-I-2025',  '2025-07-01', '2025-11-30', '2025-11-15', '2025-08-31', 0),
    (2, 'SEM-II-2026', '2026-01-01', '2026-05-31', '2026-05-15', '2026-02-28', 0),
    (3, 'SEM-I-2026',  '2026-07-15', '2026-12-15', '2026-12-01', '2026-11-15', 1);

INSERT INTO classroom (room_id, room_number, room_type, room_capacity) VALUES
    (1, 'A-101', 'Theory', 60),
    (2, 'A-102', 'Theory', 60),
    (3, 'B-201', 'Lab',    30),
    (4, 'B-202', 'Lab',    30),
    (5, 'C-101', 'Theory', 45);

INSERT INTO faculty (faculty_id, name, email, phone, designation, dept_id) VALUES
    (1, 'Dr. Priya Sharma',  'priya.sharma@university.edu', '9822000001', 'Associate Professor', 1),
    (2, 'Prof. Rahul Mehta', 'rahul.mehta@university.edu',  '9822000002', 'Assistant Professor', 1),
    (3, 'Dr. Anita Joshi',   'anita.joshi@university.edu',  '9822000003', 'Professor',           2),
    (4, 'Dr. Vikram Rao',         'vikram.rao@university.edu',      '9822000004', 'Professor',           1),
    (5, 'Prof. Neha Kapoor',      'neha.kapoor@university.edu',     '9822000005', 'Assistant Professor', 2),
    (6, 'Dr. Arjun Nair',         'arjun.nair@university.edu',      '9822000006', 'Associate Professor', 3),
    (7, 'Prof. Kavita Deshmukh',  'kavita.deshmukh@university.edu', '9822000007', 'Assistant Professor', 1);

INSERT INTO student (student_id, prn, name, email, phone, address, year_of_study, dept_id, contact_verified_at) VALUES
    (1, '25070122259', 'Parth Vohra',    'parth.vohra@student.edu',    '9876543210', 'Hostel A, Pune',      2, 1, NULL),
    (2, '25070122260', 'Aditi Kulkarni', 'aditi.kulkarni@student.edu', '9876543211', 'Kothrud, Pune',       3, 1, '2026-07-10 10:00:00'),
    (3, '25070122261', 'Rohan Desai',    'rohan.desai@student.edu',    '9876543212', 'Baner, Pune',         1, 2, '2026-07-10 10:00:00'),
    (4, '25070122262', 'Sneha Patil',    'sneha.patil@student.edu',    '9876543213', 'Hadapsar, Pune',      3, 1, '2026-07-10 10:00:00'),
    (5, '25070122263', 'Karan Shah',     'karan.shah@student.edu',     '9876543214', 'Viman Nagar, Pune',   3, 1, '2026-07-10 10:00:00'),
    (6, '25070122264', 'Meera Iyer',     'meera.iyer@student.edu',     '9876543215', 'Aundh, Pune',         3, 1, NULL),
    (7,  '25070122265', 'Ananya Singh',     'ananya.singh@student.edu',     '9876543216', 'Kalyani Nagar, Pune', 1, 1, '2026-07-10 10:00:00'),
    (8,  '25070122266', 'Vivek Kumar',      'vivek.kumar@student.edu',      '9876543217', 'Wakad, Pune',         2, 1, '2026-07-10 10:00:00'),
    (9,  '25070122267', 'Ishita Mehra',     'ishita.mehra@student.edu',     '9876543218', 'Pimpri, Pune',        1, 2, '2026-07-10 10:00:00'),
    (10, '25070122268', 'Rahul Gupta',      'rahul.gupta@student.edu',      '9876543219', 'Shivajinagar, Pune',  2, 2, '2026-07-10 10:00:00'),
    (11, '25070122269', 'Pooja Nair',       'pooja.nair@student.edu',       '9876543220', 'Kharadi, Pune',       1, 3, '2026-07-10 10:00:00'),
    (12, '25070122270', 'Siddharth Joshi',  'siddharth.joshi@student.edu',  '9876543221', 'Bavdhan, Pune',       2, 1, '2026-07-10 10:00:00'),
    (13, '25070122271', 'Neha Reddy',       'neha.reddy@student.edu',       '9876543222', 'Magarpatta, Pune',    1, 1, NULL),
    (14, '25070122272', 'Arjun Malhotra',   'arjun.malhotra@student.edu',   '9876543223', 'Hinjewadi, Pune',     2, 1, '2026-07-10 10:00:00'),
    (15, '25070122273', 'Kavya Menon',      'kavya.menon@student.edu',      '9876543224', 'Camp, Pune',          1, 2, '2026-07-10 10:00:00'),
    (16, '25070122274', 'Yash Agarwal',     'yash.agarwal@student.edu',     '9876543225', 'Deccan, Pune',        2, 1, '2026-07-10 10:00:00'),
    (17, '25070122275', 'Tanvi Bhosale',    'tanvi.bhosale@student.edu',    '9876543226', 'Warje, Pune',         1, 3, '2026-07-10 10:00:00'),
    (18, '25070122276', 'Aman Verma',       'aman.verma@student.edu',       '9876543227', 'Katraj, Pune',        2, 2, '2026-07-10 10:00:00'),
    (19, '25070122277', 'Riya Chatterjee',  'riya.chatterjee@student.edu',  '9876543228', 'Sinhagad Road, Pune', 1, 1, '2026-07-10 10:00:00'),
    (20, '25070122278', 'Harsh Patel',      'harsh.patel@student.edu',      '9876543229', 'Yerawada, Pune',      2, 1, '2026-07-10 10:00:00');

-- Sections: 1-10, 14-17 = current semester (3); 11-13 = semester 2; 18-19 = semester 1
INSERT INTO section (section_id, course_id, component_id, faculty_id, room_id, semester_id, section_number, max_capacity, slot) VALUES
    (1,  1, 1,  1, 1, 3, 'A', 60, 'Mon/Wed 09:00-10:30'),
    (2,  1, 2,  1, 3, 3, 'A', 30, 'Tue 14:00-16:00'),
    (3,  2, 3,  2, 2, 3, 'A', 60, 'Tue/Thu 11:00-12:30'),
    (4,  2, 4,  2, 4, 3, 'A', 30, 'Wed 14:00-16:00'),
    (5,  3, 5,  1, 1, 3, 'A', 60, 'Mon/Wed 11:00-12:30'),
    (6,  3, 6,  1, 3, 3, 'A', 30, 'Thu 14:00-16:00'),
    (7,  6, 9,  3, 5, 3, 'A', 45, 'Tue/Thu 09:00-10:30'),
    (8,  4, 7,  2, 2, 3, 'A', 3,  'Fri 09:00-12:00'),     -- tiny capacity: waitlist demo
    (9,  7, 11, 3, 5, 3, 'A', 40, 'Mon/Fri 14:00-15:30'),
    (10, 5, 8,  2, 2, 3, 'A', 50, 'Sat 10:00-13:00'),
    (11, 1, 1,  1, 1, 2, 'A', 60, 'Mon/Wed 09:00-10:30'),
    (12, 6, 9,  3, 5, 2, 'A', 45, 'Tue/Thu 09:00-10:30'),
    (13, 2, 3,  2, 2, 2, 'A', 60, 'Tue/Thu 11:00-12:30'),
    (14, 1, 1,  7, 5, 3, 'B', 45, 'Tue/Thu 14:00-15:30'),
    (15, 2, 4,  4, 3, 3, 'B', 30, 'Fri 14:00-16:00'),
    (16, 6, 10, 5, 4, 3, 'A', 30, 'Mon 14:00-16:00'),
    (17, 2, 3,  4, 1, 3, 'B', 60, 'Tue/Thu 15:00-16:30'),
    (18, 1, 1,  1, 1, 1, 'A', 60, 'Mon/Wed 09:00-10:30'),
    (19, 6, 9,  3, 5, 1, 'A', 45, 'Tue/Thu 09:00-10:30');

INSERT INTO enrollment (enrollment_id, student_id, section_id, status, grade, enrollment_date, is_overridden) VALUES
    -- Parth: passed CS101 + IT101 last semester, now doing CS201
    (1,  1, 11, 'Completed', 'A',  '2026-01-05', 0),
    (2,  1, 12, 'Completed', 'B+', '2026-01-05', 0),
    (3,  1, 3,  'Enrolled',  NULL, '2026-07-16', 0),
    (4,  1, 4,  'Enrolled',  NULL, '2026-07-16', 0),
    -- Aditi
    (5,  2, 11, 'Completed', 'B',  '2026-01-05', 0),
    (6,  2, 13, 'Completed', 'B+', '2026-01-05', 0),
    (7,  2, 5,  'Enrolled',  NULL, '2026-07-16', 0),
    (8,  2, 6,  'Enrolled',  NULL, '2026-07-16', 0),
    (9,  2, 8,  'Enrolled',  NULL, '2026-07-16', 0),
    -- Rohan: first year
    (10, 3, 1,  'Enrolled',  NULL, '2026-07-16', 0),
    (11, 3, 7,  'Enrolled',  NULL, '2026-07-16', 0),
    -- Sneha
    (12, 4, 11, 'Completed', 'A',  '2026-01-05', 0),
    (13, 4, 13, 'Completed', 'A',  '2026-01-05', 0),
    (14, 4, 8,  'Enrolled',  NULL, '2026-07-16', 0),
    (15, 4, 5,  'Enrolled',  NULL, '2026-07-16', 0),
    -- Karan (fills the last CS401 seat)
    (16, 5, 11, 'Completed', 'C',  '2026-01-05', 0),
    (17, 5, 13, 'Completed', 'B',  '2026-01-05', 0),
    (18, 5, 8,  'Enrolled',  NULL, '2026-07-16', 0),
    -- Meera: eligible for CS401 but it is full -> waitlist demo
    (19, 6, 11, 'Completed', 'A+', '2026-01-05', 0),
    (20, 6, 13, 'Completed', 'A',  '2026-01-05', 0),
    -- First-year students: CS101 (section A or B) and IT101
    (21, 7, 1,  'Enrolled',  NULL, '2026-07-16', 0),
    (22, 7, 7,  'Enrolled',  NULL, '2026-07-16', 0),
    (23, 9, 1,  'Enrolled',  NULL, '2026-07-16', 0),
    (24, 9, 7,  'Enrolled',  NULL, '2026-07-16', 0),
    (25, 9, 16, 'Enrolled',  NULL, '2026-07-16', 0),
    (26, 11, 1, 'Enrolled',  NULL, '2026-07-16', 0),
    (27, 13, 14, 'Enrolled', NULL, '2026-07-16', 0),
    (28, 15, 14, 'Enrolled', NULL, '2026-07-16', 0),
    (29, 15, 7,  'Enrolled', NULL, '2026-07-16', 0),
    (30, 15, 16, 'Enrolled', NULL, '2026-07-16', 0),
    (31, 17, 14, 'Enrolled', NULL, '2026-07-16', 0),
    (32, 19, 14, 'Enrolled', NULL, '2026-07-16', 0),
    -- Second-year students: earlier results + CS201 now
    (33, 8, 11,  'Completed', 'B+', '2026-01-05', 0),
    (34, 8, 3,   'Enrolled',  NULL, '2026-07-16', 0),
    (35, 8, 4,   'Enrolled',  NULL, '2026-07-16', 0),
    (36, 10, 19, 'Completed', 'A',  '2025-07-05', 0),
    (37, 10, 11, 'Completed', 'B',  '2026-01-05', 0),
    (38, 10, 17, 'Enrolled',  NULL, '2026-07-16', 0),
    (39, 10, 15, 'Enrolled',  NULL, '2026-07-16', 0),
    (40, 12, 18, 'Completed', 'A+', '2025-07-05', 0),
    (41, 12, 13, 'Completed', 'A',  '2026-01-05', 0),
    (42, 12, 5,  'Enrolled',  NULL, '2026-07-16', 0),
    (43, 14, 11, 'Completed', 'C+', '2026-01-05', 0),
    (44, 14, 17, 'Enrolled',  NULL, '2026-07-16', 0),
    (45, 14, 4,  'Enrolled',  NULL, '2026-07-16', 0),
    -- Yash: D and F last semester -> CGPA 2.0 -> academic probation demo
    (46, 16, 11, 'Completed', 'D',  '2026-01-05', 0),
    (47, 16, 12, 'Completed', 'F',  '2026-01-05', 0),
    (48, 16, 3,  'Enrolled',  NULL, '2026-07-16', 0),
    (49, 18, 12, 'Completed', 'C',  '2026-01-05', 0),
    (50, 18, 18, 'Completed', 'C',  '2025-07-05', 0),
    (51, 18, 17, 'Enrolled',  NULL, '2026-07-16', 0),
    (52, 18, 15, 'Enrolled',  NULL, '2026-07-16', 0),
    (53, 20, 11, 'Completed', 'A',  '2026-01-05', 0),
    (54, 20, 12, 'Completed', 'A',  '2026-01-05', 0),
    (55, 20, 3,  'Enrolled',  NULL, '2026-07-16', 0),
    (56, 20, 4,  'Enrolled',  NULL, '2026-07-16', 0),
    (57, 20, 9,  'Enrolled',  NULL, '2026-07-16', 0);

INSERT INTO attendance (enrollment_id, class_date, status) VALUES
    (3, '2026-08-04', 'Present'), (3, '2026-08-06', 'Present'), (3, '2026-08-11', 'Late'),
    (3, '2026-08-13', 'Present'), (3, '2026-08-18', 'Absent'),  (3, '2026-08-20', 'Present'),
    (3, '2026-08-25', 'Present'), (3, '2026-08-27', 'Present'), (3, '2026-09-01', 'Present'),
    (3, '2026-09-03', 'Present'),
    (4, '2026-08-05', 'Present'), (4, '2026-08-12', 'Absent'),  (4, '2026-08-19', 'Absent'),
    (4, '2026-08-26', 'Present'), (4, '2026-09-02', 'Absent'),  (4, '2026-09-09', 'Present'),
    (7, '2026-08-03', 'Present'), (7, '2026-08-05', 'Present'), (7, '2026-08-10', 'Present'),
    (7, '2026-08-12', 'Late'),    (7, '2026-08-17', 'Present'), (7, '2026-08-19', 'Present'),
    (10, '2026-08-03', 'Present'), (10, '2026-08-05', 'Absent'), (10, '2026-08-10', 'Present'),
    (11, '2026-08-04', 'Present'), (11, '2026-08-06', 'Present'), (11, '2026-08-11', 'Present'),
    (21, '2026-08-03', 'Present'), (21, '2026-08-05', 'Present'), (21, '2026-08-10', 'Late'),
    (23, '2026-08-03', 'Present'), (23, '2026-08-05', 'Present'), (23, '2026-08-10', 'Present'),
    (26, '2026-08-03', 'Absent'),  (26, '2026-08-05', 'Present'), (26, '2026-08-10', 'Present'),
    (34, '2026-08-04', 'Present'), (34, '2026-08-06', 'Present'), (34, '2026-08-11', 'Present'),
    (48, '2026-08-04', 'Absent'),  (48, '2026-08-06', 'Absent'),  (48, '2026-08-11', 'Present'),
    (55, '2026-08-04', 'Present'), (55, '2026-08-06', 'Late'),    (55, '2026-08-11', 'Present');

-- Video links are YouTube searches so they always open; admin can replace them with exact videos.
INSERT INTO prerequisite_task (task_id, course_id, task_type, applies_to, title, description, video_url) VALUES
    (1, 3, 'Assignment', 'Both',   'ER Diagram Exercise',
        'Draw an ER diagram for a library system and describe each entity and relationship.', NULL),
    (2, 3, 'Video',      'Theory', 'SQL Basics in 15 Minutes',
        'Watch an introduction to SELECT, WHERE, JOIN and GROUP BY.',
        'https://www.youtube.com/results?search_query=sql+basics+tutorial'),
    (3, 3, 'Video',      'Lab',    'MySQL Workbench Setup',
        'Install MySQL Workbench and connect to a local server before the first lab.',
        'https://www.youtube.com/results?search_query=mysql+workbench+setup+tutorial'),
    (4, 4, 'Assignment', 'Both',   'Linux Shell Basics',
        'Explain what the commands ls, cd, grep, ps and kill do, with one example each.', NULL),
    (5, 7, 'Assignment', 'Both',   'Networking Fundamentals Essay',
        'In about 150 words, explain the difference between TCP and UDP and when to use each.', NULL),
    (6, 7, 'Video',      'Theory', 'Introduction to Cryptography',
        'Watch a beginner overview of symmetric vs public-key encryption.',
        'https://www.youtube.com/results?search_query=introduction+to+cryptography'),
    (7, 1, 'Video',      'Lab',    'Install the JDK and Run Your First Program',
        'Install Java and run Hello World before the first programming lab.',
        'https://www.youtube.com/results?search_query=install+jdk+hello+world+java');

INSERT INTO task_submission (task_id, student_id, content, status, remarks) VALUES
    (1, 2, 'Entities: Book, Member, Loan ...',          'Approved', 'Good work'),
    (1, 4, 'Entities: Book, Author, Member ...',        'Approved', NULL),
    (2, 2, NULL,                                         'Watched',  NULL),
    (3, 2, NULL,                                         'Watched',  NULL),
    (2, 4, NULL,                                         'Watched',  NULL),
    (4, 2, 'ls lists files, cd changes directory ...',  'Approved', NULL),
    (4, 4, 'ls lists directory contents ...',           'Approved', NULL),
    (4, 5, 'ls - list, cd - change dir ...',            'Submitted', NULL),
    (1, 12, 'Entities: Book, Member, Loan, Fine ...',    'Approved', NULL),
    (2, 12, NULL,                                         'Watched',  NULL),
    (5, 20, 'TCP guarantees delivery and ordering ...',  'Approved', NULL),
    (6, 20, NULL,                                         'Watched',  NULL);

INSERT INTO system_setting (setting_key, setting_value) VALUES
    ('probation_cgpa', '5.00');

-- Passwords: students = stud123, faculty = fac123, admin = admin123 (SHA-256 hex)
INSERT INTO user_account (email, password_hash, role, role_ref_id) VALUES
    ('parth.vohra@student.edu',     SHA2('stud123', 256),  'STUDENT', 1),
    ('aditi.kulkarni@student.edu',  SHA2('stud123', 256),  'STUDENT', 2),
    ('rohan.desai@student.edu',     SHA2('stud123', 256),  'STUDENT', 3),
    ('sneha.patil@student.edu',     SHA2('stud123', 256),  'STUDENT', 4),
    ('karan.shah@student.edu',      SHA2('stud123', 256),  'STUDENT', 5),
    ('meera.iyer@student.edu',      SHA2('stud123', 256),  'STUDENT', 6),
    ('priya.sharma@university.edu', SHA2('fac123',  256),  'FACULTY', 1),
    ('rahul.mehta@university.edu',  SHA2('fac123',  256),  'FACULTY', 2),
    ('anita.joshi@university.edu',  SHA2('fac123',  256),  'FACULTY', 3),
    ('vikram.rao@university.edu',      SHA2('fac123', 256), 'FACULTY', 4),
    ('neha.kapoor@university.edu',     SHA2('fac123', 256), 'FACULTY', 5),
    ('arjun.nair@university.edu',      SHA2('fac123', 256), 'FACULTY', 6),
    ('kavita.deshmukh@university.edu', SHA2('fac123', 256), 'FACULTY', 7),
    ('ananya.singh@student.edu',     SHA2('stud123', 256), 'STUDENT', 7),
    ('vivek.kumar@student.edu',      SHA2('stud123', 256), 'STUDENT', 8),
    ('ishita.mehra@student.edu',     SHA2('stud123', 256), 'STUDENT', 9),
    ('rahul.gupta@student.edu',      SHA2('stud123', 256), 'STUDENT', 10),
    ('pooja.nair@student.edu',       SHA2('stud123', 256), 'STUDENT', 11),
    ('siddharth.joshi@student.edu',  SHA2('stud123', 256), 'STUDENT', 12),
    ('neha.reddy@student.edu',       SHA2('stud123', 256), 'STUDENT', 13),
    ('arjun.malhotra@student.edu',   SHA2('stud123', 256), 'STUDENT', 14),
    ('kavya.menon@student.edu',      SHA2('stud123', 256), 'STUDENT', 15),
    ('yash.agarwal@student.edu',     SHA2('stud123', 256), 'STUDENT', 16),
    ('tanvi.bhosale@student.edu',    SHA2('stud123', 256), 'STUDENT', 17),
    ('aman.verma@student.edu',       SHA2('stud123', 256), 'STUDENT', 18),
    ('riya.chatterjee@student.edu',  SHA2('stud123', 256), 'STUDENT', 19),
    ('harsh.patel@student.edu',      SHA2('stud123', 256), 'STUDENT', 20),
    ('admin@ucms.edu',              SHA2('admin123', 256), 'ADMIN',   0);
