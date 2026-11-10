package com.unimatrix.service;

import com.unimatrix.dao.DatabaseConnection;
import com.unimatrix.dao.EntityDataDAO;
import com.unimatrix.dao.Transactions;
import com.unimatrix.model.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;

/** Transaction boundary for snapshots, manual saves and legacy engine appends. */
public class ScheduleEditorService {
    public record Catalog(List<Course> courses, List<Room> rooms, List<BlockSection> blocks, List<Faculty> faculty) { }
    public Catalog catalog() throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) { return catalog(c); }
    }
    public static Catalog catalog(Connection c) throws SQLException {
        EntityDataDAO dao=new EntityDataDAO();
        List<Course> courses=new ArrayList<>(); List<Room> rooms=new ArrayList<>();
        List<BlockSection> blocks=new ArrayList<>(); List<Faculty> faculty=new ArrayList<>();
        for(var r:dao.load(c,EntityDataDAO.Kind.COURSE)) courses.add(new Course(r.id(),r.values().get(0),r.values().get(1),0,r.values().get(2)));
        for(var r:dao.load(c,EntityDataDAO.Kind.ROOM)) rooms.add(new Room(r.id(),r.values().get(0),r.values().get(1),Integer.parseInt(r.values().get(2))));
        for(var r:dao.load(c,EntityDataDAO.Kind.BLOCK)) blocks.add(new BlockSection(r.id(),r.values().get(0),Integer.parseInt(r.values().get(1)),r.values().get(2)));
        for(var r:dao.load(c,EntityDataDAO.Kind.FACULTY)) faculty.add(new Faculty(r.id(),r.values().get(0),r.values().get(1)));
        courses.sort(Comparator.comparing(Course::toString, String.CASE_INSENSITIVE_ORDER));
        rooms.sort(Comparator.comparing(Room::toString, String.CASE_INSENSITIVE_ORDER));
        blocks.sort(Comparator.comparing(BlockSection::toString, String.CASE_INSENSITIVE_ORDER));
        faculty.sort(Comparator.comparing(Faculty::toString, String.CASE_INSENSITIVE_ORDER));
        return new Catalog(courses,rooms,blocks,faculty);
    }
    public static void validate(List<ExamSchedule> exams, Catalog catalog) {
        Map<Integer,Course> courses=new HashMap<>(); catalog.courses().forEach(x->courses.put(x.getId(),x));
        Map<Integer,Room> rooms=new HashMap<>(); catalog.rooms().forEach(x->rooms.put(x.getId(),x));
        Map<Integer,BlockSection> blocks=new HashMap<>(); catalog.blocks().forEach(x->blocks.put(x.getId(),x));
        Set<Integer> faculty=new HashSet<>(); catalog.faculty().forEach(x->faculty.add(x.getId()));
        ScheduleValidator.validate(exams,courses,rooms,blocks,faculty);
    }
    public static void validateStored(Connection c,int id) throws SQLException { validate(read(c,id).exams(),catalog(c)); }
    public ScheduleDocument load(int id) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ); c.setAutoCommit(false);
            try { ScheduleDocument document=read(c,id); c.commit(); return document; }
            catch(SQLException|RuntimeException ex) { c.rollback(); throw ex; }
        }
    }
    public static ScheduleDocument read(Connection c,int id) throws SQLException {
        Integer folder; String name,period,year,semester; long revision;
        try(PreparedStatement p=c.prepareStatement("SELECT folder_id,name,examination_period,academic_year,semester,revision FROM schedule_files WHERE id=? AND deleted=FALSE")) {
            p.setInt(1,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next()) throw new SQLException("Schedule no longer exists. Refresh the explorer.");
                folder=(Integer)r.getObject(1); name=r.getString(2); period=r.getString(3); year=r.getString(4); semester=r.getString(5); revision=r.getLong(6);
            }
        }
        List<ExamSchedule> exams=new ArrayList<>();
        String sql="SELECT e.*, c.course_code, c.course_title, r.room_name, b.section_name, f.faculty_name FROM exam_schedules e "
                +"LEFT JOIN courses c ON c.id=e.course_id LEFT JOIN rooms r ON r.id=e.room_id "
                +"LEFT JOIN block_sections b ON b.id=e.block_section_id LEFT JOIN faculty f ON f.id=e.faculty_id "
                +"WHERE e.schedule_id=? ORDER BY e.exam_date,e.start_time,e.id";
        try(PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,id);
            try(ResultSet r=p.executeQuery()) { while(r.next()) {
                Time start=r.getTime("start_time"),end=r.getTime("end_time");
                String slot=start!=null&&end!=null?ExamTimes.slot(start.toLocalTime(),end.toLocalTime()):r.getString("time_slot");
                java.sql.Date date=r.getDate("exam_date");
                ExamSchedule exam=new ExamSchedule(r.getInt("id"),r.getInt("course_id"),r.getInt("room_id"),r.getInt("block_section_id"),r.getInt("faculty_id"),
                        date==null?null:date.toLocalDate(),slot,r.getString("course_code"),r.getString("room_name"),r.getString("section_name"),r.getString("faculty_name"));
                exam.setCourseTitle(r.getString("course_title")); exam.setScheduleId(id); exams.add(exam);
            }}
        }
        return new ScheduleDocument(id,folder,name,period,year,semester,revision,exams);
    }
    public ScheduleDocument save(ScheduleDocument document, boolean asNew) throws SQLException {
        validateMetadata(document);
        if(document.exams().isEmpty()) throw new IllegalArgumentException("Add at least one completed exam row.");
        return Transactions.write(c -> {
            validate(document.exams(),catalog(c));
            if(document.folderId()!=null)try(PreparedStatement p=c.prepareStatement("SELECT id FROM schedule_folders WHERE id=? AND deleted=FALSE")){p.setInt(1,document.folderId());try(ResultSet rows=p.executeQuery()){if(!rows.next())throw new SQLException("Destination folder was deleted. Choose an active folder.");}}
            int id=document.id();
            boolean published=!asNew&&id>0&&PlanningRules.isPublished(c,id);
            PlanningRules.validate(c,asNew?0:id,document.exams(),published);
            if(!asNew&&id>0) ScheduleHistory.capture(c,read(c,id),"Before edit");
            if(asNew || id==0) {
                try(PreparedStatement p=c.prepareStatement("INSERT INTO schedule_files (folder_id,name,examination_period,academic_year,semester) VALUES (?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setObject(1,document.folderId(),Types.INTEGER); p.setString(2,document.name().trim()); p.setString(3,document.period());
                    p.setString(4,document.academicYear().trim());p.setString(5,document.semester().trim());p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()) { if(!r.next()) throw new SQLException("No schedule ID returned."); id=r.getInt(1); }
                }
            } else {
                try(PreparedStatement p=c.prepareStatement("UPDATE schedule_files SET name=?,examination_period=?,academic_year=?,semester=?,revision=revision+1 WHERE id=? AND revision=? AND deleted=FALSE")) {
                    p.setString(1,document.name().trim());p.setString(2,document.period());p.setString(3,document.academicYear().trim());p.setString(4,document.semester().trim());
                    p.setInt(5,id);p.setLong(6,document.revision());
                    if(p.executeUpdate()!=1) throw new SQLException("Schedule changed or was deleted elsewhere. Reopen it before saving; your draft has been retained.");
                }
                try(PreparedStatement p=c.prepareStatement("DELETE FROM exam_schedules WHERE schedule_id=?")) {p.setInt(1,id);p.executeUpdate();}
            }
            insert(c,id,document.exams());
            return read(c,id);
        });
    }
    public void append(int id,List<ExamSchedule> exams) throws SQLException {
        Transactions.write(c -> {
            ScheduleDocument stored=read(c,id);
            List<ExamSchedule> all=new ArrayList<>(stored.exams()); all.addAll(exams);
            validate(all,catalog(c)); PlanningRules.validate(c,id,all,PlanningRules.isPublished(c,id)); ScheduleHistory.capture(c,stored,"Before append"); insert(c,id,exams);
            try(PreparedStatement p=c.prepareStatement("UPDATE schedule_files SET revision=revision+1 WHERE id=?")){p.setInt(1,id);p.executeUpdate();}
            return null;
        });
    }
    public static void insert(Connection c,int id,List<ExamSchedule> exams) throws SQLException {
        String sql="INSERT INTO exam_schedules (course_id,room_id,block_section_id,faculty_id,schedule_id,exam_date,time_slot,start_time,end_time) VALUES (?,?,?,?,?,?,?,?,?)";
        try(PreparedStatement p=c.prepareStatement(sql)) {
            for(ExamSchedule e:exams) {
                LocalTime[] t=ExamTimes.interval(e.getTimeSlot());
                p.setInt(1,e.getCourseId());p.setInt(2,e.getRoomId());p.setInt(3,e.getBlockSectionId());p.setInt(4,e.getFacultyId());p.setInt(5,id);
                p.setDate(6,java.sql.Date.valueOf(e.getExamDate()));p.setString(7,ExamTimes.slot(t[0],t[1]));p.setTime(8,Time.valueOf(t[0]));p.setTime(9,Time.valueOf(t[1]));p.addBatch();
            }
            p.executeBatch();
        }
    }
    private static void validateMetadata(ScheduleDocument d) {
        required(d.name(),100,"Schedule name"); required(d.academicYear(),20,"Academic year"); required(d.semester(),30,"Semester");
        if(!Set.of("Prelims","Midterms","Finals","Other").contains(d.period())) throw new IllegalArgumentException("Select an examination period.");
    }
    private static void required(String text,int limit,String field) {
        if(text==null||text.isBlank()||text.codePointCount(0,text.length())>limit) throw new IllegalArgumentException(field+" is required (maximum "+limit+" characters).");
    }
}
