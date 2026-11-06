package com.unimatrix.service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Manual starts are unrestricted; each exam must finish on the same calendar date. */
public final class ExamTimes {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LEGACY = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private ExamTimes() { }

    public static LocalTime parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Enter a time, e.g. 08:30 or 2:30 PM.");
        String s = value.trim().toUpperCase(Locale.ENGLISH);
        LocalTime t;
        try { t = LocalTime.parse(s, DateTimeFormatter.ISO_LOCAL_TIME); }
        catch (RuntimeException ex) { t = LocalTime.parse(s, LEGACY); }
        if (t.getSecond() != 0 || t.getNano() != 0) throw new IllegalArgumentException("Use whole-minute times.");
        return t;
    }
    public static int durationMinutes(float hours) {
        double minutes = hours * 60.0;
        if (!Float.isFinite(hours) || minutes < 1 || minutes >= 1440
                || Math.abs(minutes - Math.round(minutes)) > .001)
            throw new IllegalArgumentException("Duration must be positive whole minutes and less than 24 hours.");
        return (int) Math.round(minutes);
    }
    public static LocalTime end(LocalTime start, float hours) {
        int minutes = durationMinutes(hours);
        if (start == null || start.toSecondOfDay() + minutes * 60 >= 86400)
            throw new IllegalArgumentException("Exam must finish before midnight on its examination date.");
        return start.plusMinutes(minutes);
    }
    public static String slot(LocalTime start, LocalTime end) { return CLOCK.format(start) + " - " + CLOCK.format(end); }
    public static LocalTime[] interval(String slot) {
        if (slot == null) throw new IllegalArgumentException("Missing exam time; open this schedule and correct it.");
        String[] parts = slot.split("\\s*-\\s*", -1);
        if (parts.length != 2) throw new IllegalArgumentException("Invalid exam time: " + slot);
        LocalTime start = parse(parts[0]), end = parse(parts[1]);
        if (!end.isAfter(start)) throw new IllegalArgumentException("Exam end must follow its start on the same date.");
        return new LocalTime[]{start, end};
    }
    public static boolean overlaps(String first, String second) {
        LocalTime[] a = interval(first), b = interval(second);
        return a[0].isBefore(b[1]) && b[0].isBefore(a[1]);
    }
}
