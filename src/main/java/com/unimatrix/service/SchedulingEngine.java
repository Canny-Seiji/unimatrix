package com.unimatrix.service;

import java.time.*;
import java.util.Set;

/** Compatibility guard: automatic generation now requires explicit exam requirements. */
@Deprecated
public final class SchedulingEngine {
    @Deprecated public boolean generateMasterSchedule(LocalDate date) { throw unsupported(); }
    @Deprecated public GenerationResult generateForAll(int scheduleId, LocalDate start, Set<DayOfWeek> days) { throw unsupported(); }
    @Deprecated public GenerationResult generate(ScheduleRequest request) { throw unsupported(); }
    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Use ExplicitPlanner with course/block/faculty requirements, exam lengths, and a bounded examination period. Rotating assignments are disabled.");
    }
}
