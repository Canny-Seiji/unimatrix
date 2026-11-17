-- Upgrade the supplied version-3 database without deleting courses, sections or schedules.
-- Back up your database and close UniMatrix before running this file in MySQL Workbench.
-- MySQL DDL commits separately, so keep that backup until verification finishes.
USE unimatrix_db;

DELIMITER $$
DROP PROCEDURE IF EXISTS migrate_track_terms_v4$$
CREATE PROCEDURE migrate_track_terms_v4()
main: BEGIN
    DECLARE current_version INT;
    SELECT MAX(version) INTO current_version FROM schema_version;
    IF current_version = 4 THEN
        LEAVE main;
    END IF;
    IF current_version IS NULL OR current_version <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Version 3 is required before migration 004.';
    END IF;

    CREATE TABLE IF NOT EXISTS track_terms (
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

    INSERT IGNORE INTO track_terms (id,label,track_name,term_number) VALUES
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

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='track_term_id') THEN
        ALTER TABLE courses ADD COLUMN track_term_id INT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='track') THEN
        -- Preserve every old label. Recognized suffixes are split into track and term;
        -- bare legacy names use term 0 because their real term is unknown.
        INSERT INTO track_terms (label,track_name,term_number)
        SELECT DISTINCT TRIM(e.track),
            CASE WHEN TRIM(e.track) REGEXP ', (1st|2nd|3rd) term$'
                THEN TRIM(LEFT(TRIM(e.track),CHAR_LENGTH(TRIM(e.track))-10)) ELSE TRIM(e.track) END,
            CASE WHEN TRIM(e.track) REGEXP ', (1st|2nd|3rd) term$'
                THEN CAST(SUBSTRING(TRIM(e.track),CHAR_LENGTH(TRIM(e.track))-7,1) AS UNSIGNED) ELSE 0 END
        FROM courses e LEFT JOIN track_terms t ON t.label=TRIM(e.track)
        WHERE e.track IS NOT NULL AND TRIM(e.track)<>'' AND t.id IS NULL;
        UPDATE courses e JOIN track_terms t
            ON t.label=COALESCE(NULLIF(TRIM(e.track),''),'General')
            SET e.track_term_id=t.id WHERE e.track_term_id IS NULL;
    ELSE
        -- A v3 database from before track columns existed keeps its original General behavior.
        UPDATE courses SET track_term_id=1 WHERE track_term_id IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM courses WHERE track_term_id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unmapped track labels. Restore the backup and review legacy tracks.';
    END IF;
    ALTER TABLE courses MODIFY track_term_id INT NOT NULL DEFAULT 1;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage
            WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='track_term_id'
                AND referenced_table_name='track_terms' AND referenced_column_name='id') THEN
        ALTER TABLE courses ADD CONSTRAINT fk_courses_track_term
            FOREIGN KEY (track_term_id) REFERENCES track_terms(id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='block_sections' AND column_name='track_term_id') THEN
        ALTER TABLE block_sections ADD COLUMN track_term_id INT NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='block_sections' AND column_name='track') THEN
        -- Preserve every old label. Recognized suffixes are split into track and term;
        -- bare legacy names use term 0 because their real term is unknown.
        INSERT INTO track_terms (label,track_name,term_number)
        SELECT DISTINCT TRIM(e.track),
            CASE WHEN TRIM(e.track) REGEXP ', (1st|2nd|3rd) term$'
                THEN TRIM(LEFT(TRIM(e.track),CHAR_LENGTH(TRIM(e.track))-10)) ELSE TRIM(e.track) END,
            CASE WHEN TRIM(e.track) REGEXP ', (1st|2nd|3rd) term$'
                THEN CAST(SUBSTRING(TRIM(e.track),CHAR_LENGTH(TRIM(e.track))-7,1) AS UNSIGNED) ELSE 0 END
        FROM block_sections e LEFT JOIN track_terms t ON t.label=TRIM(e.track)
        WHERE e.track IS NOT NULL AND TRIM(e.track)<>'' AND t.id IS NULL;
        UPDATE block_sections e JOIN track_terms t
            ON t.label=COALESCE(NULLIF(TRIM(e.track),''),'General')
            SET e.track_term_id=t.id WHERE e.track_term_id IS NULL;
    ELSE
        -- A v3 database from before track columns existed keeps its original General behavior.
        UPDATE block_sections SET track_term_id=1 WHERE track_term_id IS NULL;
    END IF;
    IF EXISTS (SELECT 1 FROM block_sections WHERE track_term_id IS NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unmapped track labels. Restore the backup and review legacy tracks.';
    END IF;
    ALTER TABLE block_sections MODIFY track_term_id INT NOT NULL DEFAULT 1;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage
            WHERE table_schema=DATABASE() AND table_name='block_sections' AND column_name='track_term_id'
                AND referenced_table_name='track_terms' AND referenced_column_name='id') THEN
        ALTER TABLE block_sections ADD CONSTRAINT fk_blocks_track_term
            FOREIGN KEY (track_term_id) REFERENCES track_terms(id)
            ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='courses' AND column_name='track') THEN
        ALTER TABLE courses DROP COLUMN track;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='block_sections' AND column_name='track') THEN
        ALTER TABLE block_sections DROP COLUMN track;
    END IF;
    INSERT INTO schema_version (version) VALUES (4);
END$$
CALL migrate_track_terms_v4()$$
DROP PROCEDURE migrate_track_terms_v4$$
DELIMITER ;

-- Both results should show readable labels and all your original records.
SELECT c.id,c.course_code,t.label AS track FROM courses c JOIN track_terms t ON t.id=c.track_term_id;
SELECT b.id,b.section_name,t.label AS track FROM block_sections b JOIN track_terms t ON t.id=b.track_term_id;
