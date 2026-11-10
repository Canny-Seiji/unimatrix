package com.unimatrix.service;
import com.unimatrix.model.*;
import java.time.*;
import java.util.*;

/** One requirement represents one actual course/block/faculty exam, not a rotation. */
public final class ExplicitPlanner {
    public record Requirement(int course,int block,int faculty,int minutes) { }
    public static List<ExamSchedule> generate(List<Requirement> requirements,List<Room> rooms,List<BlockSection> blocks,
            LocalDate first,LocalDate last,LocalTime dailyStart,LocalTime dailyEnd,PlanningRules.Rules rules) {
        if(first==null||last==null||last.isBefore(first)||last.isAfter(first.plusYears(1)))throw new IllegalArgumentException("Choose an exam period of at most one year.");
        if(requirements.isEmpty()||!dailyEnd.isAfter(dailyStart))throw new IllegalArgumentException("Enter exam requirements and valid daily hours.");
        List<ExamSchedule> result=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(Requirement req:requirements) {
            if(req.minutes()<30||req.minutes()%30!=0||req.minutes()>840)throw new IllegalArgumentException("Exam lengths must be 30-minute multiples, from 30 to 840 minutes.");
            if(!seen.add(req.course()+":"+req.block()))throw new IllegalArgumentException("Duplicate course/block requirement.");
            BlockSection block=blocks.stream().filter(b->b.getId()==req.block()).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown block."));
            ExamSchedule placed=null;
            for(LocalDate d=first;!d.isAfter(last)&&placed==null;d=d.plusDays(1)) {
                if(d.getDayOfWeek()==DayOfWeek.SUNDAY||rules.excluded().contains(d))continue;
                for(int minute=dailyStart.toSecondOfDay()/60;minute+req.minutes()<=dailyEnd.toSecondOfDay()/60&&placed==null;minute+=30) {
                    LocalTime start=LocalTime.of(minute/60,minute%60);
                    for(Room room:rooms.stream().sorted(Comparator.comparingInt(Room::getCapacity).thenComparingInt(Room::getId)).toList()) {
                        if(room.getCapacity()<block.getStudentCount())continue;
                        ExamSchedule e=new ExamSchedule(0,req.course(),room.getId(),req.block(),req.faculty(),d,ExamTimes.slot(start,start.plusMinutes(req.minutes())));
                        LocalDate candidateDate=d;
                        ExamSchedule candidate=e;
                        long daily=result.stream().filter(x->candidateDate.equals(x.getExamDate())).count();
                        boolean fitsDaily=daily < PlanningRules.MAX_EXAMS_PER_DAY
                                && result.stream().filter(x->x.getRoomId()==candidate.getRoomId()||x.getBlockSectionId()==candidate.getBlockSectionId()).allMatch(x->PlanningRules.hasMinimumGap(candidate,x));
                        boolean fitsPublished=rules.published().stream().filter(x->x.getRoomId()==candidate.getRoomId()||x.getBlockSectionId()==candidate.getBlockSectionId()).allMatch(x->PlanningRules.hasMinimumGap(candidate,x));
                        if(fitsDaily&&fitsPublished&&PlanningRules.available(rules,candidate)&&result.stream().noneMatch(x->PlanningRules.conflicts(candidate,x))&&rules.published().stream().noneMatch(x->PlanningRules.conflicts(candidate,x))){placed=candidate;break;}
                    }
                }
            }
            if(placed==null)throw new IllegalArgumentException("Cannot place course #"+req.course()+", block #"+req.block()+" within the selected dates, availability and room capacity. Nothing saved.");
            result.add(placed);
        }
        return result;
    }
}
