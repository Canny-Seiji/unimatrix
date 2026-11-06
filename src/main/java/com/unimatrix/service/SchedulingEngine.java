package com.unimatrix.service;

import java.time.*;
import java.util.Set;

/** Compatibility guard: automatic generation now requires explicit exam requirements. */
@Deprecated
public final class SchedulingEngine {
    public boolean generateMasterSchedule(LocalDate date) { throw unsupported(); }
    public GenerationResult generateForAll(int scheduleId, LocalDate start, Set<DayOfWeek> days) { throw unsupported(); }
    public GenerationResult generate(ScheduleRequest request) { throw unsupported(); }
    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Use ExplicitPlanner with course/block/faculty requirements, exam lengths, and a bounded examination period. Rotating assignments are disabled.");
    }
}