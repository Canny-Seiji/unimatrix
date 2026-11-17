package com.unimatrix.view;

import com.unimatrix.model.BlockSection;
import com.unimatrix.model.Course;
import com.unimatrix.service.TrackRules;

final class TrackChoices {
    private TrackChoices() { }
    static boolean compatible(Course course, BlockSection block) {
        return TrackRules.compatible(course, block);
    }
}
