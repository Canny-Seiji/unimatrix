package com.unimatrix.dao;

import com.unimatrix.model.ScheduleFile;
import com.unimatrix.model.ScheduleFolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persistence for the explorer: folders and schedule files. Deletes cascade (see migration_001). */
public class ScheduleFolderDAO {

    public List<ScheduleFolder> getAllFolders() {
        List<ScheduleFolder> out = new ArrayList<>();
        String q = "SELECT id, parent_id, name FROM schedule_folders ORDER BY name";
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
        String q = "SELECT id, folder_id, name FROM schedule_files ORDER BY name";
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

    /** @return the created folder, or null on failure. parentId null = root level. */
    public ScheduleFolder createFolder(Integer parentId, String name) {
        String q = "INSERT INTO schedule_folders (parent_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            if (parentId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, parentId);
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new ScheduleFolder(keys.getInt(1), parentId, name);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
        return null;
    }

    /** @return the created schedule file, or null on failure. folderId null = root level. */
    public ScheduleFile createFile(Integer folderId, String name) {
        String q = "INSERT INTO schedule_files (folder_id, name) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS)) {
            if (folderId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, folderId);
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return new ScheduleFile(keys.getInt(1), folderId, name);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
        return null;
    }

    /** Deletes the folder, its sub-folders, their schedule files and all exam rows inside (cascade). */
    public boolean deleteFolder(int id) { return deleteById("schedule_folders", id); }

    /** Deletes the schedule file and its exam rows (cascade). */
    public boolean deleteFile(int id) { return deleteById("schedule_files", id); }

    private boolean deleteById(String table, int id) {
        try {
            return Transactions.write(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
                    ps.setInt(1, id);
                    return ps.executeUpdate() > 0;
                }
            });
        } catch (SQLException ex) { throw new IllegalStateException("Delete failed: " + ex.getMessage(), ex); }
    }
}
