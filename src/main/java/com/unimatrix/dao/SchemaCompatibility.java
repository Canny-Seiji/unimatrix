package com.unimatrix.dao;
import java.sql.*;
public final class SchemaCompatibility {
    public static void check()throws SQLException {
        try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement()){
            try(ResultSet r=s.executeQuery("SELECT MAX(version) FROM schema_version")){if(!r.next()||r.getInt(1)!=3)throw new SQLException("Database version 3 is required. Apply migration 003 after 002.");}
            s.executeQuery("SELECT folder_id,name,examination_period,academic_year,semester,revision,published,deleted FROM schedule_files LIMIT 0").close();try(ResultSet r=s.executeQuery("SELECT id FROM scheduler_write_lock WHERE id=1")){if(!r.next())throw new SQLException("Missing scheduler lock row.");}
            s.executeQuery("SELECT parent_id,name,deleted FROM schedule_folders LIMIT 0").close();
            s.executeQuery("SELECT schedule_id,course_id,room_id,block_section_id,faculty_id,exam_date,time_slot,start_time,end_time FROM exam_schedules LIMIT 0").close();
            s.executeQuery("SELECT faculty_name,department FROM faculty LIMIT 0").close();
            s.executeQuery("SELECT course_code,course_title,duration_hours FROM courses LIMIT 0").close();
            s.executeQuery("SELECT room_name,building,capacity FROM rooms LIMIT 0").close();
            s.executeQuery("SELECT section_name,student_count FROM block_sections LIMIT 0").close();
            s.executeQuery("SELECT faculty_id,weekday,exception_date,available,start_time,end_time FROM faculty_availability LIMIT 0").close();
            s.executeQuery("SELECT exam_date,reason FROM excluded_dates LIMIT 0").close();
            s.executeQuery("SELECT snapshot FROM schedule_history LIMIT 0").close();
        }catch(SQLException ex){throw new SQLException("Database compatibility check failed. Verify the connection and apply database/003_planning.sql after migration 002. Details: "+ex.getMessage(),ex);}
    }
}