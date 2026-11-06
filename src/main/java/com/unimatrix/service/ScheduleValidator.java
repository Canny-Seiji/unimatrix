package com.unimatrix.service;

import com.unimatrix.model.*;
import java.time.DayOfWeek;
import java.util.*;

public final class ScheduleValidator {
    private ScheduleValidator() { }
    public static void validate(List<ExamSchedule> exams, Map<Integer, Course> courses,
                                Map<Integer, Room> rooms, Map<Integer, BlockSection> blocks,
                                Set<Integer> facultyIds) {
        Set<String> duplicates = new HashSet<>();
        for (int i = 0; i < exams.size(); i++) {
            ExamSchedule e = exams.get(i);
            String prefix = "Row " + (i + 1) + ": ";
            Course c = courses.get(e.getCourseId());
            Room r = rooms.get(e.getRoomId());
            BlockSection b = blocks.get(e.getBlockSectionId());
            if (c == null || r == null || b == null || !facultyIds.contains(e.getFacultyId()))
                throw new IllegalArgumentException(prefix + "select an existing course, room, block and faculty member.");
            if (e.getExamDate() == null || e.getExamDate().getDayOfWeek() == DayOfWeek.SUNDAY)
                throw new IllegalArgumentException(prefix + "choose a date from Monday to Saturday.");
            if (b.getStudentCount() <= 0 || r.getCapacity() < b.getStudentCount())
                throw new IllegalArgumentException(prefix + r.getRoomName() + " cannot accommodate " + b.getStudentCount() + " students.");
            var times = ExamTimes.interval(e.getTimeSlot());
            if (!ExamTimes.end(times[0], c.getDurationHours()).equals(times[1]))
                throw new IllegalArgumentException(prefix + "end time must match the current course duration. Reopen or edit the row.");
            if (!duplicates.add(e.getCourseId() + ":" + e.getBlockSectionId()))
                throw new IllegalArgumentException(prefix + "duplicate course-block exam in this schedule.");
            for (int j = 0; j < i; j++) {
                ExamSchedule other = exams.get(j);
                if (e.getExamDate().equals(other.getExamDate()) && ExamTimes.overlaps(e.getTimeSlot(), other.getTimeSlot())) {
                    String resource = e.getRoomId() == other.getRoomId() ? "room"
                            : e.getBlockSectionId() == other.getBlockSectionId() ? "block section"
                            : e.getFacultyId() == other.getFacultyId() ? "faculty" : null;
                    if (resource != null) throw new IllegalArgumentException(prefix + resource + " overlaps row " + (j + 1) + ".");
                }
            }
        }
    }
}
