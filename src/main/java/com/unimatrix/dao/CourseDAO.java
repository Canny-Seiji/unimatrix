package com.unimatrix.dao;

import com.unimatrix.model.Course;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT id, course_code, course_title, duration_hours FROM courses";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                courses.add(new Course(
                    rs.getInt("id"),
                    rs.getString("course_code"),
                    rs.getString("course_title"),
                    rs.getFloat("duration_hours")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    public boolean addCourse(Course course) {
        String query = "INSERT INTO courses (course_code, course_title, duration_hours) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, course.getCourseCode());
            pstmt.setString(2, course.getCourseTitle());
            pstmt.setFloat(3, course.getDurationHours());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}