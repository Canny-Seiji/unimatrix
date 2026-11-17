package com.unimatrix.dao;

import com.unimatrix.model.BlockSection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlockSectionDAO {

    // Fetch all block sections from the database
    public List<BlockSection> getAllBlockSections() {
        List<BlockSection> sections = new ArrayList<>();
        String query = "SELECT b.id, b.section_name, b.student_count, t.label AS track FROM block_sections b JOIN track_terms t ON t.id=b.track_term_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sections.add(new BlockSection(
                    rs.getInt("id"),
                    rs.getString("section_name"),
                    rs.getInt("student_count"),
                    rs.getString("track")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database read failed: " + e.getMessage(), e);
        }
        return sections;
    }

    // Add a new block section
    public boolean addBlockSection(BlockSection section) {
        String query = "INSERT INTO block_sections (section_name, student_count, track_term_id) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, section.getSectionName());
            pstmt.setInt(2, section.getStudentCount());
            pstmt.setInt(3, TrackTermDAO.requireId(conn, section.getTrack()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Database insert failed: " + e.getMessage(), e);
        }
    }
}