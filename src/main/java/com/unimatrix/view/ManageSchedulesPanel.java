package com.unimatrix.view;

import com.unimatrix.dao.*;
import com.unimatrix.model.*;
import com.unimatrix.service.*;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.components.WrapLayout;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ManageSchedulesPanel extends JPanel {
    private static final String ROOT_NAME = "Exam Workspaces";
    private static final String[] DAY_NAMES = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    private final ExamScheduleDAO scheduleDAO = new ExamScheduleDAO();
    private final ScheduleFolderDAO folderDAO = new ScheduleFolderDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final BlockSectionDAO blockDAO = new BlockSectionDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final SchedulingEngine engine = new SchedulingEngine();

    private DefaultTableModel viewerModel;
    private DefaultTreeModel treeModel;
    private JTree folderTree;
    private DefaultMutableTreeNode rootNode;
    private final JLabel viewerScope = new JLabel(" ");
    private JComboBox<FileChoice> manualTarget;
    private Runnable autoReload = () -> { };
    private Runnable manualReload = () -> { };

    /** Entry in the Manual Encoding "Save to" combo. file == null means "create a new schedule". */
    private record FileChoice(ScheduleFile file, String label) {
        @Override public String toString() { return label; }
    }

    private record Outcome(ScheduleFile file, GenerationResult result) { }

    public ManageSchedulesPanel() {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        setOpaque(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UniMatrixTheme.font(Font.BOLD, 13f));
        tabs.addTab("Schedules", createSchedulesViewerTab());
        tabs.addTab("Auto-Generate Config", createAutoGenerateConfigTab());
        tabs.addTab("Manual Encoding", createManualEncodingTab());
        // Entities and schedule files may have changed elsewhere: refresh when a tab is opened.
        tabs.addChangeListener(e -> {
            int i = tabs.getSelectedIndex();
            if (i == 1) autoReload.run();
            else if (i == 2) manualReload.run();
        });
        add(tabs, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    // TAB 1: explorer + viewer
    // ------------------------------------------------------------------
    private JComponent createSchedulesViewerTab() {
        rootNode = new DefaultMutableTreeNode(ROOT_NAME, true);
        treeModel = new DefaultTreeModel(rootNode, true); // empty folders keep the folder icon
        folderTree = new JTree(treeModel);
        folderTree.setOpaque(false);
        folderTree.setShowsRootHandles(true);
        folderTree.setRowHeight(24);
        loadTree();

        JButton addFolderBtn = UniMatrixTheme.secondaryButton("New Folder");
        JButton addScheduleBtn = UniMatrixTheme.secondaryButton("New Schedule");
        JButton delNodeBtn = UniMatrixTheme.secondaryButton("Delete");

        addFolderBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Folder name:", "New Folder", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.isBlank()) return;
            DefaultMutableTreeNode parent = selectedFolderOrRoot();
            ScheduleFolder created = folderDAO.createFolder(folderIdOf(parent), name.trim());
            if (created == null) {
                showError(this, "The folder could not be created. Check the database connection.");
                return;
            }
            addNode(parent, new DefaultMutableTreeNode(created, true));
        });

        addScheduleBtn.addActionListener(e -> {
            DefaultMutableTreeNode target = selectedFolderOrRoot();
            String[] options = {"Auto-Generated Schedule", "Manual Encoding"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Create a schedule in: " + folderLabel(target), "New Schedule",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 0) showAutoGenerateDialog(target);
            else if (choice == 1) showManualEncodingDialog(target);
        });

        delNodeBtn.addActionListener(e -> deleteSelected());

        JPanel treeBtnBar = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
        treeBtnBar.setOpaque(false);
        treeBtnBar.add(addFolderBtn);
        treeBtnBar.add(addScheduleBtn);
        treeBtnBar.add(delNodeBtn);

        JLabel explorerLabel = new JLabel("Explorer");
        explorerLabel.setFont(UniMatrixTheme.font(Font.BOLD, 14f));
        explorerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        Card folderCard = new Card(new BorderLayout());
        folderCard.add(explorerLabel, BorderLayout.NORTH);
        folderCard.add(new JScrollPane(folderTree), BorderLayout.CENTER);
        folderCard.add(treeBtnBar, BorderLayout.SOUTH);
        folderCard.setMinimumSize(new Dimension(220, 100));

        // Right side: filters + table
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
        viewerModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable viewerTable = createTable(viewerModel, 50, 190, 160, 100, 120, 130, 170);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(viewerModel);
        viewerTable.setRowSorter(sorter);
        loadViewerData();
        folderTree.addTreeSelectionListener(e -> loadViewerData());

        JButton applyBtn = UniMatrixTheme.primaryButton("Filter");
        applyBtn.addActionListener(e -> {
            java.util.List<RowFilter<DefaultTableModel, Object>> filters = new ArrayList<>();
            String day = (String) dayFilter.getSelectedItem();
            if (day != null && !day.startsWith("--")) filters.add(RowFilter.regexFilter("^" + day + " ", 1));
            addExact(filters, (String) courseFilter.getSelectedItem(), 3);
            addExact(filters, (String) blockFilter.getSelectedItem(), 5);
            addExact(filters, (String) facultyFilter.getSelectedItem(), 6);
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        });

        JButton refreshBtn = UniMatrixTheme.secondaryButton("Refresh");
        refreshBtn.addActionListener(e -> loadViewerData());

        JPanel filterPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 6));
        filterPanel.setOpaque(false);
        filterPanel.add(labeled("Day:", dayFilter));
        filterPanel.add(labeled("Course:", courseFilter));
        filterPanel.add(labeled("Block:", blockFilter));
        filterPanel.add(labeled("Faculty:", facultyFilter));
        filterPanel.add(applyBtn);
        filterPanel.add(refreshBtn);

        viewerScope.setFont(UniMatrixTheme.font(Font.BOLD, 13f));
        viewerScope.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        Card tableCard = new Card(new BorderLayout());
        tableCard.add(viewerScope, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(viewerTable), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        rightPanel.setOpaque(false);
        rightPanel.add(filterPanel, BorderLayout.NORTH);
        rightPanel.add(tableCard, BorderLayout.CENTER);
        rightPanel.setMinimumSize(new Dimension(420, 100));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, folderCard, rightPanel);
        split.setOpaque(false);
        split.setBorder(null);
        split.setResizeWeight(0.0);
        split.setDividerSize(10);
        split.setContinuousLayout(true);
        split.setDividerLocation(270);
        return split;
    }

    private void addExact(java.util.List<RowFilter<DefaultTableModel, Object>> filters, String value, int col) {
        if (value != null && !value.startsWith("--")) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(value) + "$", col));
        }
    }

    /** Shows the exams of the selected schedule, or of every schedule under the selected folder; root/none = all. */
    private void loadViewerData() {
        if (viewerModel == null) return;
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        List<ExamSchedule> rows;
        String scope;
        if (sel == null || sel == rootNode) {
            rows = scheduleDAO.getAllSchedules();
            scope = "All schedules";
        } else {
            rows = scheduleDAO.getSchedulesByFiles(fileIdsUnder(sel));
            scope = (sel.getUserObject() instanceof ScheduleFile ? "Schedule: " : "Folder: ") + sel.getUserObject();
        }
        viewerScope.setText(scope + "  (" + rows.size() + " exams)");
        viewerModel.setRowCount(0);
        for (ExamSchedule s : rows) {
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
    }

    // ------------------------------------------------------------------
    // Folder-tree helpers (tree mirrors schedule_folders / schedule_files)
    // ------------------------------------------------------------------
    private void loadTree() {
        rootNode.removeAllChildren();
        List<ScheduleFolder> folders = folderDAO.getAllFolders();
        List<ScheduleFile> files = folderDAO.getAllFiles();

        Map<Integer, DefaultMutableTreeNode> nodes = new HashMap<>();
        for (ScheduleFolder f : folders) nodes.put(f.getId(), new DefaultMutableTreeNode(f, true));
        for (ScheduleFolder f : folders) {
            DefaultMutableTreeNode parent = f.getParentId() == null ? rootNode : nodes.getOrDefault(f.getParentId(), rootNode);
            parent.add(nodes.get(f.getId()));
        }
        for (ScheduleFile f : files) {
            DefaultMutableTreeNode parent = f.getFolderId() == null ? rootNode : nodes.getOrDefault(f.getFolderId(), rootNode);
            parent.add(new DefaultMutableTreeNode(f, false));
        }
        treeModel.reload();
        folderTree.expandPath(new TreePath(rootNode.getPath()));
    }

    private DefaultMutableTreeNode selectedFolderOrRoot() {
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        if (sel == null) return rootNode;
        return sel.getAllowsChildren() ? sel : (DefaultMutableTreeNode) sel.getParent();
    }

    /** Database id of a folder node; null for the root workspace. */
    private Integer folderIdOf(DefaultMutableTreeNode folderNode) {
        return folderNode.getUserObject() instanceof ScheduleFolder f ? f.getId() : null;
    }

    private String folderLabel(DefaultMutableTreeNode folder) {
        return folder == rootNode ? ROOT_NAME + " (root)" : String.valueOf(folder.getUserObject());
    }

    private List<Integer> fileIdsUnder(DefaultMutableTreeNode node) {
        List<Integer> ids = new ArrayList<>();
        for (TreeNode t : Collections.list(node.depthFirstEnumeration())) {
            Object o = ((DefaultMutableTreeNode) t).getUserObject();
            if (o instanceof ScheduleFile f) ids.add(f.getId());
        }
        return ids;
    }

    private String pathLabel(DefaultMutableTreeNode node) {
        TreeNode[] path = node.getPath();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < path.length; i++) {
            if (i > 1) sb.append(" / ");
            sb.append(((DefaultMutableTreeNode) path[i]).getUserObject());
        }
        return sb.toString();
    }

    /** Inserts without collapsing the rest of the tree. */
    private void addNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
        treeModel.insertNodeInto(child, parent, parent.getChildCount());
        folderTree.expandPath(new TreePath(parent.getPath()));
        folderTree.scrollPathToVisible(new TreePath(child.getPath()));
    }

    private void deleteSelected() {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        if (selected == null || selected == rootNode) {
            JOptionPane.showMessageDialog(this, "Select a folder or schedule to delete. The root workspace cannot be deleted.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Object obj = selected.getUserObject();
        int files = fileIdsUnder(selected).size();
        String what = obj instanceof ScheduleFolder
                ? "the folder '" + obj + "' and everything inside it (" + files + " schedule" + (files == 1 ? "" : "s") + ")"
                : "the schedule '" + obj + "'";
        int ok = JOptionPane.showConfirmDialog(this,
                "Permanently delete " + what + "?\nAll exam entries inside are deleted too. This cannot be undone.",
                "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        boolean done = obj instanceof ScheduleFolder f ? folderDAO.deleteFolder(f.getId())
                : obj instanceof ScheduleFile sf && folderDAO.deleteFile(sf.getId());
        if (!done) {
            showError(this, "The delete failed. Check the database connection.");
            return;
        }
        treeModel.removeNodeFromParent(selected);
        loadViewerData();
        refreshManualTargets();
    }

    // ------------------------------------------------------------------
    // Auto-generation (shared by the dialog and the config tab)
    // ------------------------------------------------------------------

    /**
     * Creates a schedule file, runs the job off the EDT, then keeps the file only if exams were scheduled.
     * onSuccess runs on the EDT after a file was kept.
     */
    private void runGeneration(Component owner, AbstractButton trigger, DefaultMutableTreeNode folder, String name,
                               Function<Integer, GenerationResult> job, Runnable onSuccess) {
        Integer folderId = folderIdOf(folder);
        trigger.setEnabled(false);
        new SwingWorker<Outcome, Void>() {
            @Override protected Outcome doInBackground() {
                ScheduleFile file = folderDAO.createFile(folderId, name);
                if (file == null) {
                    return new Outcome(null, GenerationResult.failed("Could not create the schedule in the database."));
                }
                GenerationResult res;
                try {
                    res = job.apply(file.getId());
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    res = GenerationResult.failed("Unexpected error: " + ex);
                }
                if (!res.ok() || res.scheduled() == 0) { // never leave an empty schedule behind
                    folderDAO.deleteFile(file.getId());
                    file = null;
                }
                return new Outcome(file, res);
            }

            @Override protected void done() {
                trigger.setEnabled(true);
                try {
                    Outcome o = get();
                    if (o.file() != null) addNode(folder, new DefaultMutableTreeNode(o.file(), false));
                    loadViewerData();
                    refreshManualTargets();
                    showResult(owner, o, folder);
                    if (o.file() != null) onSuccess.run();
                } catch (InterruptedException | ExecutionException ex) {
                    showError(owner, "Generation failed: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showResult(Component owner, Outcome o, DefaultMutableTreeNode folder) {
        GenerationResult r = o.result();
        if (!r.ok()) {
            showError(owner, r.error());
            return;
        }
        String head = o.file() != null
                ? "Scheduled " + r.scheduled() + " of " + r.totalCourses() + " courses.<br>Saved as '"
                    + esc(o.file().getName()) + "' in " + esc(folderLabel(folder)) + "."
                : "No exams could be scheduled, so nothing was saved.";
        if (r.skipped().isEmpty()) {
            JOptionPane.showMessageDialog(owner, "<html>" + head + "</html>", "Auto-Generation", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTextArea area = new JTextArea(String.join("\n", r.skipped()), 8, 44);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(owner, new Object[]{
                new JLabel("<html>" + head + "<br><br>Skipped (" + r.skipped().size() + "):</html>"),
                new JScrollPane(area)
        }, "Auto-Generation", JOptionPane.WARNING_MESSAGE);
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void showError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private LocalDate parseDate(Component parent, String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            showError(parent, "Date must be in YYYY-MM-DD format.");
            return null;
        }
    }

    private String defaultStart() { return LocalDate.now().plusDays(7).toString(); }

    private String nameOrDefault(JTextField f, String fallback) {
        String s = f.getText().trim();
        return s.isEmpty() ? fallback : s;
    }

    private JCheckBox[] makeDayBoxes() {
        JCheckBox[] boxes = new JCheckBox[DAY_NAMES.length];
        for (int i = 0; i < boxes.length; i++) boxes[i] = new JCheckBox(DAY_NAMES[i], true);
        return boxes;
    }

    private JPanel dayBoxesPanel(JCheckBox[] boxes) {
        JPanel p = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 2));
        p.setOpaque(false);
        for (JCheckBox b : boxes) p.add(b);
        return p;
    }

    private Set<DayOfWeek> selectedDays(JCheckBox[] boxes) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (int i = 0; i < boxes.length; i++) if (boxes[i].isSelected()) days.add(DayOfWeek.of(i + 1)); // Mon=1
        return days;
    }

    private void showAutoGenerateDialog(DefaultMutableTreeNode targetFolder) {
        JDialog dialog = newDialog("Auto-Generate Schedule");

        JPanel form = formPanel();
        JCheckBox[] dayBoxes = makeDayBoxes();
        JTextField startField = new JTextField(defaultStart(), 12);
        JTextField nameField = new JTextField("Generated_Schedule_" + System.currentTimeMillis() % 10000, 20);

        addRow(form, 0, "Saving to:", new JLabel(folderLabel(targetFolder)));
        addRow(form, 1, "Exam days:", dayBoxesPanel(dayBoxes));
        addRow(form, 2, "Start date (YYYY-MM-DD):", startField);
        addRow(form, 3, "Schedule name:", nameField);
        addRow(form, 4, "Uses:", new JLabel("All rooms, courses, block sections and faculty"));

        JButton generateBtn = UniMatrixTheme.primaryButton("Generate & Save");
        JButton cancelBtn = UniMatrixTheme.secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        generateBtn.addActionListener(e -> {
            LocalDate start = parseDate(dialog, startField.getText());
            if (start == null) return;
            Set<DayOfWeek> days = selectedDays(dayBoxes);
            if (days.isEmpty()) { showError(dialog, "Select at least one exam day."); return; }
            String name = nameOrDefault(nameField, "Generated_Schedule");
            runGeneration(dialog, generateBtn, targetFolder, name,
                    id -> engine.generateForAll(id, start, days), dialog::dispose);
        });
        showDialog(dialog, form, generateBtn, cancelBtn);
    }

    // ------------------------------------------------------------------
    // Manual encoding
    // ------------------------------------------------------------------
    private void showManualEncodingDialog(DefaultMutableTreeNode targetFolder) {
        JDialog dialog = newDialog("Manual Schedule Encoding");
        ManualForm mf = new ManualForm("Manual_Exam_");

        JPanel form = formPanel();
        addRow(form, 0, "Saving to:", new JLabel(folderLabel(targetFolder)));
        mf.addTo(form, 1);

        JButton saveBtn = UniMatrixTheme.primaryButton("Save Schedule");
        JButton cancelBtn = UniMatrixTheme.secondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dialog.dispose());
        saveBtn.addActionListener(e -> {
            if (saveManualAsNew(dialog, mf, targetFolder)) dialog.dispose();
        });
        showDialog(dialog, form, saveBtn, cancelBtn);
    }

    /** New schedule file in the folder, containing this one exam. */
    private boolean saveManualAsNew(Component parent, ManualForm mf, DefaultMutableTreeNode folder) {
        ExamSchedule exam;
        try {
            exam = mf.build();
        } catch (Exception ex) {
            showError(parent, ex.getMessage());
            return false;
        }
        ScheduleFile file = folderDAO.createFile(folderIdOf(folder), mf.scheduleName());
        if (file == null) {
            showError(parent, "The schedule could not be created. Check the database connection.");
            return false;
        }
        exam.setScheduleId(file.getId());
        if (!scheduleDAO.saveSchedule(exam)) {
            folderDAO.deleteFile(file.getId());
            showError(parent, "The exam could not be saved.");
            return false;
        }
        addNode(folder, new DefaultMutableTreeNode(file, false));
        loadViewerData();
        refreshManualTargets();
        JOptionPane.showMessageDialog(parent, "Saved '" + file.getName() + "' in " + folderLabel(folder) + ".");
        return true;
    }

    /** Adds one exam to an existing schedule file, refusing hard-constraint conflicts. */
    private boolean saveManualInto(Component parent, ManualForm mf, ScheduleFile file) {
        try {
            ExamSchedule exam = mf.build();
            exam.setScheduleId(file.getId());
            List<String> conflicts = scheduleDAO.findConflicts(file.getId(), exam);
            if (!conflicts.isEmpty()) {
                showError(parent, "Not saved. Conflicts with an existing exam in '" + file.getName() + "':\n- "
                        + String.join("\n- ", conflicts));
                return false;
            }
            if (!scheduleDAO.saveSchedule(exam)) {
                showError(parent, "The exam could not be saved.");
                return false;
            }
            loadViewerData();
            JOptionPane.showMessageDialog(parent, "Exam added to '" + file.getName() + "'.");
            return true;
        } catch (Exception ex) {
            showError(parent, ex.getMessage());
            return false;
        }
    }

    private void refreshManualTargets() {
        if (manualTarget == null) return;
        FileChoice prev = (FileChoice) manualTarget.getSelectedItem();
        Integer prevId = prev != null && prev.file() != null ? prev.file().getId() : null;

        DefaultComboBoxModel<FileChoice> model = new DefaultComboBoxModel<>();
        FileChoice newChoice = new FileChoice(null, "[ New schedule in " + ROOT_NAME + " ]");
        model.addElement(newChoice);
        FileChoice toSelect = newChoice;
        for (TreeNode t : Collections.list(rootNode.preorderEnumeration())) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) t;
            if (n.getUserObject() instanceof ScheduleFile f) {
                FileChoice c = new FileChoice(f, pathLabel(n));
                model.addElement(c);
                if (prevId != null && prevId == f.getId()) toSelect = c;
            }
        }
        manualTarget.setModel(model);
        manualTarget.setSelectedItem(toSelect);
    }

    // ------------------------------------------------------------------
    // TAB 2: auto-generate config (always saves at the root workspace)
    // ------------------------------------------------------------------
    private JPanel createAutoGenerateConfigTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        JCheckBox[] dayBoxes = makeDayBoxes();
        JTextField startField = new JTextField(defaultStart(), 10);
        JTextField nameField = new JTextField("Generated_Schedule_" + System.currentTimeMillis() % 10000, 16);

        JPanel topConfig = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 5));
        topConfig.setOpaque(false);
        topConfig.add(labeled("Schedule name:", nameField));
        topConfig.add(labeled("Start date (YYYY-MM-DD):", startField));
        topConfig.add(labeled("Exam days:", dayBoxesPanel(dayBoxes)));

        final List<Room> rooms = new ArrayList<>();
        final List<Course> courses = new ArrayList<>();
        final List<BlockSection> blocks = new ArrayList<>();
        final List<Faculty> faculty = new ArrayList<>();

        DefaultTableModel roomModel = checkModel("Include", "ID", "Room Name", "Building", "Capacity");
        DefaultTableModel courseModel = checkModel("Include", "ID", "Code", "Title");
        DefaultTableModel blockModel = checkModel("Include", "ID", "Section", "Students");
        DefaultTableModel facultyModel = checkModel("Include", "ID", "Name", "Department");

        Runnable reload = () -> {
            rooms.clear(); rooms.addAll(roomDAO.getAllRooms());
            courses.clear(); courses.addAll(courseDAO.getAllCourses());
            blocks.clear(); blocks.addAll(blockDAO.getAllBlockSections());
            faculty.clear(); faculty.addAll(facultyDAO.getAllFaculty());
            roomModel.setRowCount(0);
            for (Room r : rooms) roomModel.addRow(new Object[]{true, r.getId(), r.getRoomName(), r.getBuilding(), r.getCapacity()});
            courseModel.setRowCount(0);
            for (Course c : courses) courseModel.addRow(new Object[]{true, c.getId(), c.getCourseCode(), c.getCourseTitle()});
            blockModel.setRowCount(0);
            for (BlockSection b : blocks) blockModel.addRow(new Object[]{true, b.getId(), b.getSectionName(), b.getStudentCount()});
            facultyModel.setRowCount(0);
            for (Faculty f : faculty) facultyModel.addRow(new Object[]{true, f.getId(), f.getFacultyName(), f.getDepartment()});
        };
        reload.run();
        autoReload = reload;

        JTabbedPane entityTabs = new JTabbedPane();
        entityTabs.addTab("Rooms", new JScrollPane(createTable(roomModel, 70, 60, 180, 160, 90)));
        entityTabs.addTab("Courses", new JScrollPane(createTable(courseModel, 70, 60, 120, 320)));
        entityTabs.addTab("Block Sections", new JScrollPane(createTable(blockModel, 70, 60, 180, 100)));
        entityTabs.addTab("Faculty", new JScrollPane(createTable(facultyModel, 70, 60, 220, 220)));

        JButton generateBtn = UniMatrixTheme.primaryButton("Run Auto-Generation");
        generateBtn.addActionListener(e -> {
            LocalDate start = parseDate(panel, startField.getText());
            if (start == null) return;
            Set<DayOfWeek> days = selectedDays(dayBoxes);
            if (days.isEmpty()) { showError(panel, "Select at least one exam day."); return; }
            List<Room> selRooms = checked(roomModel, rooms);
            List<Course> selCourses = checked(courseModel, courses);
            List<BlockSection> selBlocks = checked(blockModel, blocks);
            List<Faculty> selFaculty = checked(facultyModel, faculty);
            String name = nameOrDefault(nameField, "Generated_Schedule");
            runGeneration(panel, generateBtn, rootNode, name,
                    id -> engine.generate(new ScheduleRequest(id, start, days, selRooms, selCourses, selBlocks, selFaculty)),
                    () -> nameField.setText("Generated_Schedule_" + System.currentTimeMillis() % 10000));
        });

        JButton reloadBtn = UniMatrixTheme.secondaryButton("Reload from database");
        reloadBtn.addActionListener(e -> reload.run());

        JPanel bottomBar = new JPanel(new WrapLayout(FlowLayout.RIGHT, 8, 8));
        bottomBar.setOpaque(false);
        bottomBar.add(reloadBtn);
        bottomBar.add(generateBtn);

        Card card = new Card(new BorderLayout());
        card.add(entityTabs, BorderLayout.CENTER);
        card.add(bottomBar, BorderLayout.SOUTH);

        panel.add(topConfig, BorderLayout.NORTH);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel checkModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : Object.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
    }

    /** Items whose Include checkbox is ticked (table has no sorter, so row index == list index). */
    private <T> List<T> checked(DefaultTableModel model, List<T> items) {
        List<T> out = new ArrayList<>();
        for (int i = 0; i < items.size() && i < model.getRowCount(); i++) {
            if (Boolean.TRUE.equals(model.getValueAt(i, 0))) out.add(items.get(i));
        }
        return out;
    }

    // ------------------------------------------------------------------
    // TAB 3: manual encoding
    // ------------------------------------------------------------------
    private JPanel createManualEncodingTab() {
        ManualForm mf = new ManualForm("Manual_Exam_");
        JPanel form = formPanel();

        manualTarget = new JComboBox<>();
        refreshManualTargets();
        manualTarget.addActionListener(e -> {
            FileChoice c = (FileChoice) manualTarget.getSelectedItem();
            mf.fileName.setEnabled(c == null || c.file() == null); // name only matters for a new schedule
        });
        addRow(form, 0, "Save to:", manualTarget);
        int row = mf.addTo(form, 1);

        JLabel availabilityCue = new JLabel("● Not checked yet");
        availabilityCue.setForeground(UniMatrixTheme.TEXT_MUTED);
        availabilityCue.setFont(UniMatrixTheme.font(Font.BOLD, 12f));
        JButton checkBtn = UniMatrixTheme.secondaryButton("Check Availability");
        checkBtn.addActionListener(e -> {
            try {
                ExamSchedule exam = mf.build();
                FileChoice c = (FileChoice) manualTarget.getSelectedItem();
                List<String> conflicts = (c == null || c.file() == null)
                        ? List.of() : scheduleDAO.findConflicts(c.file().getId(), exam);
                if (conflicts.isEmpty()) {
                    availabilityCue.setText("● Slot available ✓");
                    availabilityCue.setForeground(UniMatrixTheme.SUCCESS);
                } else {
                    availabilityCue.setText("● " + String.join("; ", conflicts));
                    availabilityCue.setForeground(UniMatrixTheme.DANGER);
                }
            } catch (Exception ex) {
                availabilityCue.setText("● " + ex.getMessage());
                availabilityCue.setForeground(UniMatrixTheme.DANGER);
            }
        });
        JPanel availabilityRow = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 0));
        availabilityRow.setOpaque(false);
        availabilityRow.add(checkBtn);
        availabilityRow.add(availabilityCue);
        addRow(form, row, "Availability:", availabilityRow);

        JButton saveBtn = UniMatrixTheme.primaryButton("Save Manual Schedule");
        saveBtn.addActionListener(e -> {
            FileChoice c = (FileChoice) manualTarget.getSelectedItem();
            boolean saved = (c == null || c.file() == null)
                    ? saveManualAsNew(this, mf, rootNode)
                    : saveManualInto(this, mf, c.file());
            if (saved) {
                availabilityCue.setText("● Not checked yet");
                availabilityCue.setForeground(UniMatrixTheme.TEXT_MUTED);
            }
        });

        manualReload = () -> { mf.reloadEntities(); refreshManualTargets(); };

        JPanel bottomBar = new JPanel(new WrapLayout(FlowLayout.RIGHT, 8, 8));
        bottomBar.setOpaque(false);
        bottomBar.add(saveBtn);

        Card card = new Card(new BorderLayout());
        card.add(scrollable(form), BorderLayout.CENTER);
        card.add(bottomBar, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    /** Exam input fields shared by the Manual Encoding tab and the manual dialog. */
    private final class ManualForm {
        final JComboBox<Course> course = new JComboBox<>(courseDAO.getAllCourses().toArray(new Course[0]));
        final JComboBox<Room> room = new JComboBox<>(roomDAO.getAllRooms().toArray(new Room[0]));
        final JComboBox<BlockSection> block = new JComboBox<>(blockDAO.getAllBlockSections().toArray(new BlockSection[0]));
        final JComboBox<Faculty> faculty = new JComboBox<>(facultyDAO.getAllFaculty().toArray(new Faculty[0]));
        final JTextField date = new JTextField("2026-06-01", 12);
        final JComboBox<String> slot = new JComboBox<>(TimeSlots.asArray());
        final JTextField fileName;

        ManualForm(String namePrefix) {
            fileName = new JTextField(namePrefix + System.currentTimeMillis() % 10000, 12);
        }

        void reloadEntities() {
            course.setModel(new DefaultComboBoxModel<>(courseDAO.getAllCourses().toArray(new Course[0])));
            room.setModel(new DefaultComboBoxModel<>(roomDAO.getAllRooms().toArray(new Room[0])));
            block.setModel(new DefaultComboBoxModel<>(blockDAO.getAllBlockSections().toArray(new BlockSection[0])));
            faculty.setModel(new DefaultComboBoxModel<>(facultyDAO.getAllFaculty().toArray(new Faculty[0])));
        }

        /** Adds the rows starting at startRow; returns the next free row. */
        int addTo(JPanel form, int startRow) {
            int r = startRow;
            addRow(form, r++, "Course:", course);
            addRow(form, r++, "Room:", room);
            addRow(form, r++, "Block Section:", block);
            addRow(form, r++, "Faculty:", faculty);
            addRow(form, r++, "Exam Date (YYYY-MM-DD):", date);
            addRow(form, r++, "Time Slot:", slot);
            addRow(form, r++, "Schedule name:", fileName);
            return r;
        }

        String scheduleName() {
            String s = fileName.getText().trim();
            return s.isEmpty() ? "Manual_Schedule" : s;
        }

        ExamSchedule build() {
            Course c = (Course) course.getSelectedItem();
            Room r = (Room) room.getSelectedItem();
            BlockSection b = (BlockSection) block.getSelectedItem();
            Faculty f = (Faculty) faculty.getSelectedItem();
            if (c == null || r == null || b == null || f == null) {
                throw new IllegalStateException("Select a course, room, block section and faculty first.");
            }
            LocalDate d;
            try {
                d = LocalDate.parse(date.getText().trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Date must be in YYYY-MM-DD format.");
            }
            return new ExamSchedule(0, c.getId(), r.getId(), b.getId(), f.getId(), d, (String) slot.getSelectedItem());
        }
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    /** Keeps column widths (no truncation) and scrolls horizontally when the window is too narrow. */
    private JTable createTable(TableModel model, int... widths) {
        JTable t = new JTable(model) {
            @Override public boolean getScrollableTracksViewportWidth() {
                Container p = getParent();
                return p != null && getPreferredSize().width < p.getWidth();
            }
        };
        t.setRowHeight(28);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        for (int i = 0; i < widths.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        return t;
    }

    private JPanel labeled(String text, JComponent field) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.add(new JLabel(text));
        p.add(field);
        return p;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return form;
    }

    private void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.insets = new Insets(5, 5, 5, 5);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        form.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, g);
    }

    private JScrollPane scrollable(JComponent content) {
        TrackingPanel holder = new TrackingPanel(new BorderLayout());
        holder.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(holder);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JDialog newDialog(String title) {
        return new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
    }

    /** Content scrolls; the button bar is pinned at the bottom so buttons never disappear. */
    private void showDialog(JDialog d, JComponent content, JButton... buttons) {
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.RIGHT, 8, 8));
        for (JButton b : buttons) bar.add(b);
        d.getContentPane().setLayout(new BorderLayout());
        d.add(scrollable(content), BorderLayout.CENTER);
        d.add(bar, BorderLayout.SOUTH);
        d.pack();
        Dimension size = d.getSize();
        d.setSize(Math.max(size.width, 460), size.height);
        d.setMinimumSize(new Dimension(360, Math.min(size.height, 260)));
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private static final class TrackingPanel extends JPanel implements Scrollable {
        TrackingPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return o == SwingConstants.VERTICAL ? r.height : r.width;
        }
        @Override public boolean getScrollableTracksViewportWidth() {
            Container p = getParent();
            return p != null && p.getWidth() >= getPreferredSize().width;
        }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}