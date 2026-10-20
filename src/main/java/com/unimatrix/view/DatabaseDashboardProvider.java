package com.unimatrix.view;

import com.unimatrix.dao.*;
import com.unimatrix.model.ExamSchedule;
import java.util.List;

public class DatabaseDashboardProvider implements DashboardDataProvider {
    private final RoomDAO roomDAO = new RoomDAO();
    private final BlockSectionDAO blockDAO = new BlockSectionDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final ExamScheduleDAO scheduleDAO = new ExamScheduleDAO();

    @Override
    public DashboardStats load() throws Exception {
        int roomsCount = roomDAO.getAllRooms().size();
        int blocksCount = blockDAO.getAllBlockSections().size();
        int facultyCount = facultyDAO.getAllFaculty().size();
        int coursesCount = courseDAO.getAllCourses().size();

        List<ExamSchedule> schedules = scheduleDAO.getAllSchedules();
        String lastSummary = null;
        if (!schedules.isEmpty()) {
            lastSummary = "Generated " + schedules.size() + " slots successfully. Latest date: " 
                        + schedules.get(schedules.size() - 1).getExamDate();
        }

        return new DashboardStats(roomsCount, blocksCount, facultyCount, coursesCount, lastSummary);
    }
}