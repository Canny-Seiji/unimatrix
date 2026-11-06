package com.unimatrix.service;
import com.unimatrix.dao.*;
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
    public static void recover(Item item)throws SQLException {
        Transactions.write(c->{List<Item> deleted=items(c,true),active=items(c,false);Item actual=deleted.stream().filter(x->x.id()==item.id()&&x.folder()==item.folder()).findFirst().orElseThrow(()->new IllegalArgumentException("Item is no longer in Trash."));
            Integer parent=actual.parent();if(parent!=null&&active.stream().noneMatch(x->x.folder()&&x.id()==parent))throw new IllegalArgumentException("Restore the parent folder first.");
            // Restore one item at a time. Children remain in Trash until explicitly selected.
            try(PreparedStatement p=c.prepareStatement("UPDATE "+(item.folder()?"schedule_folders":"schedule_files")+" SET deleted=FALSE"+(item.folder()?"":",published=FALSE,revision=revision+1")+" WHERE id=?")){p.setInt(1,item.id());p.executeUpdate();}return null;});
    }
}