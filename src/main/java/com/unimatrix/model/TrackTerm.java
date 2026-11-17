package com.unimatrix.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One shared database option, for example CSSE, 1st term. Term 0 preserves legacy values. */
public record TrackTerm(int id, String label, String trackName, int termNumber) {
    private static final Pattern TERM = Pattern.compile(
            "^(.+), (1st|2nd|3rd) term$", Pattern.CASE_INSENSITIVE);

    public TrackTerm {
        if (label == null || label.isBlank() || trackName == null || trackName.isBlank()
                || termNumber < 0 || termNumber > AcademicTerm.values().length)
            throw new IllegalArgumentException("Choose a valid track and term.");
    }

    /** Used for existing model constructors as well as catalog labels. */
    public static TrackTerm fromLabel(String value) {
        String label = value == null || value.isBlank() ? "General" : value.trim();
        Matcher match = TERM.matcher(label);
        if (match.matches())
            return new TrackTerm(0, label, match.group(1).trim(), Integer.parseInt(match.group(2).substring(0,1)));
        return new TrackTerm(0, label, label, 0);
    }

    public boolean isGeneral() { return trackName.equalsIgnoreCase("General"); }
    @Override public String toString() { return label; }
}
