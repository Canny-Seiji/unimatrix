package com.unimatrix.service;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Availability exceptions replace weekly hours for their specific date. */
public final class PlanningRules {
    public record Window(int faculty, Integer weekday, LocalDate date, boolean available, LocalTime start, LocalTime end) { }
    public record Rules(List<Window> windows, Set<LocalDate> excluded, List<ExamSchedule> published) { }
    private PlanningRules() { }
    public static Rules load(Connection c, int exceptSchedule) throws SQLException {
        List<Window> windows=new ArrayList<>(); Set<LocalDate> excluded=new HashSet<>(); List<ExamSchedule> published=new ArrayList<>();
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM faculty_availability")) {
            while(r.next()) windows.add(new Window(r.getInt("faculty_id"),(Integer)r.getObject("weekday"),r.getDate("exception_date")==null?null:r.getDate("exception_date").toLocalDate(),r.getBoolean("available"),r.getTime("start_time").toLocalTime(),r.getTime("end_time").toLocalTime()));
        }
        try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT exam_date FROM excluded_dates")){while(r.next())excluded.add(r.getDate(1).toLocalDate());}
        try(PreparedStatement s=c.prepareStatement("SELECT e.* FROM exam_schedules e JOIN schedule_files f ON f.id=e.schedule_id WHERE f.published=TRUE AND f.deleted=FALSE AND f.id<>?")) {
            s.setInt(1,exceptSchedule);try(ResultSet r=s.executeQuery()){while(r.next()) published.add(new ExamSchedule(r.getInt("id"),r.getInt("course_id"),r.getInt("room_id"),r.getInt("block_section_id"),r.getInt("faculty_id"),r.getDate("exam_date").toLocalDate(),r.getString("time_slot")));}
        }
        return new Rules(windows,excluded,published);
    }
    public static boolean available(Rules rules,ExamSchedule exam) {
        var time=ExamTimes.interval(exam.getTimeSlot());LocalDate date=exam.getExamDate();
        if(rules.excluded().contains(date)||date.getDayOfWeek()==DayOfWeek.SUNDAY)return false;
        List<Window> exceptions=rules.windows().stream().filter(w->w.faculty()==exam.getFacultyId()&&date.equals(w.date())).toList();
        List<Window> windows=exceptions.isEmpty()?rules.windows().stream().filter(w->w.faculty()==exam.getFacultyId()&&w.date()==null&&Objects.equals(w.weekday(),date.getDayOfWeek().getValue())).toList():exceptions;
        if(windows.stream().anyMatch(w->!w.available()&&time[0].isBefore(w.end())&&w.start().isBefore(time[1])))return false;
        // Merge touching available windows, so 08-09 and 09-10 cover an 08-10 exam.
        LocalTime covered=time[0];
        for(Window w:windows.stream().filter(Window::available).sorted(Comparator.comparing(Window::start)).toList()) {
            if(w.start().isAfter(covered))break;
            if(w.end().isAfter(covered))covered=w.end();
            if(!covered.isBefore(time[1]))return true;
        }
        return false;
    }
    public static boolean conflicts(ExamSchedule a,ExamSchedule b) {
        return a.getExamDate().equals(b.getExamDate())&&ExamTimes.overlaps(a.getTimeSlot(),b.getTimeSlot())
                &&(a.getRoomId()==b.getRoomId()||a.getBlockSectionId()==b.getBlockSectionId()||a.getFacultyId()==b.getFacultyId());
    }
    public static void validate(Connection c,int id,List<ExamSchedule> exams,boolean publishing)throws SQLException {
        Rules rules=load(c,id);
        for(int i=0;i<exams.size();i++) {
            ExamSchedule e=exams.get(i);
            if(!available(rules,e))throw new IllegalArgumentException("Row "+(i+1)+": faculty unavailable or examination date excluded.");
            if(publishing&&rules.published().stream().anyMatch(other->conflicts(e,other)))throw new IllegalArgumentException("Row "+(i+1)+": resource conflict with another published schedule.");
        }
    }
    public static boolean isPublished(Connection c,int id)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT published FROM schedule_files WHERE id=? AND deleted=FALSE")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Schedule is deleted or missing.");return r.getBoolean(1);}}
    }
}