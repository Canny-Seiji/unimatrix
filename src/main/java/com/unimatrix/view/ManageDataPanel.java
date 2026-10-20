package com.unimatrix.view;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ManageDataPanel extends JPanel {
    private final RoomDAO roomDAO = new RoomDAO();
    private final BlockSectionDAO blockDAO = new BlockSectionDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();

    private DefaultTableModel roomTableModel;
    private DefaultTableModel blockTableModel;
    private DefaultTableModel courseTableModel;
    private DefaultTableModel facultyTableModel;

    public ManageDataPanel() {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        setOpaque(false);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UniMatrixTheme.font(Font.BOLD, 13f));
        tabbedPane.addTab("Rooms", createRoomTab());
        tabbedPane.addTab("Block Sections", createBlockTab());
        tabbedPane.addTab("Courses", createCourseTab());
        tabbedPane.addTab("Faculty", createFacultyTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // --- 1. ROOMS TAB ---
    private JPanel createRoomTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        roomTableModel = new DefaultTableModel(new String[]{"ID", "Room Name", "Building", "Capacity"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(roomTableModel);
        table.setRowHeight(28);
        loadRooms();

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        JTextField nameField = new JTextField(10);
        JTextField bldgField = new JTextField(10);
        JTextField capField = new JTextField(5);
        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Building:")); form.add(bldgField);
        form.add(new JLabel("Capacity:")); form.add(capField);

        JButton addBtn = UniMatrixTheme.primaryButton("Add Room");
        addBtn.addActionListener(e -> {
            try {
                if(roomDAO.addRoom(new Room(0, nameField.getText().trim(), bldgField.getText().trim(), Integer.parseInt(capField.getText().trim())))) {
                    loadRooms();
                    nameField.setText(""); bldgField.setText(""); capField.setText("");
                    JOptionPane.showMessageDialog(this, "Room added successfully!");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE); }
        });
        form.add(addBtn);

        Card card = new Card(new BorderLayout());
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void loadRooms() {
        roomTableModel.setRowCount(0);
        for (Room r : roomDAO.getAllRooms()) {
            roomTableModel.addRow(new Object[]{r.getId(), r.getRoomName(), r.getBuilding(), r.getCapacity()});
        }
    }

    // --- 2. BLOCK SECTIONS TAB ---
    private JPanel createBlockTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        blockTableModel = new DefaultTableModel(new String[]{"ID", "Section Name", "Student Count"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(blockTableModel);
        table.setRowHeight(28);
        loadBlocks();

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        JTextField nameField = new JTextField(12);
        JTextField countField = new JTextField(5);
        form.add(new JLabel("Section:")); form.add(nameField);
        form.add(new JLabel("Students:")); form.add(countField);

        JButton addBtn = UniMatrixTheme.primaryButton("Add Block Section");
        addBtn.addActionListener(e -> {
            try {
                if(blockDAO.addBlockSection(new BlockSection(0, nameField.getText().trim(), Integer.parseInt(countField.getText().trim())))) {
                    loadBlocks();
                    nameField.setText(""); countField.setText("");
                    JOptionPane.showMessageDialog(this, "Block Section added successfully!");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE); }
        });
        form.add(addBtn);

        Card card = new Card(new BorderLayout());
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void loadBlocks() {
        blockTableModel.setRowCount(0);
        for (BlockSection b : blockDAO.getAllBlockSections()) {
            blockTableModel.addRow(new Object[]{b.getId(), b.getSectionName(), b.getStudentCount()});
        }
    }

    // --- 3. COURSES TAB ---
    private JPanel createCourseTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        courseTableModel = new DefaultTableModel(new String[]{"ID", "Course Code", "Course Title", "Duration (Hrs)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(courseTableModel);
        table.setRowHeight(28);
        loadCourses();

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        JTextField codeField = new JTextField(8);
        JTextField titleField = new JTextField(12);
        JTextField durField = new JTextField(5);
        form.add(new JLabel("Code:")); form.add(codeField);
        form.add(new JLabel("Title:")); form.add(titleField);
        form.add(new JLabel("Duration:")); form.add(durField);

        JButton addBtn = UniMatrixTheme.primaryButton("Add Course");
        addBtn.addActionListener(e -> {
            try {
                if(courseDAO.addCourse(new Course(0, codeField.getText().trim(), titleField.getText().trim(), Float.parseFloat(durField.getText().trim())))) {
                    loadCourses();
                    codeField.setText(""); titleField.setText(""); durField.setText("");
                    JOptionPane.showMessageDialog(this, "Course added successfully!");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid input. Check duration number.", "Error", JOptionPane.ERROR_MESSAGE); }
        });
        form.add(addBtn);

        Card card = new Card(new BorderLayout());
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void loadCourses() {
        courseTableModel.setRowCount(0);
        for (Course c : courseDAO.getAllCourses()) {
            courseTableModel.addRow(new Object[]{c.getId(), c.getCourseCode(), c.getCourseTitle(), c.getDurationHours()});
        }
    }

    // --- 4. FACULTY TAB ---
    private JPanel createFacultyTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        // 1. Ensure columns match the data points being loaded
        facultyTableModel = new DefaultTableModel(new String[]{"ID", "Faculty Name", "Department"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(facultyTableModel);
        table.setRowHeight(28);
        loadFaculty();

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        form.setOpaque(false);
        JTextField nameField = new JTextField(12);
        JTextField deptField = new JTextField(10);
        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Department:")); form.add(deptField);

        JButton addBtn = UniMatrixTheme.primaryButton("Add Faculty");
        addBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String dept = deptField.getText().trim();
                if (!name.isEmpty() && !dept.isEmpty() && facultyDAO.addFaculty(new Faculty(0, name, dept))) {
                    loadFaculty(); // Refreshes table from DB
                    nameField.setText(""); deptField.setText("");
                    JOptionPane.showMessageDialog(this, "Faculty added successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) { 
                JOptionPane.showMessageDialog(this, "Error adding faculty: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
            }
        });
        form.add(addBtn);

        Card card = new Card(new BorderLayout());
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void loadFaculty() {
        facultyTableModel.setRowCount(0);
        for (Faculty f : facultyDAO.getAllFaculty()) {
            // 2. Ensure getFacultyName() and getDepartment() match your model
            facultyTableModel.addRow(new Object[]{f.getId(), f.getFacultyName(), f.getDepartment()});
        }
    }
}
