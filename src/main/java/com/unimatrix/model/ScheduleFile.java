package com.unimatrix.model;

/** One named schedule (a set of exam rows) living in a folder. */
public class ScheduleFile {
    private final int id;
    private final Integer folderId; // null = root level
    private final String name;

    public ScheduleFile(int id, Integer folderId, String name) {
        this.id = id;
        this.folderId = folderId;
        this.name = name;
    }

    public int getId() { return id; }
    public Integer getFolderId() { return folderId; }
    public String getName() { return name; }

    @Override public String toString() { return name; } // shown by the explorer tree
}