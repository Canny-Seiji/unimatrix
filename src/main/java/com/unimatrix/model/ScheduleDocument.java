package com.unimatrix.model;

import java.util.List;

/** An editable snapshot. Revision prevents one editor overwriting another editor's work. */
public record ScheduleDocument(int id, Integer folderId, String name, String period,
                               String academicYear, String semester, long revision,
                               List<ExamSchedule> exams) {
    public ScheduleDocument { exams = List.copyOf(exams); }
}
