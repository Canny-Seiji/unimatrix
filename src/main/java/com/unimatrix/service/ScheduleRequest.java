package com.unimatrix.service;

import com.unimatrix.model.BlockSection;
import com.unimatrix.model.Course;
import com.unimatrix.model.Faculty;
import com.unimatrix.model.Room;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Everything the engine needs for one run. Lists are the entities the user left checked. */
public record ScheduleRequest(
        int scheduleId,
        LocalDate startDate,
        Set<DayOfWeek> allowedDays,
        List<Room> rooms,
        List<Course> courses,
        List<BlockSection> blocks,
        List<Faculty> faculty) {
}