package com.unimatrix.service;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import java.time.*;
import java.util.*;

public class SchedulingEngine {
    private final RoomDAO roomDAO=new RoomDAO();
    private final BlockSectionDAO blockDAO=new BlockSectionDAO();
    private final CourseDAO courseDAO=new CourseDAO();
    private final FacultyDAO facultyDAO=new FacultyDAO();
    private final ExamScheduleDAO scheduleDAO=new ExamScheduleDAO();
    private static final int MAX_SEARCH_DAYS=365;
    public record Plan(List<ExamSchedule> exams,List<String> skipped,int requested) {
        public Plan {exams=List.copyOf(exams);skipped=List.copyOf(skipped);}
    }
    /** Global destructive generation is retired: callers must choose a schedule file. */
    @Deprecated public boolean generateMasterSchedule(LocalDate startDate) {
        throw new UnsupportedOperationException("Use scoped generation with a schedule ID; existing schedules are never cleared globally.");
    }
    public GenerationResult generateForAll(int scheduleId,LocalDate startDate,Set<DayOfWeek> allowedDays) {
        return generate(new ScheduleRequest(scheduleId,startDate,allowedDays,roomDAO.getAllRooms(),courseDAO.getAllCourses(),blockDAO.getAllBlockSections(),facultyDAO.getAllFaculty()));
    }
    /** Compatibility entry point. Persistence revalidates under the database write lock. */
    public GenerationResult generate(ScheduleRequest request) {
        try {
            Plan plan=plan(request,LocalTime.of(8,0),LocalTime.of(18,30));
            new ScheduleEditorService().append(request.scheduleId(),plan.exams());
            return new GenerationResult(plan.requested(),plan.exams().size(),plan.skipped(),null);
        } catch(Exception ex){return GenerationResult.failed(ex.getMessage());}
    }
    /**
     * Earliest-fit placement, smallest sufficient room first. Prefers separate days per block.
     * LIMITATION: the schema has no course-to-block or course-to-faculty relation.
     * Automatic generation retains round-robin assignments; edit them in Manual Encoding.
     * User-selected daily bounds apply only to automatic placement, never to manual encoding.
     */
    public Plan plan(ScheduleRequest req,LocalTime dayStart,LocalTime dayEnd) {
        if(req.startDate()==null||req.courses().isEmpty()||req.rooms().isEmpty()||req.blocks().isEmpty()||req.faculty().isEmpty())
            throw new IllegalArgumentException("Choose a start date and at least one course, room, block and faculty member.");
        Set<DayOfWeek> days=new HashSet<>(req.allowedDays());days.remove(DayOfWeek.SUNDAY);
        if(days.isEmpty())throw new IllegalArgumentException("Choose at least one day from Monday to Saturday.");
        if(dayStart==null||dayEnd==null||!dayEnd.isAfter(dayStart))throw new IllegalArgumentException("Daily end must follow daily start.");
        List<ExamSchedule> occupancy=new ArrayList<>();
        if(req.scheduleId()>0)occupancy.addAll(scheduleDAO.getSchedulesByFile(req.scheduleId()));
        List<Room> rooms=new ArrayList<>(req.rooms());rooms.sort(Comparator.comparingInt(Room::getCapacity).thenComparingInt(Room::getId));
        List<ExamSchedule> output=new ArrayList<>();List<String> skipped=new ArrayList<>();
        for(int i=0;i<req.courses().size();i++) {
            Course course=req.courses().get(i);BlockSection block=req.blocks().get(i%req.blocks().size());Faculty faculty=req.faculty().get(i%req.faculty().size());
            if(occupancy.stream().anyMatch(e->e.getCourseId()==course.getId()&&e.getBlockSectionId()==block.getId())){skipped.add(course.getCourseCode()+": course-block exam already exists.");continue;}
            int duration=ExamTimes.durationMinutes(course.getDurationHours());ExamSchedule placed=null;
            for(int pass=0;pass<2&&placed==null;pass++) {
                for(int offset=0;offset<MAX_SEARCH_DAYS&&placed==null;offset++) {
                    LocalDate date=req.startDate().plusDays(offset);
                    if(!days.contains(date.getDayOfWeek()))continue;
                    if(pass==0&&occupancy.stream().anyMatch(e->date.equals(e.getExamDate())&&e.getBlockSectionId()==block.getId()))continue;
                    int latest=dayEnd.toSecondOfDay()/60-duration;
                    for(int minute=dayStart.toSecondOfDay()/60;minute<=latest&&placed==null;minute++) {
                        LocalTime start=LocalTime.of(minute/60,minute%60);String slot=ExamTimes.slot(start,start.plusMinutes(duration));
                        for(Room room:rooms) {
                            if(room.getCapacity()<block.getStudentCount())continue;
                            boolean conflict=occupancy.stream().anyMatch(e->date.equals(e.getExamDate())&&ExamTimes.overlaps(slot,e.getTimeSlot())&&(e.getRoomId()==room.getId()||e.getBlockSectionId()==block.getId()||e.getFacultyId()==faculty.getId()));
                            if(!conflict){placed=new ExamSchedule(0,course.getId(),room.getId(),block.getId(),faculty.getId(),date,slot);break;}
                        }
                    }
                }
            }
            if(placed==null)skipped.add(course.getCourseCode()+": no room/time fits the chosen days, daily bounds and capacity.");
            else{output.add(placed);occupancy.add(placed);}
        }
        return new Plan(output,skipped,req.courses().size());
    }
}
