package com.unimatrix.dao;

import com.unimatrix.model.BlockSection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlockSectionDAO {

    // Fetch all block sections from the database
    public List<BlockSection> getAllBlockSections() {
        List<BlockSection> sections = new ArrayList<>();
        String query = "SELECT id, section_name, student_count FROM block_sections";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                sections.add(new BlockSection(
                    rs.getInt("id"),
                    rs.getString("section_name"),
                    rs.getInt("student_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching block sections:");
            e.printStackTrace();
        }
        return sections;
    }

    // Add a new block section
    public boolean addBlockSection(BlockSection section) {
        String query = "INSERT INTO block_sections (section_name, student_count) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, section.getSectionName());
            pstmt.setInt(2, section.getStudentCount());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting block section:");
            e.printStackTrace();
            return false;
        }
    }
}