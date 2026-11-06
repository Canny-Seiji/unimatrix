package com.unimatrix.dao;

import com.unimatrix.model.Faculty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {

    public List<Faculty> getAllFaculty() {
        List<Faculty> facultyList = new ArrayList<>();
        String query = "SELECT id, faculty_name, department FROM faculty";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                facultyList.add(new Faculty(
                    rs.getInt("id"),
                    rs.getString("faculty_name"),
                    rs.getString("department")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database read failed: " + e.getMessage(), e);
        }
        return facultyList;
    }

    public boolean addFaculty(Faculty faculty) {
        String query = "INSERT INTO faculty (faculty_name, department) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, faculty.getFacultyName());
            pstmt.setString(2, faculty.getDepartment());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Database insert failed: " + e.getMessage(), e);
        }
    }
}