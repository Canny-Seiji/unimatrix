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
            "c.course_code, r.room_name, b.section_name, f.faculty_name AS faculty_name ";

    private static final String SCHEDULE_JOINS =
            "FROM exam_schedules e JOIN schedule_files sf ON sf.id=e.schedule_id AND sf.deleted=FALSE " +
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
            throw new IllegalStateException("Cannot load exam schedules: " + e.getMessage(), e);
        }
        return schedules;
    }

    public List<String> findConflicts(int scheduleId, ExamSchedule schedule) throws SQLException {
        Set<String> conflicts = new LinkedHashSet<>();
        for (ExamSchedule other : getSchedulesByFile(scheduleId)) {
            if (schedule.getId() > 0 && schedule.getId() == other.getId()) continue;
            if (other.getCourseId() == schedule.getCourseId() && other.getBlockSectionId() == schedule.getBlockSectionId())
                conflicts.add("Course-block exam already exists in this schedule.");
            if (java.util.Objects.equals(other.getExamDate(), schedule.getExamDate())
                    && com.unimatrix.service.ExamTimes.overlaps(other.getTimeSlot(), schedule.getTimeSlot())) {
                if (other.getRoomId() == schedule.getRoomId()) conflicts.add("Room overlaps an existing exam.");
                if (other.getBlockSectionId() == schedule.getBlockSectionId()) conflicts.add("Block overlaps an existing exam.");
                if (other.getFacultyId() == schedule.getFacultyId()) conflicts.add("Faculty overlaps an existing exam.");
            }
        }
        return new ArrayList<>(conflicts);
    }

    public boolean saveSchedule(ExamSchedule schedule) {
        return saveAll(List.of(schedule), schedule.getScheduleId());
    }

    public boolean saveAll(List<ExamSchedule> schedules, int scheduleId) {
        try {
            new com.unimatrix.service.ScheduleEditorService().append(scheduleId, schedules);
            return true;
        } catch (SQLException ex) { throw new IllegalStateException("Database save failed: " + ex.getMessage(), ex); }
    }

    /** Global deletion is retired; use an explicitly selected schedule or folder. */
    @Deprecated public boolean clearAllSchedules() {
        throw new UnsupportedOperationException("Select a schedule or folder to delete; global clearing is disabled.");
    }
}