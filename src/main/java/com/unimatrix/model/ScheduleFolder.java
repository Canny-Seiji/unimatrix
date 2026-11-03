package com.unimatrix.model;

public class ScheduleFolder {
    private final int id;
    private final Integer parentId; // null = root level
    private final String name;

    public ScheduleFolder(int id, Integer parentId, String name) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
    }

    public int getId() { return id; }
    public Integer getParentId() { return parentId; }
    public String getName() { return name; }

    @Override public String toString() { return name; } // shown by the explorer tree
}