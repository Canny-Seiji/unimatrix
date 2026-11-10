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
        Map<java.time.LocalDate,Integer> dailyCounts = new HashMap<>();
        for (int i = 0; i < exams.size(); i++) {
            ExamSchedule e = exams.get(i);
            String prefix = "Row " + (i + 1) + ": ";
            Course c = courses.get(e.getCourseId());
            Room r = rooms.get(e.getRoomId());
            BlockSection b = blocks.get(e.getBlockSectionId());
            if (c == null || r == null || b == null || !facultyIds.contains(e.getFacultyId()))
                throw new IllegalArgumentException(prefix + "select an existing course, room, block and faculty member.");
            if (!c.getTrack().equalsIgnoreCase("General") && !c.getTrack().equalsIgnoreCase(b.getTrack()))
                throw new IllegalArgumentException(prefix + "course track '" + c.getTrack() + "' is not available to section track '" + b.getTrack() + "'.");
            if (e.getExamDate() == null || e.getExamDate().getDayOfWeek() == DayOfWeek.SUNDAY)
                throw new IllegalArgumentException(prefix + "choose a date from Monday to Saturday.");
            int count = dailyCounts.merge(e.getExamDate(), 1, Integer::sum);
            if (count > PlanningRules.MAX_EXAMS_PER_DAY)
                throw new IllegalArgumentException(prefix + "no more than " + PlanningRules.MAX_EXAMS_PER_DAY + " exams may be scheduled on " + e.getExamDate() + ".");
            if (b.getStudentCount() <= 0 || r.getCapacity() < b.getStudentCount())
                throw new IllegalArgumentException(prefix + r.getRoomName() + " cannot accommodate " + b.getStudentCount() + " students.");
            var times = ExamTimes.interval(e.getTimeSlot());
            if (times[0].isBefore(java.time.LocalTime.of(7,0)) || times[1].isAfter(java.time.LocalTime.of(21,0)))
                throw new IllegalArgumentException(prefix + "exam hours must be between 7 AM and 9 PM.");
            if (!duplicates.add(e.getCourseId() + ":" + e.getBlockSectionId()))
                throw new IllegalArgumentException(prefix + "duplicate course-block exam in this schedule.");
            for (int j = 0; j < i; j++) {
                ExamSchedule other = exams.get(j);
                if ((e.getRoomId() == other.getRoomId() || e.getBlockSectionId() == other.getBlockSectionId())
                        && e.getExamDate().equals(other.getExamDate()) && !PlanningRules.hasMinimumGap(e, other))
                    throw new IllegalArgumentException(prefix + "the room or block section is scheduled too close to row " + (j + 1) + ".");
            }
        }
    }
}
