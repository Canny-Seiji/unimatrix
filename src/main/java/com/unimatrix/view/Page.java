package com.unimatrix.view;

/** Top-level destinations shown in the sidebar. */
public enum Page {
    DASHBOARD("Dashboard"),
    MANAGE_DATA("Manage Data"),
    SCHEDULER("Scheduler"),
    SCHEDULE_VIEWER("Schedule Viewer"),
    EXPORT("Export");
    

    private final String title;

    Page(String title) { this.title = title; }

    public String title() { return title; }
}