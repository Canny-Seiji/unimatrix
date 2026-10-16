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

    public ExamSchedule(int id, int courseId, int roomId, int blockSectionId, int facultyId, LocalDate examDate, String timeSlot) {
        this.id = id;
        this.courseId = courseId;
        this.roomId = roomId;
        this.blockSectionId = blockSectionId;
        this.facultyId = facultyId;
        this.examDate = examDate;
        this.timeSlot = timeSlot;
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

    @Override
    public String toString() {
        return "Exam [Date: " + examDate + ", Slot: " + timeSlot + "]";
    }
}