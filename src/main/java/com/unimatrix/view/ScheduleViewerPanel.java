package com.unimatrix.view;

import com.unimatrix.dao.ExamScheduleDAO;
import com.unimatrix.model.ExamSchedule;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ScheduleViewerPanel extends JPanel {
    private final ExamScheduleDAO scheduleDAO = new ExamScheduleDAO();
    private DefaultTableModel tableModel;
    private JTable table;

    public ScheduleViewerPanel() {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        setOpaque(false);

        // Top Toolbar / Filter Header
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Generated Exam Schedules");
        titleLabel.setFont(UniMatrixTheme.font(Font.BOLD, 16f));
        titleLabel.setForeground(UniMatrixTheme.TEXT);

        JButton refreshBtn = UniMatrixTheme.primaryButton("Refresh Schedules");
        refreshBtn.addActionListener(e -> loadSchedules());

        topPanel.add(titleLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(refreshBtn);

        // Table Setup
        String[] columns = {"ID", "Course", "Room", "Block Section", "Faculty", "Time Slot"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UniMatrixTheme.font(Font.PLAIN, 12f));

        loadSchedules();

        // Card Container
        Card card = new Card(new BorderLayout());
        card.add(new JScrollPane(table), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(card, BorderLayout.CENTER);
    }

    private void loadSchedules() {
        tableModel.setRowCount(0);
        try {
            List<ExamSchedule> schedules = scheduleDAO.getAllSchedules();
            if (schedules != null) {
                for (ExamSchedule s : schedules) {
                    tableModel.addRow(new Object[]{
                        s.getId(),
                        s.getCourseCode() != null ? s.getCourseCode() : "N/A",
                        s.getRoomName() != null ? s.getRoomName() : "N/A",
                        s.getBlockSection() != null ? s.getBlockSection() : "N/A",
                        s.getFacultyName() != null ? s.getFacultyName() : "N/A",
                        s.getTimeSlot() != null ? s.getTimeSlot() : "N/A"
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading schedules: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
