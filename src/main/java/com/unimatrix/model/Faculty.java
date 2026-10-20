package com.unimatrix.model;

public class Faculty {
    private int id;
    private String facultyName;
    private String department;

    public Faculty(int id, String facultyName, String department) {
        this.id = id;
        this.facultyName = facultyName;
        this.department = department;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return facultyName + " (" + department + ")";
    }
}
