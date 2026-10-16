package com.unimatrix.model;

public class Faculty {
    private int id;
    private String fullName;
    private String department;

    public Faculty(int id, String fullName, String department) {
        this.id = id;
        this.fullName = fullName;
        this.department = department;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    @Override
    public String toString() {
        return fullName + " (" + department + ")";
    }
}
