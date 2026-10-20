package com.unimatrix.model;

import java.time.LocalDate;

public class ExamSchedule {
    private int id;
    private int courseId;
    private int roomId;
    private int blockSectionId;
    private int facultyId;
    private LocalDate examDate;
    private String timeSlot;

    // Descriptive string fields for view rendering
    private String courseCode;
    private String roomName;
    private String blockSection;
    private String facultyName;

    // Constructor with IDs and Strings (ideal for DAO joining tables)
    public ExamSchedule(int id, int courseId, int roomId, int blockSectionId, int facultyId, 
                        LocalDate examDate, String timeSlot, 
                        String courseCode, String roomName, String blockSection, String facultyName) {
        this.id = id;
        this.courseId = courseId;
        this.roomId = roomId;
        this.blockSectionId = blockSectionId;
        this.facultyId = facultyId;
        this.examDate = examDate;
        this.timeSlot = timeSlot;
        this.courseCode = courseCode;
        this.roomName = roomName;
        this.blockSection = blockSection;
        this.facultyName = facultyName;
    }

    // Overloaded basic constructor (if needed elsewhere)
    public ExamSchedule(int id, int courseId, int roomId, int blockSectionId, int facultyId, LocalDate examDate, String timeSlot) {
        this(id, courseId, roomId, blockSectionId, facultyId, examDate, timeSlot, null, null, null, null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public int getBlockSectionId() { return blockSectionId; }
    public void setBlockSectionId(int blockSectionId) { this.blockSectionId = blockSectionId; }

    public int getFacultyId() { return facultyId; }
    public void setFacultyId(int facultyId) { this.facultyId = facultyId; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    // Getters and Setters for descriptive UI fields
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getBlockSection() { return blockSection; }
    public void setBlockSection(String blockSection) { this.blockSection = blockSection; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    @Override
    public String toString() {
        return "Exam [Course: " + courseCode + ", Date: " + examDate + ", Slot: " + timeSlot + "]";
    }
}