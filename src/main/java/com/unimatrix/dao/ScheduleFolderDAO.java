package com.unimatrix.dao;

import com.unimatrix.model.ScheduleFile;
import com.unimatrix.model.ScheduleFolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persistence for the explorer: folders and schedule files. Deletion uses recoverable Trash. */
public class ScheduleFolderDAO {

    public List<ScheduleFolder> getAllFolders() {
        List<ScheduleFolder> out = new ArrayList<>();
        String q = "SELECT id, parent_id, name FROM schedule_folders WHERE deleted=FALSE ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                int parent = rs.getInt("parent_id");
                Integer parentId = rs.wasNull() ? null : parent;
                out.add(new ScheduleFolder(rs.getInt("id"), parentId, rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
        return out;
    }

    public List<ScheduleFile> getAllFiles() {
        List<ScheduleFile> out = new ArrayList<>();
        String q = "SELECT id, folder_id, name FROM schedule_files WHERE deleted=FALSE ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                int folder = rs.getInt("folder_id");
                Integer folderId = rs.wasNull() ? null : folder;
                out.add(new ScheduleFile(rs.getInt("id"), folderId, rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
        return out;
    }

    /** Creates an active folder under an active parent, using the shared write lock. */
    public ScheduleFolder createFolder(Integer parentId, String name) {
        int id=create(false,parentId,name);
        return new ScheduleFolder(id,parentId,name.trim());
    }

    public ScheduleFile createFile(Integer folderId, String name) {
        int id=create(true,folderId,name);
        return new ScheduleFile(id,folderId,name.trim());
    }

    private int create(boolean file,Integer parent,String name) {
        if(name==null||name.isBlank()||name.length()>100)throw new IllegalArgumentException("Name must contain 1-100 characters.");
        try { return Transactions.write(c->{
            if(parent!=null)try(PreparedStatement p=c.prepareStatement("SELECT id FROM schedule_folders WHERE id=? AND deleted=FALSE")){
                p.setInt(1,parent);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Parent folder was deleted. Refresh and select an active folder.");}
            }
            String sql=file?"INSERT INTO schedule_files(folder_id,name) VALUES (?,?)":"INSERT INTO schedule_folders(parent_id,name) VALUES (?,?)";
            try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
                p.setObject(1,parent,Types.INTEGER);p.setString(2,name.trim());p.executeUpdate();
                try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("Database returned no created item ID.");return r.getInt(1);}
            }
        }); }catch(SQLException e){throw new IllegalStateException("Database operation failed: "+e.getMessage(),e);}
    }

    /** Move the folder and its active descendants to recoverable Trash. */
    public boolean deleteFolder(int id) { return deleteById("schedule_folders", id); }

    /** Move the schedule to recoverable Trash. */
    public boolean deleteFile(int id) { return deleteById("schedule_files", id); }

    private boolean deleteById(String table, int id) {
        try { com.unimatrix.service.WorkspaceService.trash(id,table.equals("schedule_folders"));return true; }
        catch(SQLException ex){throw new IllegalStateException(ex.getMessage(),ex);}
    }
}
