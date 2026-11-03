package com.unimatrix.service;

import java.util.List;

/** Single source of truth for exam time slots, shared by the engine and the UI. */
public final class TimeSlots {
    private TimeSlots() {}

    public static final List<String> ALL = List.of(
            "08:00 AM - 11:00 AM",
            "12:00 PM - 03:00 PM",
            "03:30 PM - 06:30 PM");

    public static String[] asArray() { return ALL.toArray(new String[0]); }
}