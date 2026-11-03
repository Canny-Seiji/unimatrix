package com.unimatrix.dao;

import com.unimatrix.model.ExamSchedule;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExamScheduleDAO {

    private static final String SCHEDULE_COLUMNS =
            "e.id, e.course_id, e.room_id, e.block_section_id, e.faculty_id, e.schedule_id, e.exam_date, e.time_slot, " +
            "c.course_code, r.room_name, b.section_name, f.full_name AS faculty_name ";

    private static final String SCHEDULE_JOINS =
            "FROM exam_schedules e " +
            "LEFT JOIN courses c ON e.course_id = c.id " +
            "LEFT JOIN rooms r ON e.room_id = r.id " +
            "LEFT JOIN block_sections b ON e.block_section_id = b.id " +
            "LEFT JOIN faculty f ON e.faculty_id = f.id ";

    public List<ExamSchedule> getAllSchedules() {
        return querySchedules("SELECT " + SCHEDULE_COLUMNS + SCHEDULE_JOINS, List.of());
    }

    public List<ExamSchedule> getSchedulesByFile(int scheduleId) {
        return getSchedulesByFiles(List.of(scheduleId));
    }

    public List<ExamSchedule> getSchedulesByFiles(Collection<Integer> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) return List.of();

        List<Integer> ids = new ArrayList<>(scheduleIds);
        String placeholders = String.join(", ", java.util.Collections.nCopies(ids.size(), "?"));
        String query = "SELECT " + SCHEDULE_COLUMNS + SCHEDULE_JOINS +
                "WHERE e.schedule_id IN (" + placeholders + ")";
        return querySchedules(query, ids);
    }

    private List<ExamSchedule> querySchedules(String query, List<Integer> scheduleIds) {
        List<ExamSchedule> schedules = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < scheduleIds.size(); i++) stmt.setInt(i + 1, scheduleIds.get(i));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date sqlDate = rs.getDate("exam_date");
                    LocalDate examDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                    ExamSchedule schedule = new ExamSchedule(
                        rs.getInt("id"),
                        rs.getInt("course_id"),
                        rs.getInt("room_id"),
                        rs.getInt("block_section_id"),
                        rs.getInt("faculty_id"),
                        examDate,
                        rs.getString("time_slot"),
                        rs.getString("course_code"),
                        rs.getString("room_name"),
                        rs.getString("section_name"),
                        rs.getString("faculty_name")
                    );
                    schedule.setScheduleId(rs.getInt("schedule_id"));
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching exam schedules:");
            e.printStackTrace();
        }
        return schedules;
    }

    public List<String> findConflicts(int scheduleId, ExamSchedule schedule) throws SQLException {
        if (schedule.getExamDate() == null || schedule.getTimeSlot() == null) return List.of();

        String query = "SELECT room_id, block_section_id, faculty_id FROM exam_schedules " +
                "WHERE schedule_id = ? AND exam_date = ? AND time_slot = ?";
        Set<String> conflicts = new LinkedHashSet<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, scheduleId);
            stmt.setDate(2, Date.valueOf(schedule.getExamDate()));
            stmt.setString(3, schedule.getTimeSlot());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getInt("room_id") == schedule.getRoomId()) {
                        conflicts.add("Room is already booked at this date and time.");
                    }
                    if (rs.getInt("block_section_id") == schedule.getBlockSectionId()) {
                        conflicts.add("Block section already has an exam at this date and time.");
                    }
                    if (rs.getInt("faculty_id") == schedule.getFacultyId()) {
                        conflicts.add("Faculty member already has an exam at this date and time.");
                    }
                }
            }
        }
        return new ArrayList<>(conflicts);
    }

    public boolean saveSchedule(ExamSchedule schedule) {
        String query = "INSERT INTO exam_schedules " +
                "(course_id, room_id, block_section_id, faculty_id, schedule_id, exam_date, time_slot) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, schedule.getCourseId());
            pstmt.setInt(2, schedule.getRoomId());
            pstmt.setInt(3, schedule.getBlockSectionId());
            pstmt.setInt(4, schedule.getFacultyId());
            pstmt.setInt(5, schedule.getScheduleId());
            pstmt.setDate(6, schedule.getExamDate() != null ? Date.valueOf(schedule.getExamDate()) : null);
            pstmt.setString(7, schedule.getTimeSlot());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving exam schedule:");
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveAll(List<ExamSchedule> schedules, int scheduleId) {
        if (schedules.isEmpty()) return true;

        String query = "INSERT INTO exam_schedules " +
                "(course_id, room_id, block_section_id, faculty_id, schedule_id, exam_date, time_slot) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            conn.setAutoCommit(false);
            try {
                for (ExamSchedule schedule : schedules) {
                    pstmt.setInt(1, schedule.getCourseId());
                    pstmt.setInt(2, schedule.getRoomId());
                    pstmt.setInt(3, schedule.getBlockSectionId());
                    pstmt.setInt(4, schedule.getFacultyId());
                    pstmt.setInt(5, scheduleId);
                    pstmt.setDate(6, schedule.getExamDate() != null ? Date.valueOf(schedule.getExamDate()) : null);
                    pstmt.setString(7, schedule.getTimeSlot());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error saving exam schedules:");
            e.printStackTrace();
            return false;
        }
    }

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