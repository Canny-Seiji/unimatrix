package com.unimatrix.model;

public class Course {
    private int id;
    private String courseCode;
    private String courseTitle;
    private float durationHours;

    public Course(int id, String courseCode, String courseTitle, float durationHours) {
        this.id = id;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.durationHours = durationHours;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public float getDurationHours() { return durationHours; }
    public void setDurationHours(float durationHours) { this.durationHours = durationHours; }

    @Override
    public String toString() {
        return courseCode + " - " + courseTitle;
    }
}