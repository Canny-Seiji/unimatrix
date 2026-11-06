USE unimatrix_db;

ALTER TABLE courses
    MODIFY duration_hours FLOAT NOT NULL DEFAULT 0;

ALTER TABLE schedule_files
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE schedule_folders
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

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
        (
            weekday BETWEEN 1 AND 6
            AND exception_date IS NULL
        )
        OR
        (
            weekday IS NULL
            AND exception_date IS NOT NULL
        )
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
);

INSERT INTO schema_version (version)
VALUES (3);