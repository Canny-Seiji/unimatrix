package com.unimatrix.model;

public class BlockSection {
    private int id;
    private String sectionName;
    private int studentCount;
    private String track;

    public BlockSection(int id, String sectionName, int studentCount) {
        this(id, sectionName, studentCount, "General");
    }
    public BlockSection(int id, String sectionName, int studentCount, String track) {
        this.id = id;
        this.sectionName = sectionName;
        this.studentCount = studentCount;
        this.track = track == null || track.isBlank() ? "General" : track;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    public String getTrack() { return track; }
    public void setTrack(String track) { this.track = track; }

    @Override
    public String toString() {
        return sectionName + " [" + studentCount + " students]";
    }
}
