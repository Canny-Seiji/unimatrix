package com.unimatrix.service;

import com.unimatrix.model.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** No database or third-party test runner required. */
public final class TrackRegressionChecks {
    private static int checks;

    public static void main(String[] args) {
        checks=0;
        Course course=new Course(1,"CS1","Programming",2,"CSSE, 1st term");
        BlockSection block=new BlockSection(1,"TN03",30,"CSSE, 1st term");
        check(TrackRules.compatible(course,block),"same track and term");
        check(TrackRules.compatible(null,block),"first course can be chosen");
        check(TrackRules.compatible(course,null),"first block can be chosen");
        block.setTrack("CSSE, 2nd term");
        check(!TrackRules.compatible(course,block),"different terms rejected");
        block.setTrack("ECE, 1st term");
        check(!TrackRules.compatible(course,block),"different tracks rejected");
        block.setTrack("General");
        check(!TrackRules.compatible(course,block),"General section is not a wildcard");
        course.setTrack("General");
        check(TrackRules.compatible(course,block),"legacy General remains compatible");
        block.setTrack("ECE, 3rd term");
        check(TrackRules.compatible(course,block),"legacy General course works across terms");
        course.setTrack("General, 1st term");
        check(!TrackRules.compatible(course,block),"term-specific General does not cross terms");
        block.setTrack("ECE, 1st term");
        check(TrackRules.compatible(course,block),"term-specific General crosses tracks");
        course.setTrack("  csse, 1st term  ");block.setTrack("CSSE, 1st term");
        check(TrackRules.compatible(course,block),"trim and case normalization");
        course.setTrack("CSSE");
        check(!TrackRules.compatible(course,block),"legacy term is not guessed");
        block.setTrack("CSSE");
        check(TrackRules.compatible(course,block),"matching bare legacy tracks preserved");
        course.setTrack("Custom track");block.setTrack("Custom track");
        check(TrackRules.compatible(course,block),"custom legacy labels preserved");
        check(TrackTerm.fromLabel(null).isGeneral(),"legacy default");
        for (AcademicTerm term:AcademicTerm.values()) {
            check(TrackTerm.fromLabel("CPE, "+term).termNumber()==term.number(),"enum term "+term);
        }

        Room room=new Room(1,"R1","A",40);
        var exam=new ExamSchedule(0,1,1,1,1,LocalDate.of(2026,11,17),"08:00 - 10:00");
        course.setTrack("CSSE, 1st term");block.setTrack("CSSE, 1st term");
        var catalog=new ScheduleEditorService.Catalog(List.of(course),List.of(room),List.of(block),
                List.of(new Faculty(1,"Faculty","Computing")));
        ScheduleEditorService.validate(List.of(exam),catalog);checks++;
        block.setTrack("CSSE, 2nd term");
        reject(()->ScheduleEditorService.validate(List.of(exam),catalog),"save rejects wrong term");
        block.setTrack("CPE, 1st term");
        reject(()->ScheduleEditorService.validate(List.of(exam),catalog),"save rejects wrong track");
        block.setTrack("General");
        reject(()->ScheduleEditorService.validate(List.of(exam),catalog),"save agrees on General section");
        course.setTrack("General");
        ScheduleEditorService.validate(List.of(exam),catalog);checks++;
        System.out.println("Track regression checks passed: "+checks);
    }

    private static void check(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static void reject(Runnable action,String message) {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { checks++; }
    }
}
