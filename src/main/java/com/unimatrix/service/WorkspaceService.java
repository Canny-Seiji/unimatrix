package com.unimatrix.service;
import com.unimatrix.dao.DatabaseConnection;
import com.unimatrix.dao.Transactions;
import java.sql.*;
import java.util.*;

public final class WorkspaceService {
    public record Item(int id,boolean folder,Integer parent,String name){public String toString(){return (folder?"Folder: ":"Schedule: ")+name+" (#"+id+")";}}
    public static List<Item> items(boolean deleted)throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()){return items(c,deleted);}
    }
    private static List<Item> items(Connection c,boolean deleted)throws SQLException {
        List<Item> result=new ArrayList<>();for(boolean folder:new boolean[]{true,false})try(PreparedStatement p=c.prepareStatement("SELECT id,"+(folder?"parent_id":"folder_id")+",name FROM "+(folder?"schedule_folders":"schedule_files")+" WHERE deleted=? ORDER BY name")){p.setBoolean(1,deleted);try(ResultSet r=p.executeQuery()){while(r.next())result.add(new Item(r.getInt(1),folder,(Integer)r.getObject(2),r.getString(3)));}}return result;
    }
    public static void renameMove(Item item,String name,Integer destination)throws SQLException {
        if(name==null||name.isBlank()||name.length()>100)throw new IllegalArgumentException("Name must contain 1-100 characters.");
        Transactions.write(c->{List<Item> all=items(c,false);Item current=all.stream().filter(x->x.id()==item.id()&&x.folder()==item.folder()).findFirst().orElseThrow(()->new IllegalArgumentException("Item is deleted or missing."));if(!current.equals(item))throw new SQLException("Item changed; refresh first.");
            Integer cursor=destination;Set<Integer> seen=new HashSet<>();while(cursor!=null){int id=cursor;if(item.folder()&&id==item.id()||!seen.add(id))throw new IllegalArgumentException("Cannot move a folder into itself or a descendant.");cursor=all.stream().filter(x->x.folder()&&x.id()==id).findFirst().orElseThrow(()->new IllegalArgumentException("Destination folder missing.")).parent();}
            if(!item.folder())ScheduleHistory.capture(c,ScheduleEditorService.read(c,item.id()),"Before rename/move");
            String table=item.folder()?"schedule_folders":"schedule_files",parent=item.folder()?"parent_id":"folder_id";
            try(PreparedStatement p=c.prepareStatement("UPDATE "+table+" SET name=?,"+parent+"=?"+(item.folder()?"":",revision=revision+1")+" WHERE id=?")){p.setString(1,name.trim());p.setObject(2,destination,Types.INTEGER);p.setInt(3,item.id());p.executeUpdate();}return null;});
    }
    public static void trash(int id,boolean folder)throws SQLException {
        Transactions.write(c->{List<Item> all=items(c,false);Set<Integer> folders=new HashSet<>();if(folder)folders.add(id);
            boolean changed;do{changed=false;for(Item i:all)if(i.folder()&&i.parent()!=null&&folders.contains(i.parent()))changed|=folders.add(i.id());}while(changed);
            for(Item i:all)if(!i.folder()&&(!folder&&i.id()==id||folder&&i.parent()!=null&&folders.contains(i.parent()))){ScheduleHistory.capture(c,ScheduleEditorService.read(c,i.id()),"Before deletion");try(PreparedStatement p=c.prepareStatement("UPDATE schedule_files SET deleted=TRUE,published=FALSE,revision=revision+1 WHERE id=?")){p.setInt(1,i.id());p.executeUpdate();}}
            for(int fid:folders)try(PreparedStatement p=c.prepareStatement("UPDATE schedule_folders SET deleted=TRUE WHERE id=?")){p.setInt(1,fid);p.executeUpdate();}return null;});
    }
    public static int recover(Item item)throws SQLException {
        return Transactions.write(c->{List<Item> deleted=items(c,true),active=items(c,false);Item actual=deleted.stream().filter(x->x.id()==item.id()&&x.folder()==item.folder()).findFirst().orElseThrow(()->new IllegalArgumentException("Item is no longer in Trash."));
            Integer parent=actual.parent();if(parent!=null&&active.stream().noneMatch(x->x.folder()&&x.id()==parent))throw new IllegalArgumentException("Restore the parent folder first.");
            int cleared=0;
            if(!item.folder()) {
                List<Integer> conflicted=new ArrayList<>();
                String conflict="SELECT deletedExam.id,deletedExam.start_time,deletedExam.end_time,activeExam.start_time,activeExam.end_time FROM exam_schedules deletedExam "
                        +"JOIN exam_schedules activeExam ON activeExam.schedule_id<>deletedExam.schedule_id "
                        +"AND activeExam.exam_date=deletedExam.exam_date AND (activeExam.room_id=deletedExam.room_id OR activeExam.block_section_id=deletedExam.block_section_id) "
                        +"JOIN schedule_files activeFile ON activeFile.id=activeExam.schedule_id AND activeFile.deleted=FALSE "
                        +"WHERE deletedExam.schedule_id=? AND deletedExam.exam_date IS NOT NULL "
                        +"AND deletedExam.room_id IS NOT NULL AND deletedExam.start_time IS NOT NULL AND deletedExam.end_time IS NOT NULL";
                try(PreparedStatement p=c.prepareStatement(conflict)){p.setInt(1,item.id());try(ResultSet r=p.executeQuery()){while(r.next()){
                    java.time.LocalTime deletedStart=r.getTime(2).toLocalTime(),deletedEnd=r.getTime(3).toLocalTime(),activeStart=r.getTime(4).toLocalTime(),activeEnd=r.getTime(5).toLocalTime();
                    boolean gapOk=!deletedEnd.plusMinutes(PlanningRules.MIN_GAP_MINUTES).isAfter(activeStart)||!activeEnd.plusMinutes(PlanningRules.MIN_GAP_MINUTES).isAfter(deletedStart);
                    if(!gapOk)conflicted.add(r.getInt(1));
                }}}
                cleared=conflicted.size();
                try(PreparedStatement p=c.prepareStatement("UPDATE exam_schedules SET room_id=NULL,exam_date=NULL,time_slot=NULL,start_time=NULL,end_time=NULL WHERE id=?")){for(int id:conflicted){p.setInt(1,id);p.addBatch();}p.executeBatch();}
            }
            // Restore one item at a time. Children remain in Trash until explicitly selected.
            try(PreparedStatement p=c.prepareStatement("UPDATE "+(item.folder()?"schedule_folders":"schedule_files")+" SET deleted=FALSE"+(item.folder()?"":",published=FALSE,revision=revision+1")+" WHERE id=?")){p.setInt(1,item.id());p.executeUpdate();}return cleared;});
    }
    public static void permanentlyDelete(Item item)throws SQLException {
        Transactions.write(c->{
            List<Item> deleted=items(c,true);Item actual=deleted.stream().filter(x->x.id()==item.id()&&x.folder()==item.folder()).findFirst().orElseThrow(()->new IllegalArgumentException("Item is no longer in Trash."));
            Set<Integer> folderIds=new HashSet<>();if(actual.folder())folderIds.add(actual.id());
            boolean changed;do{changed=false;for(Item child:deleted)if(child.folder()&&child.parent()!=null&&folderIds.contains(child.parent()))changed|=folderIds.add(child.id());}while(changed);
            for(Item file:deleted)if(!file.folder()&&((!actual.folder()&&file.id()==actual.id())||(actual.folder()&&file.parent()!=null&&folderIds.contains(file.parent())))){
                try(PreparedStatement p=c.prepareStatement("DELETE FROM schedule_history WHERE schedule_id=?")){p.setInt(1,file.id());p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement("DELETE FROM schedule_files WHERE id=?")){p.setInt(1,file.id());p.executeUpdate();}
            }
            for(Integer folderId:folderIds)try(PreparedStatement p=c.prepareStatement("DELETE FROM schedule_folders WHERE id=?")){p.setInt(1,folderId);p.executeUpdate();}
            return null;
        });
    }
}
