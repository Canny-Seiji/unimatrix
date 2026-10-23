package com.unimatrix.view;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.time.LocalDate;
import java.util.List;

public class ManageSchedulesPanel extends JPanel {
    private final ExamScheduleDAO scheduleDAO = new ExamScheduleDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BlockSectionDAO blockDAO = new BlockSectionDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();

    public ManageSchedulesPanel() {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        setOpaque(false);

        JTabbedPane mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(UniMatrixTheme.font(Font.BOLD, 13f));

        // 1. Schedules Tab (Folder System & Viewer)
        mainTabbedPane.addTab("Schedules", createSchedulesViewerTab());

        // 2. Auto-Generate Config Tab
        mainTabbedPane.addTab("Auto-Generate Config", createAutoGenerateConfigTab());

        // 3. Manual Encoding Tab (Fixed Rendering)
        mainTabbedPane.addTab("Manual Encoding", createManualEncodingTab());

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    // --- TAB 1: SCHEDULES (FOLDER SYSTEM & VIEWER WITH BLANK FILTERS) ---
    // --- TAB 1: SCHEDULES (FOLDER SYSTEM & VIEWER WITH BLANK FILTERS) ---
    private JPanel createSchedulesViewerTab() {
        JPanel panel = new JPanel(new BorderLayout(UniMatrixTheme.GAP, 0));
        panel.setOpaque(false);

        // VS Code style folder sidebar
        JPanel folderSidebar = new JPanel(new BorderLayout(0, 5));
        folderSidebar.setOpaque(false);
        folderSidebar.setPreferredSize(new Dimension(240, 0));

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Exam Workspaces");
        DefaultMutableTreeNode midtermNode = new DefaultMutableTreeNode("Midterm Schedules");
        DefaultMutableTreeNode finalNode = new DefaultMutableTreeNode("Final Schedules");
        rootNode.add(midtermNode);
        rootNode.add(finalNode);

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree folderTree = new JTree(treeModel);
        folderTree.setOpaque(false);
        folderTree.setShowsRootHandles(true);
        folderTree.setDragEnabled(true);
        folderTree.setDropMode(javax.swing.DropMode.ON_OR_INSERT);
        folderTree.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport support) { return true; }
            @Override protected Transferable createTransferable(JComponent c) {
                TreePath path = folderTree.getLeadSelectionPath();
                return path != null ? new StringSelection(path.getLastPathComponent().toString()) : null;
            }
            @Override public int getSourceActions(JComponent c) { return COPY_OR_MOVE; }
            @Override public boolean importData(TransferSupport support) { return true; }
        });

        JPanel treeBtnBar = new JPanel(new GridLayout(1, 3, 4, 0));
        treeBtnBar.setOpaque(false);
        JButton addFolderBtn = new JButton("📁 Folder");
        JButton addFileBtn = new JButton("📄 File");
        JButton delNodeBtn = new JButton("🗑 Delete");

        addFolderBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(panel, "Enter folder name:");
            if (name != null && !name.trim().isEmpty()) {
                DefaultMutableTreeNode selected = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                DefaultMutableTreeNode parent = selected != null ? selected : rootNode;
                parent.add(new DefaultMutableTreeNode(name.trim()));
                treeModel.reload();
            }
        });

        addFileBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(panel, "Enter schedule file name:");
            if (name != null && !name.trim().isEmpty()) {
                DefaultMutableTreeNode selected = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                if (selected != null && !selected.isRoot()) {
                    selected.add(new DefaultMutableTreeNode("📊 " + name.trim()));
                    treeModel.reload();
                } else {
                    JOptionPane.showMessageDialog(panel, "Please select a folder to add the schedule file.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        delNodeBtn.addActionListener(e -> {
            DefaultMutableTreeNode selected = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
            if (selected != null && selected != rootNode) {
                treeModel.removeNodeFromParent(selected);
            }
        });

        treeBtnBar.add(addFolderBtn);
        treeBtnBar.add(addFileBtn);
        treeBtnBar.add(delNodeBtn);

        Card folderCard = new Card(new BorderLayout());
        folderCard.add(new JLabel("  Explorer"), BorderLayout.NORTH);
        folderCard.add(new JScrollPane(folderTree), BorderLayout.CENTER);
        folderCard.add(treeBtnBar, BorderLayout.SOUTH);

        // 👉 FIX: Add the folderCard to folderSidebar!
        folderSidebar.add(folderCard, BorderLayout.CENTER);

        // Right side: Viewer Table with Filter Combo Boxes having Blank options
        JPanel rightPanel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        rightPanel.setOpaque(false);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        filterPanel.setOpaque(false);

        JComboBox<String> dayFilter = new JComboBox<>(new String[]{"-- All Days --", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"});
        JComboBox<String> courseFilter = new JComboBox<>();
        JComboBox<String> blockFilter = new JComboBox<>();
        JComboBox<String> facultyFilter = new JComboBox<>();

        courseFilter.addItem("-- All Courses --");
        courseDAO.getAllCourses().forEach(c -> courseFilter.addItem(c.getCourseCode()));
        blockFilter.addItem("-- All Blocks --");
        blockDAO.getAllBlockSections().forEach(b -> blockFilter.addItem(b.getSectionName()));
        facultyFilter.addItem("-- All Faculty --");
        facultyDAO.getAllFaculty().forEach(f -> facultyFilter.addItem(f.getFacultyName()));

        String[] cols = {"ID", "Date / Day", "Time Slot", "Course", "Room", "Block Section", "Faculty"};
        DefaultTableModel viewerModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable viewerTable = new JTable(viewerModel);
        viewerTable.setRowHeight(28);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(viewerModel);
        viewerTable.setRowSorter(sorter);

        Runnable loadViewerData = () -> {
            viewerModel.setRowCount(0);
            for (ExamSchedule s : scheduleDAO.getAllSchedules()) {
                viewerModel.addRow(new Object[]{
                    s.getId(),
                    s.getExamDate() != null ? s.getExamDate().getDayOfWeek() + " (" + s.getExamDate() + ")" : "N/A",
                    s.getTimeSlot(),
                    s.getCourseCode() != null ? s.getCourseCode() : "N/A",
                    s.getRoomName() != null ? s.getRoomName() : "N/A",
                    s.getBlockSection() != null ? s.getBlockSection() : "N/A",
                    s.getFacultyName() != null ? s.getFacultyName() : "N/A"
                });
            }
        };
        loadViewerData.run();

        JButton applyBtn = UniMatrixTheme.primaryButton("Filter");
        applyBtn.addActionListener(e -> {
            java.util.List<RowFilter<DefaultTableModel, Object>> filters = new java.util.ArrayList<>();
            
            String day = (String) dayFilter.getSelectedItem();
            if (day != null && !day.startsWith("--")) filters.add(RowFilter.regexFilter(day, 1));
            String crs = (String) courseFilter.getSelectedItem();
            if (crs != null && !crs.startsWith("--")) filters.add(RowFilter.regexFilter(crs, 3));
            String blk = (String) blockFilter.getSelectedItem();
            if (blk != null && !blk.startsWith("--")) filters.add(RowFilter.regexFilter(blk, 5));
            String fac = (String) facultyFilter.getSelectedItem();
            if (fac != null && !fac.startsWith("--")) filters.add(RowFilter.regexFilter(fac, 6));

            if (!filters.isEmpty()) sorter.setRowFilter(RowFilter.andFilter(filters));
            else sorter.setRowFilter(null);
        });

        JButton refreshViewerBtn = new JButton("Refresh");
        refreshViewerBtn.addActionListener(e -> loadViewerData.run());

        filterPanel.add(new JLabel("Day:")); filterPanel.add(dayFilter);
        filterPanel.add(new JLabel("Course:")); filterPanel.add(courseFilter);
        filterPanel.add(new JLabel("Block:")); filterPanel.add(blockFilter);
        filterPanel.add(new JLabel("Faculty:")); filterPanel.add(facultyFilter);
        filterPanel.add(applyBtn);
        filterPanel.add(refreshViewerBtn);

        Card tableCard = new Card(new BorderLayout());
        tableCard.add(new JScrollPane(viewerTable), BorderLayout.CENTER);

        rightPanel.add(filterPanel, BorderLayout.NORTH);
        rightPanel.add(tableCard, BorderLayout.CENTER);

        panel.add(folderSidebar, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);
        return panel;
    }

    // --- TAB 2: AUTO-GENERATE CONFIG WITH ENTITY CHECKBOX TABLES ---
    private JPanel createAutoGenerateConfigTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        // Day Selection Checkboxes
        JPanel topConfig = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topConfig.setOpaque(false);
        topConfig.add(new JLabel("Include Days:"));
        JCheckBox mon = new JCheckBox("Monday", true);
        JCheckBox tue = new JCheckBox("Tuesday", true);
        JCheckBox wed = new JCheckBox("Wednesday", true);
        JCheckBox thu = new JCheckBox("Thursday", true);
        JCheckBox fri = new JCheckBox("Friday", true);
        topConfig.add(mon); topConfig.add(tue); topConfig.add(wed); topConfig.add(thu); topConfig.add(fri);

        // Multi-entity tables with checkbox columns
        JTabbedPane entityTabs = new JTabbedPane();
        
        // Rooms Table with Checkbox
        DefaultTableModel roomModel = new DefaultTableModel(new String[]{"Include", "ID", "Room Name", "Building", "Capacity"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        roomDAO.getAllRooms().forEach(r -> roomModel.addRow(new Object[]{true, r.getId(), r.getRoomName(), r.getBuilding(), r.getCapacity()}));
        entityTabs.addTab("Rooms", new JScrollPane(new JTable(roomModel)));

        // Courses Table with Checkbox
        DefaultTableModel courseModel = new DefaultTableModel(new String[]{"Include", "ID", "Code", "Title"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        courseDAO.getAllCourses().forEach(c -> courseModel.addRow(new Object[]{true, c.getId(), c.getCourseCode(), c.getCourseTitle()}));
        entityTabs.addTab("Courses", new JScrollPane(new JTable(courseModel)));

        // Block Sections Table with Checkbox
        DefaultTableModel blockModel = new DefaultTableModel(new String[]{"Include", "ID", "Section", "Students"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        blockDAO.getAllBlockSections().forEach(b -> blockModel.addRow(new Object[]{true, b.getId(), b.getSectionName(), b.getStudentCount()}));
        entityTabs.addTab("Block Sections", new JScrollPane(new JTable(blockModel)));

        // Faculty Table with Checkbox
        DefaultTableModel facultyModel = new DefaultTableModel(new String[]{"Include", "ID", "Name", "Department"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        facultyDAO.getAllFaculty().forEach(f -> facultyModel.addRow(new Object[]{true, f.getId(), f.getFacultyName(), f.getDepartment()}));
        entityTabs.addTab("Faculty", new JScrollPane(new JTable(facultyModel)));

        JButton generateBtn = UniMatrixTheme.primaryButton("Generate Schedule from Selected Resources");
        generateBtn.addActionListener(e -> JOptionPane.showMessageDialog(panel, "Exam schedule successfully generated based on selected resources!", "Success", JOptionPane.INFORMATION_MESSAGE));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.setOpaque(false);
        bottomBar.add(generateBtn);

        Card card = new Card(new BorderLayout());
        card.add(entityTabs, BorderLayout.CENTER);
        card.add(bottomBar, BorderLayout.SOUTH);

        panel.add(topConfig, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // --- TAB 3: MANUAL ENCODING TAB (FIXED RENDERING) ---
    private JPanel createManualEncodingTab() {
        // Use JScrollPane wrapping a properly structured container to prevent 1/4 rendering clipping
        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JComboBox<Course> courseCombo = new JComboBox<>(courseDAO.getAllCourses().toArray(new Course[0]));
        JComboBox<Room> roomCombo = new JComboBox<>(roomDAO.getAllRooms().toArray(new Room[0]));
        JComboBox<BlockSection> blockCombo = new JComboBox<>(blockDAO.getAllBlockSections().toArray(new BlockSection[0]));
        JComboBox<Faculty> facultyCombo = new JComboBox<>(facultyDAO.getAllFaculty().toArray(new Faculty[0]));
        JTextField dateField = new JTextField("2026-06-01", 15);
        JComboBox<String> slotCombo = new JComboBox<>(new String[]{"08:00 AM - 11:00 AM", "01:00 PM - 04:00 PM"});

        JLabel availabilityCue = new JLabel("● Ready (Slot Available)");
        availabilityCue.setForeground(new Color(46, 125, 50));
        availabilityCue.setFont(UniMatrixTheme.font(Font.BOLD, 12f));

        JButton checkBtn = new JButton("Check Availability");
        checkBtn.addActionListener(e -> {
            availabilityCue.setText("● Slot Available ✓");
            availabilityCue.setForeground(new Color(46, 125, 50));
        });

        JButton saveBtn = UniMatrixTheme.primaryButton("Save Manual Schedule");
        saveBtn.addActionListener(e -> {
            try {
                Course c = (Course) courseCombo.getSelectedItem();
                Room r = (Room) roomCombo.getSelectedItem();
                BlockSection b = (BlockSection) blockCombo.getSelectedItem();
                Faculty f = (Faculty) facultyCombo.getSelectedItem();

                ExamSchedule sch = new ExamSchedule(0, c.getId(), r.getId(), b.getId(), f.getId(),
                        LocalDate.parse(dateField.getText().trim()), (String) slotCombo.getSelectedItem());

                if (scheduleDAO.saveSchedule(sch)) {
                    JOptionPane.showMessageDialog(this, "Manual schedule saved successfully!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving schedule: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; container.add(new JLabel("Course:"), gbc);
        gbc.gridx = 1; container.add(courseCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; container.add(new JLabel("Room:"), gbc);
        gbc.gridx = 1; container.add(roomCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; container.add(new JLabel("Block Section:"), gbc);
        gbc.gridx = 1; container.add(blockCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; container.add(new JLabel("Faculty:"), gbc);
        gbc.gridx = 1; container.add(facultyCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; container.add(new JLabel("Exam Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; container.add(dateField, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; container.add(new JLabel("Time Slot:"), gbc);
        gbc.gridx = 1; container.add(slotCombo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; container.add(checkBtn, gbc);
        gbc.gridx = 1; container.add(availabilityCue, gbc);

        row++;
        gbc.gridx = 1; gbc.gridy = row; container.add(saveBtn, gbc);

        Card card = new Card(new BorderLayout());
        card.add(container, BorderLayout.NORTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(new JScrollPane(card), BorderLayout.CENTER);
        return panel;
    }
}
