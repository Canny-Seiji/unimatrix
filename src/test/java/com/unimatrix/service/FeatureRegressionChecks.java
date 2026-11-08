package com.unimatrix.service;
import com.unimatrix.model.*;
import java.time.*;
import java.util.*;

public final class FeatureRegressionChecks {
    static int checks;
    static LocalDate sat=LocalDate.of(2026,11,7);
    static ExamSchedule exam(String time){return new ExamSchedule(0,1,1,1,1,sat,time);}
    static PlanningRules.Window weekly(LocalTime start,LocalTime end){return new PlanningRules.Window(1,6,null,true,start,end);}
    public static void main(String[] args){checks=0;
        check(ExamTimes.parse("9:00 PM").equals(LocalTime.of(21,0)),"12-hour input");
        check(!ExamTimes.overlaps("07:00 - 08:00","08:00 - 09:00"),"adjacent intervals");
        check(ExamTimes.overlaps("07:00 - 09:00","08:30 - 10:00"),"partial overlap");
        var windows=List.of(weekly(LocalTime.of(7,0),LocalTime.of(12,0)),weekly(LocalTime.of(12,0),LocalTime.of(21,0)));
        var rules=new PlanningRules.Rules(windows,Set.of(),List.of());
        check(PlanningRules.available(rules,exam("11:00 - 13:00")),"adjacent availability windows merge");
        check(PlanningRules.available(new PlanningRules.Rules(List.of(),Set.of(),List.of()),exam("07:00 - 08:00")),"missing availability defaults to planning hours");
        check(!PlanningRules.available(new PlanningRules.Rules(windows,Set.of(sat),List.of()),exam("07:00 - 08:00")),"excluded date");
        List<PlanningRules.Window> exception=new ArrayList<>(windows);exception.add(new PlanningRules.Window(1,null,sat,false,LocalTime.of(7,0),LocalTime.of(21,0)));
        check(!PlanningRules.available(new PlanningRules.Rules(exception,Set.of(),List.of()),exam("07:00 - 08:00")),"unavailable date overrides weekly");
        exception.set(2,new PlanningRules.Window(1,null,sat,true,LocalTime.of(10,0),LocalTime.of(12,0)));
        var overridden=new PlanningRules.Rules(exception,Set.of(),List.of());
        check(!PlanningRules.available(overridden,exam("07:00 - 08:00")),"exception replaces weekly hours");check(PlanningRules.available(overridden,exam("10:00 - 11:00")),"available exception");
        List<Room> rooms=List.of(new Room(1,"Room","A",50));List<BlockSection> blocks=List.of(new BlockSection(1,"A",30),new BlockSection(2,"B",30));
        var requirements=List.of(new ExplicitPlanner.Requirement(1,1,1,60),new ExplicitPlanner.Requirement(1,2,1,90));
        var result=ExplicitPlanner.generate(requirements,rooms,blocks,sat,sat,LocalTime.of(7,0),LocalTime.of(21,0),rules);
        check(result.size()==2,"same course produces exams for both specified blocks");check(result.get(0).getBlockSectionId()==1&&result.get(1).getBlockSectionId()==2,"explicit blocks preserved");check(result.stream().allMatch(e->e.getFacultyId()==1),"explicit faculty preserved");check(!PlanningRules.conflicts(result.get(0),result.get(1)),"generated resources do not overlap");
        reject(()->ExplicitPlanner.generate(requirements,rooms,blocks,sat,sat.minusDays(1),LocalTime.of(7,0),LocalTime.of(21,0),rules),"invalid date range");
        reject(()->ExplicitPlanner.generate(requirements,rooms,blocks,sat,sat,LocalTime.of(7,0),LocalTime.of(8,0),rules),"all requirements must fit");
        reject(()->ExplicitPlanner.generate(List.of(requirements.get(0),requirements.get(0)),rooms,blocks,sat,sat,LocalTime.of(7,0),LocalTime.of(21,0),rules),"duplicate requirements");
        var live=new PlanningRules.Rules(windows,Set.of(),List.of(exam("07:00 - 08:00")));
        var after=ExplicitPlanner.generate(List.of(requirements.get(0)),rooms,blocks,sat,sat,LocalTime.of(7,0),LocalTime.of(21,0),live);check(ExamTimes.interval(after.get(0).getTimeSlot())[0].equals(LocalTime.of(8,0)),"planner avoids published bookings");
        var courses=Map.of(1,new Course(1,"CS","Title",99));var roomMap=Map.of(1,rooms.get(0));var blockMap=Map.of(1,blocks.get(0));
        ScheduleValidator.validate(List.of(exam("07:00 - 08:30")),courses,roomMap,blockMap,Set.of(1));checks++;
        reject(()->ScheduleValidator.validate(List.of(exam("06:30 - 08:00")),courses,roomMap,blockMap,Set.of(1)),"manual starts before seven");
        reject(()->ScheduleValidator.validate(List.of(exam("20:00 - 21:30")),courses,roomMap,blockMap,Set.of(1)),"manual ends after nine");
        System.out.println("Planning regression checks passed: "+checks);
    }
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    static void reject(Runnable action,String message){try{action.run();throw new AssertionError(message);}catch(IllegalArgumentException expected){checks++;}}
}
