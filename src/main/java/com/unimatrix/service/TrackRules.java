package com.unimatrix.service;

import com.unimatrix.model.BlockSection;
import com.unimatrix.model.Course;
import com.unimatrix.model.TrackTerm;

/** Shared by manual choices, automatic requirements, saves and publication. */
public final class TrackRules {
    private TrackRules() { }

    public static boolean compatible(Course course, BlockSection block) {
        // An unselected side must leave the first dropdown unrestricted.
        if (course == null || block == null) return true;
        TrackTerm c = TrackTerm.fromLabel(course.getTrack());
        TrackTerm b = TrackTerm.fromLabel(block.getTrack());
        // Preserve the original General-course wildcard for existing data.
        if (c.isGeneral() && c.termNumber() == 0) return true;
        // Missing terms on legacy specific tracks are not guessed.
        return c.termNumber() == b.termNumber()
                && (c.isGeneral() || c.trackName().equalsIgnoreCase(b.trackName()));
    }
}
