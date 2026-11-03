package com.unimatrix.service;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class SchedulingEngine {

    private final RoomDAO roomDAO = new RoomDAO();
    private final BlockSectionDAO blockDAO = new BlockSectionDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final ExamScheduleDAO scheduleDAO = new ExamScheduleDAO();

    // Standard time slots available for exams
    private final String[] TIME_SLOTS = {
        "08:00 AM - 11:00 AM", 
        "12:00 PM - 03:00 PM", 
        "03:30 PM - 06:30 PM"
    };

    public boolean generateMasterSchedule(LocalDate startDate) {
        // 1. Clear existing schedules to start fresh
        scheduleDAO.clearAllSchedules();

        // 2. Load all entities from database via DAOs
        List<Course> courses = courseDAO.getAllCourses();
        List<Room> rooms = roomDAO.getAllRooms();
        List<BlockSection> blocks = blockDAO.getAllBlockSections();
        List<Faculty> facultyList = facultyDAO.getAllFaculty();

        if (courses.isEmpty() || rooms.isEmpty() || blocks.isEmpty() || facultyList.isEmpty()) {
            System.err.println("Error: Missing base data. Ensure rooms, blocks, courses, and faculty are populated.");
            return false;
        }

        // 3. Load already assigned schedules to check conflicts
        List<ExamSchedule> existingSchedules = scheduleDAO.getAllSchedules();

        int facultyIndex = 0;
        LocalDate currentDate = startDate;
        int slotIndex = 0;

        // 4. Greedy Assignment Loop
        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            BlockSection block = blocks.get(i % blocks.size()); // Map block round-robin for demo
            Faculty assignedFaculty = facultyList.get(facultyIndex % facultyList.size());

            Room selectedRoom = null;

            // Find a room that satisfies the capacity constraint
            for (Room room : rooms) {
                if (room.getCapacity() >= block.getStudentCount()) {
                    // Check if room, block, and faculty are free during this date/slot
                    if (isSlotAvailable(room.getId(), block.getId(), assignedFaculty.getId(), currentDate, TIME_SLOTS[slotIndex], existingSchedules)) {
                        selectedRoom = room;
                        break;
                    }
                }
            }

            if (selectedRoom != null) {
                // Create and save the schedule slot
                ExamSchedule newExam = new ExamSchedule(
                    0, 
                    course.getId(), 
                    selectedRoom.getId(), 
                    block.getId(), 
                    assignedFaculty.getId(), 
                    currentDate, 
                    TIME_SLOTS[slotIndex]
                );

                scheduleDAO.saveSchedule(newExam);
                existingSchedules.add(newExam); // Update local tracking list

                // Advance time slot / date for the next exam
                slotIndex++;
                if (slotIndex >= TIME_SLOTS.length) {
                    slotIndex = 0;
                    currentDate = currentDate.plusDays(1); // Move to next day
                }
                facultyIndex++;
            } else {
                System.out.println("Warning: Could not find a valid room/slot for course: " + course.getCourseCode());
            }
        }

        System.out.println("Schedule generation complete!");
        return true;
    }

    // Helper method to validate hard constraints
    private boolean isSlotAvailable(int roomId, int blockId, int facultyId, LocalDate date, String slot, List<ExamSchedule> schedules) {
        for (ExamSchedule s : schedules) {
            if (s.getExamDate() != null && s.getExamDate().equals(date) && s.getTimeSlot().equals(slot)) {
                // Hard constraint checks
                if (s.getRoomId() == roomId) return false;      // Room double-booking conflict
                if (s.getBlockSectionId() == blockId) return false; // Block section schedule overlap conflict
                if (s.getFacultyId() == facultyId) return false;    // Faculty double-booking conflict
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // New API used by the UI: writes into one schedule file, never deletes other schedules.
    // ------------------------------------------------------------------

    private static final int MAX_SEARCH_DAYS = 365;

    /** Convenience: use every room, course, block section and faculty member in the database. */
    public GenerationResult generateForAll(int scheduleId, LocalDate startDate, Set<DayOfWeek> allowedDays) {
        return generate(new ScheduleRequest(scheduleId, startDate, allowedDays,
                roomDAO.getAllRooms(), courseDAO.getAllCourses(),
                blockDAO.getAllBlockSections(), facultyDAO.getAllFaculty()));
    }

    /**
     * Earliest-fit placement. For each course it looks for the first (date, slot) on an allowed day where the
     * block section and faculty are free and a big-enough room is free (smallest sufficient room first).
     * Rooms run in parallel within a slot. Prefers days where the block has no other exam; falls back to
     * same-day if nothing else fits. Conflicts are checked only inside the target schedule file.
     *
     * LIMITATION (data model, not logic): the schema has no course-to-block or course-to-faculty relation.
     * As in the original demo, block = courses[i % blocks] and faculty = round-robin. Real assignment needs a
     * mapping table.
     */
    public GenerationResult generate(ScheduleRequest req) {
        if (req.courses().isEmpty() || req.rooms().isEmpty() || req.blocks().isEmpty() || req.faculty().isEmpty()) {
            return GenerationResult.failed("Select at least one room, course, block section and faculty member.");
        }
        if (req.allowedDays().isEmpty()) {
            return GenerationResult.failed("Select at least one exam day.");
        }

        Occupancy occ = new Occupancy();
        for (ExamSchedule s : scheduleDAO.getSchedulesByFile(req.scheduleId())) {
            if (s.getExamDate() != null && s.getTimeSlot() != null) {
                occ.mark(s.getExamDate(), s.getTimeSlot(), s.getRoomId(), s.getBlockSectionId(), s.getFacultyId());
            }
        }

        List<Room> rooms = new ArrayList<>(req.rooms());
        rooms.sort(Comparator.comparingInt(Room::getCapacity)); // best fit first

        List<ExamSchedule> toSave = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int facultyIndex = 0;

        for (int i = 0; i < req.courses().size(); i++) {
            Course course = req.courses().get(i);
            BlockSection block = req.blocks().get(i % req.blocks().size());
            Faculty faculty = req.faculty().get(facultyIndex % req.faculty().size());

            boolean roomFits = false;
            for (Room r : rooms) {
                if (r.getCapacity() >= block.getStudentCount()) { roomFits = true; break; }
            }
            if (!roomFits) {
                skipped.add(course.getCourseCode() + ": no selected room holds " + block.getStudentCount()
                        + " students (block " + block.getSectionName() + ")");
                continue;
            }

            Placement p = findSlot(req, occ, rooms, block, faculty, false);
            if (p == null) p = findSlot(req, occ, rooms, block, faculty, true);
            if (p == null) {
                skipped.add(course.getCourseCode() + ": no free slot within " + MAX_SEARCH_DAYS
                        + " days of the start date on the selected days");
                continue;
            }

            occ.mark(p.date(), p.slot(), p.room().getId(), block.getId(), faculty.getId());
            toSave.add(new ExamSchedule(0, course.getId(), p.room().getId(), block.getId(), faculty.getId(), p.date(), p.slot()));
            facultyIndex++;
        }

        if (!toSave.isEmpty() && !scheduleDAO.saveAll(toSave, req.scheduleId())) {
            return GenerationResult.failed("The schedule could not be saved to the database (nothing was stored). See the console for details.");
        }
        return new GenerationResult(req.courses().size(), toSave.size(), skipped, null);
    }

    private Placement findSlot(ScheduleRequest req, Occupancy occ, List<Room> rooms,
                               BlockSection block, Faculty faculty, boolean sameDayOk) {
        LocalDate last = req.startDate().plusDays(MAX_SEARCH_DAYS);
        for (LocalDate d = req.startDate(); !d.isAfter(last); d = d.plusDays(1)) {
            if (!req.allowedDays().contains(d.getDayOfWeek())) continue;
            if (!sameDayOk && occ.blockHasExamOn(d, block.getId())) continue;
            for (String slot : TimeSlots.ALL) {
                if (occ.blockBusy(d, slot, block.getId()) || occ.facultyBusy(d, slot, faculty.getId())) continue;
                for (Room r : rooms) {
                    if (r.getCapacity() >= block.getStudentCount() && !occ.roomBusy(d, slot, r.getId())) {
                        return new Placement(d, slot, r);
                    }
                }
            }
        }
        return null;
    }

    private record Placement(LocalDate date, String slot, Room room) {}

    /** Tracks what is already booked per (date, slot). */
    private static final class Occupancy {
        private final Map<String, Set<Integer>> rooms = new HashMap<>();
        private final Map<String, Set<Integer>> blocks = new HashMap<>();
        private final Map<String, Set<Integer>> faculty = new HashMap<>();
        private final Map<LocalDate, Set<Integer>> blocksByDate = new HashMap<>();

        private static String key(LocalDate d, String slot) { return d + "|" + slot; }

        boolean roomBusy(LocalDate d, String slot, int id) { return rooms.getOrDefault(key(d, slot), Set.of()).contains(id); }
        boolean blockBusy(LocalDate d, String slot, int id) { return blocks.getOrDefault(key(d, slot), Set.of()).contains(id); }
        boolean facultyBusy(LocalDate d, String slot, int id) { return faculty.getOrDefault(key(d, slot), Set.of()).contains(id); }
        boolean blockHasExamOn(LocalDate d, int blockId) { return blocksByDate.getOrDefault(d, Set.of()).contains(blockId); }

        void mark(LocalDate d, String slot, int roomId, int blockId, int facultyId) {
            String k = key(d, slot);
            rooms.computeIfAbsent(k, x -> new HashSet<>()).add(roomId);
            blocks.computeIfAbsent(k, x -> new HashSet<>()).add(blockId);
            faculty.computeIfAbsent(k, x -> new HashSet<>()).add(facultyId);
            blocksByDate.computeIfAbsent(d, x -> new HashSet<>()).add(blockId);
        }
    }

    // For testing purposes, do not touch
    /*
    public static void main(String[] args) {
        System.out.println("--- Starting UniMatrix Scheduling Engine Test ---");
        
        SchedulingEngine engine = new SchedulingEngine();
        
        // Define a start date for the exam week (e.g., today or a future date)
        LocalDate startDate = LocalDate.now().plusDays(7);
        
        System.out.println("Running scheduling algorithm starting from: " + startDate);
        boolean success = engine.generateMasterSchedule(startDate);
        
        if (success) {
            System.out.println("SUCCESS: Master schedule generated and saved to database!");
        } else {
            System.out.println("FAILURE: Schedule generation failed. Ensure your database has rooms, block sections, courses, and faculty records populated.");
        }
    }
    */
    
}