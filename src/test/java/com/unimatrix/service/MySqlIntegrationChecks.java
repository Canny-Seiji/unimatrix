package com.unimatrix.service;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Opt-in, real-MySQL checks. Requires a fresh, dedicated database; never drops data. */
public final class MySqlIntegrationChecks {
    private static int checks;
    public static void main(String[] args)throws Exception {
        String url=System.getenv("UNIMATRIX_TEST_DB_URL");
        if(url==null||!url.matches("jdbc:mysql://[^/]+/unimatrix_feature_test(?:\\?.*)?"))
            throw new IllegalArgumentException("Set UNIMATRIX_TEST_DB_URL to a fresh database named unimatrix_feature_test.");
        System.setProperty("unimatrix.db.url",url);
        System.setProperty("unimatrix.db.user",System.getenv().getOrDefault("UNIMATRIX_TEST_DB_USER","root"));
        System.setProperty("unimatrix.db.password",System.getenv().getOrDefault("UNIMATRIX_TEST_DB_PASSWORD",""));
        try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement()) {
            try(ResultSet r=s.executeQuery("SHOW TABLES")){if(r.next())throw new IllegalStateException("Test database must be empty. No tables were changed.");}
            script(s,Path.of("database/test_schema.sql"));script(s,Path.of("database/002_schedule_editor.sql"));script(s,Path.of("database/003_planning.sql"));
            s.executeUpdate("INSERT INTO rooms(room_name,building,capacity) VALUES ('R1','A',40),('R2','A',80)");
            s.executeUpdate("INSERT INTO courses(course_code,course_title,duration_hours) VALUES ('CS1','OOP',1.5),('CS2','Databases',2),('CS3','Networks',1.5)");
            s.executeUpdate("INSERT INTO block_sections(section_name,student_count) VALUES ('BSCS 1A',30),('BSCS 1B',50)");
        }
        check(new FacultyDAO().addFaculty(new Faculty(0,"José Test","Computing")),"faculty insert uses actual column");
        check(new FacultyDAO().getAllFaculty().size()==1,"faculty read alias");
        try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement()){s.executeUpdate("INSERT INTO faculty_availability(faculty_id,weekday,start_time,end_time) VALUES(1,6,'07:00','21:00')");}
        SchemaCompatibility.check();
        ScheduleEditorService service=new ScheduleEditorService();
        ScheduleDocument saved=service.save(document(0,0,"Midterms",List.of(exam(1,"08:00 - 09:30"),exam(2,"09:30 - 11:30"))),true);
        check(service.load(saved.id()).exams().get(0).getFacultyName().equals("José Test"),"faculty joined read");
        ScheduleDocument finals=service.save(document(0,0,"Finals",saved.exams()),true);check(finals.id()!=saved.id(),"separate-file draft duplicate allowed");
        ScheduleHistory.publication(saved.id(),true,saved.revision());
        reject(()->ScheduleHistory.publication(finals.id(),true,finals.revision()),"published cross-file conflict rejected");
        ScheduleDocument currentPublished=service.load(saved.id());
        ScheduleHistory.publication(saved.id(),false,currentPublished.revision());
        saved=service.load(saved.id());
        ScheduleDocument edited=service.save(document(saved.id(),saved.revision(),"Midterms",List.of(exam(1,"08:00 - 09:30"))),false);
        check(service.load(saved.id()).exams().size()==1,"save removes deleted rows");
        ScheduleDocument stale=saved;reject(()->service.save(stale,false),"stale revision rejected");
        reject(()->service.save(document(0,0,"Duplicates",List.of(exam(1,"08:00 - 09:30"),exam(1,"12:00 - 13:30"))),true),"duplicate rejected");
        // Force a failure after file creation and before the exam batch can commit.
        int count=count("schedule_files");
        try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement()){
            s.execute("CREATE TRIGGER reject_exam BEFORE INSERT ON exam_schedules FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Forced rollback check'");
        }
        try{reject(()->service.save(document(0,0,"Rollback",List.of(exam(1,"08:00 - 09:30"))),true),"batch failure reported");}
        finally{try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement()){s.execute("DROP TRIGGER reject_exam");}}
        check(count("schedule_files")==count,"file creation rolled back with exam insertion");
        EntityDataDAO entities=new EntityDataDAO();var faculty=entities.load(EntityDataDAO.Kind.FACULTY);
        reject(()->entities.save(EntityDataDAO.Kind.FACULTY,faculty,List.of()),"referenced faculty deletion rejected");
        var rooms=entities.load(EntityDataDAO.Kind.ROOM);List<EntityDataDAO.Row> tooSmall=new ArrayList<>(rooms);tooSmall.set(0,new EntityDataDAO.Row(1,List.of("R1","A","10")));
        reject(()->entities.save(EntityDataDAO.Kind.ROOM,rooms,tooSmall),"capacity change rejected");
        check(entities.load(EntityDataDAO.Kind.ROOM).get(0).values().get(2).equals("40"),"entity batch rollback");
        CountDownLatch start=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        Callable<Boolean> append=()->{start.await();try{service.append(edited.id(),List.of(exam(3,"12:00 - 13:30")));return true;}catch(IllegalArgumentException expected){return false;}};
        Future<Boolean> first=pool.submit(append),second=pool.submit(append);start.countDown();
        try{check(first.get(30,TimeUnit.SECONDS)^second.get(30,TimeUnit.SECONDS),"exactly one concurrent duplicate append succeeds");}finally{pool.shutdownNow();}
        check(service.load(edited.id()).exams().size()==2,"concurrent append committed once");
        ScheduleDocument beforeRestore=service.load(edited.id());
        var history=ScheduleHistory.list(edited.id());check(!history.isEmpty(),"history captured");
        ScheduleHistory.restore(edited.id(),history.get(0).id(),beforeRestore.revision());
        check(service.load(edited.id()).revision()>beforeRestore.revision(),"restore advances revision");
        ScheduleFolder folder=new ScheduleFolderDAO().createFolder(null,"Recover me");
        WorkspaceService.trash(folder.getId(),true);
        try {new ScheduleFolderDAO().createFolder(folder.getId(),"Hidden child");throw new AssertionError("deleted parent accepted");}
        catch(IllegalStateException expected){checks++;}
        WorkspaceService.trash(finals.id(),false);
        var courses=entities.load(EntityDataDAO.Kind.COURSE);
        var renamedCourses=new ArrayList<>(courses);renamedCourses.set(0,new EntityDataDAO.Row(1,List.of("CS1","Object-oriented programming","General")));
        entities.save(EntityDataDAO.Kind.COURSE,courses,renamedCourses);
        check(entities.load(EntityDataDAO.Kind.COURSE).get(0).values().get(1).equals("Object-oriented programming"),"master edit with trashed references succeeds");
        var deletedFolder=WorkspaceService.items(true).stream().filter(x->x.folder()&&x.id()==folder.getId()).findFirst().orElseThrow();
        WorkspaceService.recover(deletedFolder);check(WorkspaceService.items(false).stream().anyMatch(x->x.folder()&&x.id()==folder.getId()),"folder recovered");
        WorkspaceService.renameMove(new WorkspaceService.Item(folder.getId(),true,null,"Recover me"),"Renamed",null);
        reject(()->WorkspaceService.renameMove(new WorkspaceService.Item(folder.getId(),true,null,"Renamed"),"Renamed",folder.getId()),"folder cycle rejected");
        System.out.println("MySQL integration checks passed: "+checks);
    }
    private static ScheduleDocument document(int id,long revision,String name,List<ExamSchedule> rows){return new ScheduleDocument(id,null,name,"Midterms","2026-2027","First",revision,rows);}
    private static ExamSchedule exam(int course,String slot){return new ExamSchedule(0,course,1,1,1,LocalDate.of(2026,11,7),slot);}
    private static int count(String table)throws SQLException{try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getInt(1);}}
    private static void script(Statement s,Path path)throws Exception{String sql=Files.readString(path).replaceAll("(?m)^\\s*--.*$","");for(String part:sql.split(";"))if(!part.isBlank())s.execute(part);}
    @FunctionalInterface private interface Attempt{void run()throws Exception;}
    private static void reject(Attempt a,String name)throws Exception{try{a.run();throw new AssertionError(name);}catch(SQLException|IllegalArgumentException expected){checks++;}}
    private static void check(boolean result,String name){if(!result)throw new AssertionError(name);checks++;}
}
