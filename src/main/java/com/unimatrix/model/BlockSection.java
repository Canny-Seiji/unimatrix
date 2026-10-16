package com.unimatrix.model;

public class BlockSection {
    private int id;
    private String sectionName;
    private int studentCount;

    public BlockSection(int id, String sectionName, int studentCount) {
        this.id = id;
        this.sectionName = sectionName;
        this.studentCount = studentCount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }

    @Override
    public String toString() {
        return sectionName + " [" + studentCount + " students]";
    }
}
