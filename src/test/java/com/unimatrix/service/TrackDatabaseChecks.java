package com.unimatrix.service;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Opt-in checks against an EMPTY dedicated MySQL database. No existing tables are dropped. */
public final class TrackDatabaseChecks {
    private static int checks;
    public static void main(String[] args) throws Exception {
        String url=System.getenv("UNIMATRIX_TRACK_TEST_DB_URL");
        if (url==null || !url.matches("jdbc:mysql://[^/]+/unimatrix_track_test(?:\\?.*)?"))
            throw new IllegalArgumentException("Use an empty database named unimatrix_track_test via UNIMATRIX_TRACK_TEST_DB_URL.");
        System.setProperty("unimatrix.db.url",url);
        System.setProperty("unimatrix.db.user",System.getenv().getOrDefault("UNIMATRIX_TRACK_TEST_DB_USER","root"));
        System.setProperty("unimatrix.db.password",System.getenv().getOrDefault("UNIMATRIX_TRACK_TEST_DB_PASSWORD",""));
        checks=0;
        try (Connection c=DatabaseConnection.getConnection(); Statement s=c.createStatement()) {
            try (ResultSet r=s.executeQuery("SHOW TABLES")) {
                if (r.next()) throw new IllegalStateException("Database must be empty. No existing tables were changed.");
            }
            script(s,Path.of("database/001_full_setup_v3.sql"));
            s.executeUpdate("INSERT INTO courses(id,course_code,course_title,duration_hours,track) VALUES "
                    +"(1,'CS1','Programming',2,'CSSE, 1st term'),(2,'SEC1','Security',2,'Cybersecurity, 2nd term'),"
                    +"(3,'LEG1','Legacy',1,'Custom legacy'),(4,'GEN1','General education',1,'General')");
            s.executeUpdate("INSERT INTO block_sections(id,section_name,student_count,track) VALUES "
                    +"(1,'TN03',30,'CSSE, 1st term'),(2,'TN04',20,'Custom legacy')");
            s.executeUpdate("INSERT INTO rooms(id,room_name,building,capacity) VALUES (1,'R1','A',40)");
            s.executeUpdate("INSERT INTO faculty(id,faculty_name,department) VALUES (1,'Faculty','Computing')");
            s.executeUpdate("INSERT INTO schedule_files(id,name,academic_year,semester) VALUES (1,'Test','2026-2027','Term 1')");
            s.executeUpdate("INSERT INTO exam_schedules(course_id,room_id,block_section_id,faculty_id,schedule_id,exam_date,time_slot,start_time,end_time) "
                    +"VALUES (1,1,1,1,1,'2026-11-17','08:00 - 10:00','08:00','10:00')");
            script(s,Path.of("database/004_track_terms.sql"));
            script(s,Path.of("database/004_track_terms.sql")); // Successful reruns preserve the result.
            check(scalar(s,"SELECT MAX(version) FROM schema_version")==4,"schema upgraded");
            check(scalar(s,"SELECT COUNT(*) FROM courses")==4,"courses preserved");
            check(scalar(s,"SELECT COUNT(*) FROM block_sections")==2,"sections preserved");
            check(scalar(s,"SELECT COUNT(*) FROM exam_schedules")==1,"schedule rows preserved");
            check(scalar(s,"SELECT COUNT(*) FROM track_terms WHERE label='Cybersecurity, 2nd term' AND track_name='Cybersecurity' AND term_number=2")==1,"custom term migrated");
            check(scalar(s,"SELECT COUNT(*) FROM track_terms WHERE label='Custom legacy' AND term_number=0")==1,"bare legacy track preserved");
            rejectSql(()->s.executeUpdate("UPDATE courses SET track_term_id=999999 WHERE id=1"),"course FK enforced");
            rejectSql(()->s.executeUpdate("UPDATE block_sections SET track_term_id=999999 WHERE id=1"),"section FK enforced");
            rejectSql(()->s.executeUpdate("DELETE FROM track_terms WHERE id=5"),"referenced track protected");
            rejectSql(()->s.executeUpdate("INSERT INTO track_terms(label,track_name,term_number) VALUES ('Wrong','ECE',2)"),"catalog label and term agree");
        }
        SchemaCompatibility.check();checks++;
        check(new CourseDAO().getAllCourses().get(0).getTrack().equals("CSSE, 1st term"),"course DAO retains track");
        check(new BlockSectionDAO().getAllBlockSections().get(0).getTrack().equals("CSSE, 1st term"),"section DAO retains track");
        check(new CourseDAO().addCourse(new Course(0,"ECE1","Circuits",2,"ECE, 2nd term")),"legacy course insert writes FK");
        check(new BlockSectionDAO().addBlockSection(new BlockSection(0,"ECE A",20,"ECE, 2nd term")),"legacy section insert writes FK");
        EntityDataDAO dao=new EntityDataDAO();
        var before=dao.load(EntityDataDAO.Kind.COURSE);
        var mismatch=new ArrayList<>(before);
        mismatch.set(0,new EntityDataDAO.Row(1,List.of("CS1","Programming","CSSE, 2nd term")));
        rejectArgument(()->dao.save(EntityDataDAO.Kind.COURSE,before,mismatch),"active schedule blocks wrong course term");
        check(dao.load(EntityDataDAO.Kind.COURSE).equals(before),"course batch rolls back");
        var blocks=dao.load(EntityDataDAO.Kind.BLOCK);
        var wrongTrack=new ArrayList<>(blocks);
        wrongTrack.set(0,new EntityDataDAO.Row(1,List.of("TN03","30","CPE, 1st term")));
        rejectArgument(()->dao.save(EntityDataDAO.Kind.BLOCK,blocks,wrongTrack),"active schedule blocks wrong section track");
        check(dao.load(EntityDataDAO.Kind.BLOCK).equals(blocks),"section batch rolls back");
        var withNew=new ArrayList<>(before);
        withNew.add(new EntityDataDAO.Row(0,List.of("CPE1","Computer engineering","CPE, 3rd term")));
        var saved=dao.save(EntityDataDAO.Kind.COURSE,before,withNew);
        check(saved.stream().anyMatch(row->row.values().equals(withNew.get(withNew.size()-1).values())),"grid insert round trip");
        var unknown=new ArrayList<>(saved);
        unknown.add(new EntityDataDAO.Row(0,List.of("BAD","Unknown","Typo, 1st term")));
        rejectArgument(()->dao.save(EntityDataDAO.Kind.COURSE,saved,unknown),"unknown dropdown label rejected");
        check(dao.load(EntityDataDAO.Kind.COURSE).equals(saved),"unknown label insert rolls back");
        try (Connection c=DatabaseConnection.getConnection()) {
            ScheduleEditorService.validateStored(c,1);checks++;
        }
        System.out.println("MySQL track checks passed: "+checks);
    }

