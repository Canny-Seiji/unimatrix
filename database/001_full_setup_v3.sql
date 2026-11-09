-- For blank database setup, run this script to create the database and all necessary tables.
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

CREATE TABLE courses (
    id INT NOT NULL AUTO_INCREMENT,
    course_code VARCHAR(20) NOT NULL,
    course_title VARCHAR(100) NOT NULL,
    duration_hours FLOAT NOT NULL DEFAULT 0,
    track VARCHAR(50) NOT NULL DEFAULT 'General',
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE block_sections (
    id INT NOT NULL AUTO_INCREMENT,
    section_name VARCHAR(50) NOT NULL,
    student_count INT NOT NULL,
    track VARCHAR(50) NOT NULL DEFAULT 'General',
    PRIMARY KEY (id)
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
VALUES (3);
