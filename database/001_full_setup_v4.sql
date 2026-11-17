-- Version 4: run ONLY for a blank database. Existing v3 databases use 004_track_terms.sql.
-- Requires MySQL 8.0.16+ (enforced CHECK constraints).
CREATE DATABASE IF NOT EXISTS unimatrix_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE unimatrix_db;

CREATE TABLE faculty (
    id INT NOT NULL AUTO_INCREMENT,
    faculty_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE rooms (
    id INT NOT NULL AUTO_INCREMENT,
    room_name VARCHAR(50) NOT NULL,
    building VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE track_terms (
    id INT NOT NULL AUTO_INCREMENT,
    label VARCHAR(50) NOT NULL,
    track_name VARCHAR(50) NOT NULL,
    term_number TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_track_terms_label (label),
    CONSTRAINT chk_track_term_number CHECK (term_number BETWEEN 0 AND 3),
    CONSTRAINT chk_track_term_label CHECK (
        label = CASE term_number
            WHEN 0 THEN track_name
            WHEN 1 THEN CONCAT(track_name, ', 1st term')
            WHEN 2 THEN CONCAT(track_name, ', 2nd term')
            WHEN 3 THEN CONCAT(track_name, ', 3rd term')
        END
    )
) ENGINE=InnoDB;

INSERT INTO track_terms (id,label,track_name,term_number) VALUES
    (1,'General','General',0),
    (2,'CPE, 1st term','CPE',1),
    (3,'CPE, 2nd term','CPE',2),
    (4,'CPE, 3rd term','CPE',3),
    (5,'CSSE, 1st term','CSSE',1),
    (6,'CSSE, 2nd term','CSSE',2),
    (7,'CSSE, 3rd term','CSSE',3),
    (8,'ECE, 1st term','ECE',1),
    (9,'ECE, 2nd term','ECE',2),
    (10,'ECE, 3rd term','ECE',3),
    (11,'General, 1st term','General',1),
    (12,'General, 2nd term','General',2),
    (13,'General, 3rd term','General',3);

CREATE TABLE courses (
    id INT NOT NULL AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL,
    course_title VARCHAR(100) NOT NULL,
    duration_hours FLOAT NOT NULL DEFAULT 0,
    track_term_id INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_track_term FOREIGN KEY (track_term_id)
        REFERENCES track_terms(id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE block_sections (
    id INT NOT NULL AUTO_INCREMENT,
    section_name VARCHAR(50) NOT NULL,
    student_count INT NOT NULL,
    track_term_id INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT fk_blocks_track_term FOREIGN KEY (track_term_id)
        REFERENCES track_terms(id) ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE schedule_folders (
    id INT NOT NULL AUTO_INCREMENT,
    parent_id INT DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    KEY parent_id (parent_id),
    CONSTRAINT schedule_folders_ibfk_1
        FOREIGN KEY (parent_id)
        REFERENCES schedule_folders(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE schedule_files (
    id INT NOT NULL AUTO_INCREMENT,
    folder_id INT DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    examination_period VARCHAR(30) NOT NULL DEFAULT 'Other',
    academic_year VARCHAR(20) NOT NULL DEFAULT '',
    semester VARCHAR(30) NOT NULL DEFAULT '',
    revision BIGINT NOT NULL DEFAULT 0,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY folder_id (folder_id),
    CONSTRAINT schedule_files_ibfk_1
        FOREIGN KEY (folder_id)
        REFERENCES schedule_folders(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE exam_schedules (
    id INT NOT NULL AUTO_INCREMENT,
    course_id INT DEFAULT NULL,
    room_id INT DEFAULT NULL,
    block_section_id INT DEFAULT NULL,
    faculty_id INT DEFAULT NULL,
    exam_date DATE DEFAULT NULL,
    time_slot VARCHAR(50) DEFAULT NULL,
    start_time TIME DEFAULT NULL,
    end_time TIME DEFAULT NULL,
    schedule_id INT DEFAULT NULL,
    PRIMARY KEY (id),
    KEY course_id (course_id),
    KEY room_id (room_id),
    KEY block_section_id (block_section_id),
    KEY faculty_id (faculty_id),
    KEY fk_exam_schedule_file (schedule_id),
    KEY ix_exam_scope_date (schedule_id, exam_date),

    CONSTRAINT exam_schedules_ibfk_1
        FOREIGN KEY (course_id) REFERENCES courses(id),

    CONSTRAINT exam_schedules_ibfk_2
        FOREIGN KEY (room_id) REFERENCES rooms(id),

    CONSTRAINT exam_schedules_ibfk_3
        FOREIGN KEY (block_section_id) REFERENCES block_sections(id),

    CONSTRAINT exam_schedules_ibfk_4
        FOREIGN KEY (faculty_id) REFERENCES faculty(id),

    CONSTRAINT fk_exam_schedule_file
        FOREIGN KEY (schedule_id)
        REFERENCES schedule_files(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE scheduler_write_lock (
    id INT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO scheduler_write_lock (id)
VALUES (1);

CREATE TABLE faculty_availability (
    id INT AUTO_INCREMENT PRIMARY KEY,
    faculty_id INT NOT NULL,
    weekday INT NULL,
    exception_date DATE NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    FOREIGN KEY (faculty_id)
        REFERENCES faculty(id),

    CHECK (
        (weekday BETWEEN 1 AND 6 AND exception_date IS NULL)
        OR
        (weekday IS NULL AND exception_date IS NOT NULL)
    ),

    CHECK (end_time > start_time)
) ENGINE=InnoDB;

CREATE TABLE excluded_dates (
    exam_date DATE PRIMARY KEY,
    reason VARCHAR(200) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE schedule_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    schedule_id INT NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(100) NOT NULL,
    snapshot LONGBLOB NOT NULL,

    FOREIGN KEY (schedule_id)
        REFERENCES schedule_files(id)
) ENGINE=InnoDB;

CREATE TABLE schema_version (
    version INT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO schema_version (version)
VALUES (4);