    private static int scalar(Statement s,String sql) throws SQLException {
        try (ResultSet r=s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }
    /** Understand Workbench DELIMITER directives without sending them to the JDBC server. */
    private static void script(Statement s,Path path) throws Exception {
        String delimiter=";";
        StringBuilder sql=new StringBuilder();
        for (String line:Files.readAllLines(path)) {
            String trimmed=line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
            if (trimmed.startsWith("DELIMITER ")) {
                if (!sql.toString().isBlank()) throw new IllegalStateException("Incomplete SQL before DELIMITER");
                delimiter=trimmed.substring("DELIMITER ".length()).trim();continue;
            }
            sql.append(line).append('\n');
            if (trimmed.endsWith(delimiter)) {
                String statement=sql.toString().stripTrailing();
                statement=statement.substring(0,statement.length()-delimiter.length()).trim();
                // Stay in the explicitly selected test database.
                if (!statement.startsWith("CREATE DATABASE") && !statement.startsWith("USE ")) s.execute(statement);
                sql.setLength(0);
            }
        }
        if (!sql.toString().isBlank()) throw new IllegalStateException("Unterminated SQL in "+path);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void rejectSql(Action action,String message) throws Exception {
        try { action.run(); throw new AssertionError(message); }
        catch (SQLException expected) { checks++; }
    }
    private static void rejectArgument(Action action,String message) throws Exception {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void check(boolean result,String message) {
        if (!result) throw new AssertionError(message);
        checks++;
    }
}
