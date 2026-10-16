package com.unimatrix.service;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;

import java.time.LocalDate;
import java.util.List;

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