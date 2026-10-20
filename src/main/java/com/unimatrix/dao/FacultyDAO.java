package com.unimatrix.dao;

import com.unimatrix.model.Faculty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyDAO {

    // Fetch all faculty members from the database
    public List<Faculty> getAllFaculty() {
        List<Faculty> facultyList = new ArrayList<>();
        // Ensure this column matches your actual MySQL database column name
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
            System.err.println("Error fetching faculty members:");
            e.printStackTrace();
        }
        return facultyList;
    }

    // Add a new faculty member
    public boolean addFaculty(Faculty faculty) {
        // Must match the column name used in the SELECT query above
        String query = "INSERT INTO faculty (faculty_name, department) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, faculty.getFacultyName());
            pstmt.setString(2, faculty.getDepartment());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting faculty member:");
            e.printStackTrace();
            return false;
        }
    }
}