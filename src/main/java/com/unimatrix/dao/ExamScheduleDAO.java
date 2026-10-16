package com.unimatrix.dao;

import com.unimatrix.model.ExamSchedule;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExamScheduleDAO {

    // Fetch all generated exam schedules
    public List<ExamSchedule> getAllSchedules() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String query = "SELECT id, course_id, room_id, block_section_id, faculty_id, exam_date, time_slot FROM exam_schedules";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Date sqlDate = rs.getDate("exam_date");
                LocalDate examDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                schedules.add(new ExamSchedule(
                    rs.getInt("id"),
                    rs.getInt("course_id"),
                    rs.getInt("room_id"),
                    rs.getInt("block_section_id"),
                    rs.getInt("faculty_id"),
                    examDate,
                    rs.getString("time_slot")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching exam schedules:");
            e.printStackTrace();
        }
        return schedules;
    }

    // Save a generated exam schedule slot
    public boolean saveSchedule(ExamSchedule schedule) {
        String query = "INSERT INTO exam_schedules (course_id, room_id, block_section_id, faculty_id, exam_date, time_slot) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, schedule.getCourseId());
            pstmt.setInt(2, schedule.getRoomId());
            pstmt.setInt(3, schedule.getBlockSectionId());
            pstmt.setInt(4, schedule.getFacultyId());
            pstmt.setDate(5, Date.valueOf(schedule.getExamDate()));
            pstmt.setString(6, schedule.getTimeSlot());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving exam schedule:");
            e.printStackTrace();
            return false;
        }
    }

    // Clear all existing schedules before running the scheduling algorithm again
    public boolean clearAllSchedules() {
        String query = "DELETE FROM exam_schedules";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(query);
            return true;
        } catch (SQLException e) {
            System.err.println("Error clearing schedules table:");
            e.printStackTrace();
            return false;
        }
    }
}