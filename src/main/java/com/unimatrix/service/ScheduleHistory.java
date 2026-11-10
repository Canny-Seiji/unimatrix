package com.unimatrix.service;
import com.unimatrix.dao.DatabaseConnection;
import com.unimatrix.dao.Transactions;
import com.unimatrix.model.*;
import java.sql.*;
import java.io.*;
import java.time.*;
import java.util.*;

/** Explicit binary snapshots avoid Java object deserialization. Restores create another revision. */
public final class ScheduleHistory {
    public record Entry(int id,String label){public String toString(){return label;}}
    public static void capture(Connection c,ScheduleDocument d,String reason)throws SQLException {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeUTF(d.name());out.writeUTF(d.period());out.writeUTF(d.academicYear());out.writeUTF(d.semester());out.writeInt(d.exams().size());
            for(ExamSchedule e:d.exams()){out.writeInt(e.getCourseId());out.writeInt(e.getRoomId());out.writeInt(e.getBlockSectionId());out.writeInt(e.getFacultyId());out.writeUTF(e.getExamDate()==null?"":e.getExamDate().toString());out.writeUTF(Objects.toString(e.getTimeSlot(),""));}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO schedule_history(schedule_id,reason,snapshot) VALUES (?,?,?)")){p.setInt(1,d.id());p.setString(2,reason);p.setBytes(3,bytes.toByteArray());p.executeUpdate();}
        }catch(IOException e){throw new SQLException("Cannot record schedule history",e);}
    }
    public static List<Entry> list(int id)throws SQLException {
        List<Entry> result=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,saved_at,reason FROM schedule_history WHERE schedule_id=? ORDER BY id DESC")){p.setInt(1,id);try(ResultSet r=p.executeQuery()){while(r.next())result.add(new Entry(r.getInt(1),r.getString(2)+" - "+r.getString(3)));}}return result;
    }
    public static void restore(int id,int history,long expectedRevision)throws SQLException {
        Transactions.write(c->{
            ScheduleDocument current=ScheduleEditorService.read(c,id);
            if(current.revision()!=expectedRevision)throw new SQLException("Schedule changed; reload history before restoring.");
            byte[] bytes;try(PreparedStatement p=c.prepareStatement("SELECT snapshot FROM schedule_history WHERE id=? AND schedule_id=?")){p.setInt(1,history);p.setInt(2,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("History entry missing.");bytes=r.getBytes(1);}}
            try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
                String name=in.readUTF(),period=in.readUTF(),year=in.readUTF(),semester=in.readUTF();int count=in.readInt();if(count<0||count>100000)throw new IOException("Invalid snapshot size");List<ExamSchedule> exams=new ArrayList<>();
                for(int i=0;i<count;i++){int course=in.readInt(),room=in.readInt(),block=in.readInt(),faculty=in.readInt();String date=in.readUTF(),slot=in.readUTF();exams.add(new ExamSchedule(0,course,room,block,faculty,date.isEmpty()?null:LocalDate.parse(date),slot));}
                ScheduleEditorService.validate(exams,ScheduleEditorService.catalog(c));PlanningRules.validate(c,id,exams,PlanningRules.isPublished(c,id));capture(c,current,"Before history restore");
                try(PreparedStatement p=c.prepareStatement("UPDATE schedule_files SET name=?,examination_period=?,academic_year=?,semester=?,revision=revision+1 WHERE id=?")){p.setString(1,name);p.setString(2,period);p.setString(3,year);p.setString(4,semester);p.setInt(5,id);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM exam_schedules WHERE schedule_id=?")){p.setInt(1,id);p.executeUpdate();}ScheduleEditorService.insert(c,id,exams);
            }catch(IOException e){throw new SQLException("Cannot read snapshot",e);}return null;
        });
    }
    public static void publication(int id,boolean publish,long expectedRevision)throws SQLException {
        Transactions.write(c->{ScheduleDocument d=ScheduleEditorService.read(c,id);if(d.revision()!=expectedRevision)throw new SQLException("Schedule changed. Reload and retry.");
            if(publish){ScheduleEditorService.validate(d.exams(),ScheduleEditorService.catalog(c));PlanningRules.validate(c,id,d.exams(),true);}
            capture(c,d,publish?"Before publish":"Before unpublish");
            try(PreparedStatement p=c.prepareStatement("UPDATE schedule_files SET published=?,revision=revision+1 WHERE id=?")){p.setBoolean(1,publish);p.setInt(2,id);p.executeUpdate();}return null;});
    }
}
