package com.unimatrix.view;
import com.unimatrix.model.*;
import java.util.Locale;
final class TrackChoices {
    private TrackChoices() {}
    static boolean compatible(Course c, BlockSection b) {
        if (c == null || b == null) return true;
        String ct = norm(c.getTrack()), bt = norm(b.getTrack());
        return ct.equals("general") || bt.equals("general") || ct.equals(bt);
    }
    private static String norm(String s) { return s == null || s.isBlank() ? "general" : s.trim().toLowerCase(Locale.ROOT); }
}
