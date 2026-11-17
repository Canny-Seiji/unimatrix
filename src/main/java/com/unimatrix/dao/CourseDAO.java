package com.unimatrix.dao;

import com.unimatrix.model.Course;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        String query = "SELECT c.id, c.course_code, c.course_title, c.duration_hours, t.label AS track FROM courses c JOIN track_terms t ON t.id=c.track_term_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                courses.add(new Course(
                    rs.getInt("id"),
                    rs.getString("course_code"),
                    rs.getString("course_title"),
                    rs.getFloat("duration_hours"),
                    rs.getString("track")
                ));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read courses: " + e.getMessage(),e);
        }
        return courses;
    }

    public boolean addCourse(Course course) {
        String query = "INSERT INTO courses (course_code, course_title, duration_hours, track_term_id) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, course.getCourseCode());
            pstmt.setString(2, course.getCourseTitle());
            pstmt.setFloat(3, course.getDurationHours());
            pstmt.setInt(4, TrackTermDAO.requireId(conn, course.getTrack()));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save course: " + e.getMessage(),e);
        }
    }
}