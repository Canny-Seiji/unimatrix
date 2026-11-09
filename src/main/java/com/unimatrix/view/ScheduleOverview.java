package com.unimatrix.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.unimatrix.dao.ExamScheduleDAO;
import com.unimatrix.model.ExamSchedule;
import com.unimatrix.service.ExamTimes;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

/** Read-only calendar and timeline using the existing schedule query. */
final class ScheduleOverview {
    private static final Color[] COLORS = {
        new Color(0x3B82F6), new Color(0x10B981), new Color(0x8B5CF6), new Color(0x0D9488)
    };
    private final Card calendar = new Card(new BorderLayout(0, 12));
    private final Card timeline = new Card(new BorderLayout(0, 12));
    private final JPanel grid = new JPanel(new GridBagLayout());
    private final JPanel activity = new JPanel();
    private final JLabel month = new JLabel();
    private final JLabel state = UniMatrixTheme.muted("No schedules loaded.");
    private LocalDate week = monday(LocalDate.now());
    private List<ExamSchedule> exams = List.of();
    private boolean loading;
    private boolean initialized;

    ScheduleOverview() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navigation.setOpaque(false);
        JButton previous = navButton("<", "Previous week");
        JButton next = navButton(">", "Next week");
        previous.addActionListener(e -> { week = week.minusWeeks(1); render(); });
        next.addActionListener(e -> { week = week.plusWeeks(1); render(); });
        month.setFont(UniMatrixTheme.font(Font.BOLD, 15f));
        navigation.add(previous); navigation.add(month); navigation.add(next);
        JLabel badge = UniMatrixTheme.muted("Week View");
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UniMatrixTheme.BORDER), new EmptyBorder(4, 10, 4, 10)));
        header.add(navigation, BorderLayout.WEST); header.add(badge, BorderLayout.EAST);
        calendar.add(header, BorderLayout.NORTH);
        grid.setBackground(Color.WHITE);
        JScrollPane calendarScroll = new JScrollPane(grid);
        calendarScroll.setBorder(BorderFactory.createLineBorder(UniMatrixTheme.BORDER));
        calendarScroll.getVerticalScrollBar().setUnitIncrement(24);
        calendar.add(calendarScroll, BorderLayout.CENTER);
        calendar.add(state, BorderLayout.SOUTH);
        timeline.setPreferredSize(new Dimension(300, 0));
        JLabel heading = new JLabel("Schedule Timeline & Activity");
        heading.setFont(UniMatrixTheme.font(Font.BOLD, 15f));
        timeline.add(heading, BorderLayout.NORTH);
        activity.setLayout(new BoxLayout(activity, BoxLayout.Y_AXIS));
        activity.setOpaque(false);
        JScrollPane activityScroll = new JScrollPane(activity);
        activityScroll.setBorder(null);
        activityScroll.getViewport().setOpaque(false);
        activityScroll.setOpaque(false);
        timeline.add(activityScroll, BorderLayout.CENTER);
        render();
    }

    Card calendar() { return calendar; }
    Card timeline() { return timeline; }

    void refresh() {
        if (loading) return;
        loading = true;
        state.setForeground(UniMatrixTheme.TEXT_MUTED);
        state.setText("Loading saved examinations...");
        new SwingWorker<List<ExamSchedule>, Void>() {
            protected List<ExamSchedule> doInBackground() throws Exception {
                return new ExamScheduleDAO().getAllSchedules();
            }
            protected void done() {
                loading = false;
                try { display(get()); }
                catch (Exception ex) {
                    exams = List.of(); render();
                    state.setText("Could not load examinations. Check the database connection.");
                    state.setForeground(UniMatrixTheme.DANGER);
                }
            }
        }.execute();
    }

    void display(List<ExamSchedule> schedules) {
        exams = schedules.stream().filter(e -> e.getExamDate() != null)
            .sorted(Comparator.comparing(ExamSchedule::getExamDate)
                .thenComparing(e -> Objects.toString(e.getTimeSlot(), ""))).toList();
        if (!initialized && !exams.isEmpty()) {
            LocalDate today = LocalDate.now();
            week = monday(exams.stream().map(ExamSchedule::getExamDate)
                .filter(d -> !d.isBefore(today)).findFirst().orElse(exams.get(0).getExamDate()));
            initialized = true;
        }
        state.setForeground(UniMatrixTheme.TEXT_MUTED);
        render();
    }

    private void render() {
        month.setText(week.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
        grid.removeAll();
        // The last column also accommodates Sunday examinations.
        for (int column = 0; column < 8; column++) {
            JLabel label = new JLabel(column == 0 ? "Time" :
                week.plusDays(column - 1).format(DateTimeFormatter.ofPattern("EEE, d")), SwingConstants.CENTER);
            label.setFont(UniMatrixTheme.font(Font.BOLD, 11f));
            label.setOpaque(true); label.setBackground(new Color(0xF8FAFC));
            addCell(label, column, 0, 28);
        }
        for (int hour = 7; hour < 21; hour++) {
            JLabel time = new JLabel(LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h a")), SwingConstants.CENTER);
            time.setFont(UniMatrixTheme.font(Font.PLAIN, 10f));
            time.setForeground(UniMatrixTheme.TEXT_MUTED);
            addCell(time, 0, hour - 6, 54);
            for (int day = 0; day < 7; day++) {
                LocalDate date = week.plusDays(day);
                JPanel cell = new JPanel();
                cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
                cell.setBackground(Color.WHITE);
                for (ExamSchedule exam : exams) {
                    if (date.equals(exam.getExamDate()) && startHour(exam) == hour) cell.add(event(exam));
                }
                addCell(cell, day + 1, hour - 6, 54);
            }
        }
        activity.removeAll();
        List<ExamSchedule> visible = exams.stream().filter(e -> !e.getExamDate().isBefore(week)
            && e.getExamDate().isBefore(week.plusWeeks(1))).toList();
        for (ExamSchedule exam : visible) {
            JPanel row = new JPanel(new BorderLayout(0, 5));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)), new EmptyBorder(8, 12, 8, 12)));
            JLabel date = UniMatrixTheme.muted(exam.getExamDate() + " · " + Objects.toString(exam.getTimeSlot(), ""));
            date.setFont(UniMatrixTheme.font(Font.PLAIN, 11f));
            row.add(date, BorderLayout.NORTH);
            row.add(new JLabel(Objects.toString(exam.getCourseCode(), "Exam") + " · " + Objects.toString(exam.getRoomName(), "")), BorderLayout.CENTER);
            JLabel faculty = UniMatrixTheme.muted(Objects.toString(exam.getFacultyName(), ""));
            row.add(faculty, BorderLayout.SOUTH);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            activity.add(row); activity.add(Box.createVerticalStrut(8));
        }
        if (visible.isEmpty()) activity.add(UniMatrixTheme.muted("No examinations in this week."));
        state.setText(visible.size() + " saved examinations this week.");
        grid.revalidate(); grid.repaint(); activity.revalidate(); activity.repaint();
    }

    private void addCell(JComponent cell, int column, int row, int height) {
        cell.setBorder(BorderFactory.createLineBorder(new Color(0xF1F5F9)));
        cell.setPreferredSize(new Dimension(column == 0 ? 42 : 76, height));
        cell.setMinimumSize(new Dimension(0, height));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column; constraints.gridy = row;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = column == 0 ? .4 : 1;
        grid.add(cell, constraints);
    }

    private JPanel event(ExamSchedule exam) {
        JPanel event = new JPanel(new BorderLayout(0, 2));
        event.setBackground(COLORS[Math.floorMod(exam.getCourseId(), COLORS.length)]);
        event.setBorder(new EmptyBorder(4, 6, 4, 6));
        JLabel title = new JLabel(Objects.toString(exam.getCourseCode(), "Exam"));
        title.setForeground(Color.WHITE); title.setFont(UniMatrixTheme.font(Font.BOLD, 10f));
        JLabel time = new JLabel(Objects.toString(exam.getTimeSlot(), ""));
        time.setForeground(Color.WHITE); time.setFont(UniMatrixTheme.font(Font.PLAIN, 9f));
        event.add(title, BorderLayout.NORTH); event.add(time, BorderLayout.SOUTH);
        event.setToolTipText(exam.getCourseCode() + " · " + exam.getTimeSlot() + " · " + exam.getRoomName() + " · " + exam.getFacultyName());
        return event;
    }

    private static int startHour(ExamSchedule exam) {
        try { return ExamTimes.interval(exam.getTimeSlot())[0].getHour(); }
        catch (RuntimeException ex) { return -1; }
    }

    private static LocalDate monday(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static JButton navButton(String label, String tooltip) {
        JButton button = new JButton(label);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createLineBorder(UniMatrixTheme.BORDER));
        button.setBackground(UniMatrixTheme.SURFACE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(28, 28));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName(tooltip);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
