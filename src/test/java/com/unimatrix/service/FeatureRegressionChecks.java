package com.unimatrix.service;

import com.unimatrix.dao.EntityDataDAO;
import com.unimatrix.model.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Standalone checks; also invoked by FeatureRegressionTest under Maven/JUnit. */
public final class FeatureRegressionChecks {
    private static int checks;
    private static final LocalDate SATURDAY=LocalDate.of(2026,11,7);
    private static final Map<Integer,Course> COURSES=Map.of(1,new Course(1,"CS101","OOP",1.5f),2,new Course(2,"CS102","Databases",1.5f),3,new Course(3,"CS103","Networks",4));
    private static final Map<Integer,Room> ROOMS=Map.of(1,new Room(1,"R1","A",50),2,new Room(2,"R2","A",50));
    private static final Map<Integer,BlockSection> BLOCKS=Map.of(1,new BlockSection(1,"BSCS 1A",40),2,new BlockSection(2,"BSCS 1B",40));
    public static void main(String[] args) throws Exception {
        checks=0;
        check(ExamTimes.parse("2:30 PM").equals(LocalTime.of(14,30)),"12-hour input");
        check(ExamTimes.parse("00:10").equals(LocalTime.of(0,10)),"unrestricted early input");
        check(ExamTimes.end(LocalTime.of(6,15),1.5f).equals(LocalTime.of(7,45)),"duration-derived end");
        check(ExamTimes.durationMinutes(1.25f)==75,"quarter-hour duration");
        reject(()->ExamTimes.durationMinutes(Float.NaN),"nonfinite duration");
        reject(()->ExamTimes.durationMinutes(0),"zero duration");
        reject(()->ExamTimes.durationMinutes(.123f),"fraction of a minute");
        reject(()->ExamTimes.end(LocalTime.of(23,0),1.5f),"overnight exam");
        check(ExamTimes.interval("08:00 AM - 11:00 AM")[1].equals(LocalTime.of(11,0)),"legacy slot parsing");
        check(!ExamTimes.overlaps("08:00 - 09:30","09:30 - 11:00"),"adjacent exams");
        check(ExamTimes.overlaps("08:00 - 09:30","09:00 - 10:30"),"partial overlap");
        check(ExamTimes.overlaps("08:00 - 12:00","09:00 - 10:00"),"contained interval");
        valid(List.of(exam(1,1,1,1,"06:00 - 07:30"),exam(2,1,1,1,"07:30 - 09:00")));
        valid(List.of(exam(1,1,1,1,"08:00 - 09:30"),exam(2,2,2,2,"08:00 - 09:30")));
        reject(()->valid(List.of(exam(1,1,1,1,"08:00 - 09:30"),exam(2,1,2,2,"09:00 - 10:30"))),"room overlap");
        reject(()->valid(List.of(exam(1,1,1,1,"08:00 - 09:30"),exam(2,2,1,2,"09:00 - 10:30"))),"block overlap");
        reject(()->valid(List.of(exam(1,1,1,1,"08:00 - 09:30"),exam(2,2,2,1,"09:00 - 10:30"))),"faculty overlap");
        reject(()->valid(List.of(exam(1,1,1,1,"08:00 - 09:30"),exam(1,2,1,2,"12:00 - 13:30"))),"duplicate course/block");
        ExamSchedule sunday=exam(1,1,1,1,"08:00 - 09:30");sunday.setExamDate(SATURDAY.plusDays(1));reject(()->valid(List.of(sunday)),"Sunday");
        reject(()->valid(List.of(exam(1,1,1,1,"08:00 - 11:00"))),"wrong duration");
        reject(()->ScheduleValidator.validate(List.of(exam(1,1,1,1,"08:00 - 09:30")),COURSES,Map.of(1,new Room(1,"Small","A",10)),BLOCKS,Set.of(1)),"manual capacity");
        reject(()->valid(List.of(exam(999,1,1,1,"08:00 - 09:30"))),"deleted entity");
        // Independent schedules can contain the same course/block and resource/time.
        valid(List.of(exam(1,1,1,1,"08:00 - 09:30")));valid(List.of(exam(1,1,1,1,"08:00 - 09:30")));
        EntityDataDAO.validated(EntityDataDAO.Kind.FACULTY,new EntityDataDAO.Row(0,List.of("José Dela Cruz","Computing")));checks++;
        reject(()->EntityDataDAO.validated(EntityDataDAO.Kind.FACULTY,new EntityDataDAO.Row(0,List.of("Name"," "))),"partial faculty row");
        reject(()->EntityDataDAO.validated(EntityDataDAO.Kind.ROOM,new EntityDataDAO.Row(0,List.of("R1","A","-5"))),"negative room capacity");
        reject(()->EntityDataDAO.validated(EntityDataDAO.Kind.COURSE,new EntityDataDAO.Row(0,List.of("CS","Title","Infinity"))),"infinite course duration");
        ScheduleRequest req=new ScheduleRequest(0,SATURDAY,Set.of(DayOfWeek.SATURDAY,DayOfWeek.SUNDAY),List.of(ROOMS.get(1)),List.of(COURSES.get(3)),List.of(BLOCKS.get(1)),List.of(new Faculty(1,"Faculty","Dept")));
        var plan=new SchedulingEngine().plan(req,LocalTime.of(7,0),LocalTime.of(14,0));
        check(plan.exams().size()==1&&plan.exams().get(0).getTimeSlot().equals("07:00 - 11:00"),"four-hour auto exam");
        check(new SchedulingEngine().plan(req,LocalTime.of(7,0),LocalTime.of(9,0)).exams().isEmpty(),"auto window too short");
        Path output=Path.of(args.length>0?args[0]:"target/pdf-check.pdf");Files.createDirectories(output.toAbsolutePath().getParent());
        List<ExamSchedule> rows=new ArrayList<>();
        for(int i=0;i<75;i++) {
            ExamSchedule e=exam(i+1,1,1,1,"08:00 - 09:30");e.setExamDate(SATURDAY.plusDays((i/25)*7L));e.setCourseCode("CS-"+(i+1));
            e.setCourseTitle(i%3==0?"Object-Oriented Programming and Advanced Software Engineering Laboratory":"Database Systems");e.setRoomName("Engineering Building - Room 401");e.setBlockSection("BSCS 2A");e.setFacultyName(i%2==0?"Dr. José Muñoz":"Prof. María Dela Cruz");rows.add(e);
        }
        ScheduleDocument doc=new ScheduleDocument(42,null,"Midterms - College of Computing","Midterms","2026-2027","First Semester",0,rows);
        SchedulePdfExporter.export(doc,output);
        check(Files.size(output)>1000,"PDF written");
        byte[] before=Files.readAllBytes(output);ExamSchedule bad=exam(1,1,1,1,"invalid");
        try{SchedulePdfExporter.export(new ScheduleDocument(42,null,"Bad","Other","2026","First",0,List.of(bad)),output);throw new AssertionError("invalid PDF export accepted");}catch(java.io.IOException expected){checks++;}
        check(Arrays.equals(before,Files.readAllBytes(output)),"failed export preserves destination");
        System.out.println("Feature regression checks passed: "+checks);
    }
    private static ExamSchedule exam(int c,int r,int b,int f,String slot){return new ExamSchedule(0,c,r,b,f,SATURDAY,slot);}
    private static void valid(List<ExamSchedule> rows){ScheduleValidator.validate(rows,COURSES,ROOMS,BLOCKS,Set.of(1,2));checks++;}
    private static void check(boolean result,String name){if(!result)throw new AssertionError(name);checks++;}
    private static void reject(Runnable task,String name){try{task.run();throw new AssertionError("Accepted "+name);}catch(IllegalArgumentException expected){checks++;}}
}
