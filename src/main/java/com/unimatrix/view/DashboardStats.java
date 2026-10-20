package com.unimatrix.view;

/** Snapshot shown on the dashboard. lastScheduleSummary may be null when nothing was generated yet. */
public record DashboardStats(int rooms, int blocks, int faculty, int courses, String lastScheduleSummary) {}