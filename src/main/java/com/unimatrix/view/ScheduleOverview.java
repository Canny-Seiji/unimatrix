package com.unimatrix.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import com.unimatrix.dao.ExamScheduleDAO;
import com.unimatrix.model.ExamSchedule;
import com.unimatrix.service.ExamTimes;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

/** Read-only calendar with blocks positioned by their actual start/end times. */
final class ScheduleOverview {
    private static final int START_MINUTE = 7 * 60;
    private static final int END_MINUTE = 21 * 60;
    private static final int HOUR_HEIGHT = 60;
    private static final int TIME_WIDTH = 52;
    private static final int DAY_WIDTH = 86;
    private static final int TOP = 8;
    private static final Color[] COLORS = {
        new Color(0x2563EB), new Color(0x047857),
        new Color(0x7C3AED), new Color(0x0F766E)
    };

    private final Card calendar = new Card(new BorderLayout(0, 12));
    private final Card timeline = new Card(new BorderLayout(0, 12));
    private final CalendarGrid grid = new CalendarGrid();
    private final JPanel dayHeader = new JPanel(null) {
        @Override public void doLayout() {
            if (getComponentCount() != 8) return;
            getComponent(0).setBounds(0, 0, TIME_WIDTH, 28);
            for (int day = 0; day < 7; day++) {
                int x = dayX(day, getWidth());
                getComponent(day + 1).setBounds(
                    x, 0, dayX(day + 1, getWidth()) - x, 28);
            }
        }
    };
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
            BorderFactory.createLineBorder(UniMatrixTheme.BORDER),
            new EmptyBorder(4, 10, 4, 10)));
        header.add(navigation, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);
        calendar.add(header, BorderLayout.NORTH);

        dayHeader.setPreferredSize(new Dimension(TIME_WIDTH + 7 * DAY_WIDTH, 28));
        JScrollPane calendarScroll = new JScrollPane(grid);
        calendarScroll.setColumnHeaderView(dayHeader);
        calendarScroll.setBorder(BorderFactory.createLineBorder(UniMatrixTheme.BORDER));
        calendarScroll.getVerticalScrollBar().setUnitIncrement(HOUR_HEIGHT / 2);
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
        DateTimeFormatter weekFormat = DateTimeFormatter.ofPattern(
            "MMM d, yyyy", java.util.Locale.ENGLISH);
        month.setText(week.format(weekFormat) + " – "
            + week.plusDays(6).format(weekFormat));
        dayHeader.removeAll();
        for (int column = 0; column < 8; column++) {
            JLabel label = new JLabel(column == 0 ? "Time" :
                week.plusDays(column - 1).format(DateTimeFormatter.ofPattern("EEE, d")),
                SwingConstants.CENTER);
            label.setFont(UniMatrixTheme.font(Font.BOLD, 11f));
            label.setForeground(UniMatrixTheme.TEXT);
            label.setOpaque(true);
            label.setBackground(new Color(0xF8FAFC));
            label.setBorder(BorderFactory.createLineBorder(new Color(0xF1F5F9)));
            dayHeader.add(label);
        }
        List<ExamSchedule> visible = exams.stream().filter(e -> !e.getExamDate().isBefore(week)
            && e.getExamDate().isBefore(week.plusWeeks(1))).toList();
        grid.showExams(visible);

        activity.removeAll();
        for (ExamSchedule exam : visible) {
            JPanel row = new JPanel(new BorderLayout(0, 5));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                new EmptyBorder(8, 12, 8, 12)));
            JLabel date = UniMatrixTheme.muted(exam.getExamDate() + " · "
                + Objects.toString(exam.getTimeSlot(), ""));
            date.setFont(UniMatrixTheme.font(Font.PLAIN, 11f));
            row.add(date, BorderLayout.NORTH);
            row.add(new JLabel(Objects.toString(exam.getCourseCode(), "Exam") + " · "
                + Objects.toString(exam.getRoomName(), "")), BorderLayout.CENTER);
            row.add(UniMatrixTheme.muted(Objects.toString(exam.getFacultyName(), "")),
                BorderLayout.SOUTH);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            activity.add(row); activity.add(Box.createVerticalStrut(8));
        }
        if (visible.isEmpty()) activity.add(UniMatrixTheme.muted("No examinations in this week."));
        state.setText(visible.size() + " saved examinations this week.");
        dayHeader.revalidate(); dayHeader.repaint();
        activity.revalidate(); activity.repaint();
    }

    private JPanel event(ExamSchedule exam) {
        JLabel title = new JLabel(
            Objects.toString(exam.getCourseCode(), "Exam"), SwingConstants.CENTER);
        JLabel time = new JLabel(timeRange(exam.getTimeSlot()), SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        time.setForeground(Color.WHITE);
        title.setFont(UniMatrixTheme.font(Font.BOLD, 13f));
        time.setFont(UniMatrixTheme.font(Font.BOLD, 11f));

        JPanel event = new JPanel(null) {
            @Override public void doLayout() {
                int width = Math.max(1, getWidth() - 12);
                boolean compact = getHeight() < 44;
                title.setFont(fittedFont(title, compact ? 10f : 13f, width)
                    .deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON)));
                time.setFont(fittedFont(time, compact ? 9f : 11f, width));
                int titleHeight = title.getFontMetrics(title.getFont()).getHeight();
                int timeHeight = time.getFontMetrics(time.getFont()).getHeight();
                int gap = compact ? 0 : 4;
                int top = Math.max(0, (getHeight() - titleHeight - timeHeight - gap) / 2);
                title.setBounds(6, top, width, titleHeight);
                time.setBounds(6, top + titleHeight + gap, width, timeHeight);
            }
        };
        event.setBackground(COLORS[Math.floorMod(exam.getCourseId(), COLORS.length)]);
        event.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 55)));
        event.add(title); event.add(time);
        String tooltip = exam.getCourseCode() + " · " + exam.getTimeSlot() + " · "
            + exam.getRoomName() + " · " + exam.getFacultyName();
        event.setToolTipText(tooltip);
        title.setToolTipText(tooltip); time.setToolTipText(tooltip);
        event.getAccessibleContext().setAccessibleName(tooltip);
        return event;
    }

    private static Font fittedFont(JLabel label, float preferredSize, int width) {
        Font font = UniMatrixTheme.font(Font.BOLD, preferredSize);
        while (preferredSize > 9f
                && label.getFontMetrics(font).stringWidth(label.getText()) > width) {
            preferredSize -= 0.5f;
            font = UniMatrixTheme.font(Font.BOLD, preferredSize);
        }
        return font;
    }

    private static String timeRange(String value) {
        try {
            LocalTime[] interval = ExamTimes.interval(value);
            DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm");
            return interval[0].format(format) + "–" + interval[1].format(format);
        } catch (RuntimeException ex) {
            return Objects.toString(value, "");
        }
    }

    private final class CalendarGrid extends JPanel implements Scrollable {
        private final List<Block> blocks = new ArrayList<>();

        CalendarGrid() { setLayout(null); setBackground(Color.WHITE); }

        void showExams(List<ExamSchedule> visible) {
            removeAll(); blocks.clear();
            for (int day = 0; day < 7; day++) {
                List<Block> daily = new ArrayList<>();
                for (ExamSchedule exam : visible) {
                    if (!week.plusDays(day).equals(exam.getExamDate())) continue;
                    try {
                        LocalTime[] interval = ExamTimes.interval(exam.getTimeSlot());
                        int start = Math.max(START_MINUTE, interval[0].toSecondOfDay() / 60);
                        int end = Math.min(END_MINUTE, interval[1].toSecondOfDay() / 60);
                        if (end > start) daily.add(new Block(day, start, end, event(exam)));
                    } catch (RuntimeException ignored) { }
                }
                daily.sort(Comparator.comparingInt((Block b) -> b.start).thenComparingInt(b -> b.end));
                List<Block> group = new ArrayList<>();
                int groupEnd = -1;
                for (Block block : daily) {
                    if (!group.isEmpty() && block.start >= groupEnd) {
                        assignLanes(group); group.clear(); groupEnd = -1;
                    }
                    group.add(block); groupEnd = Math.max(groupEnd, block.end);
                }
                assignLanes(group);
                for (Block block : daily) { blocks.add(block); add(block.view); }
            }
            revalidate(); repaint();
        }

        private void assignLanes(List<Block> group) {
            List<Integer> ends = new ArrayList<>();
            for (Block block : group) {
                int lane = 0;
                while (lane < ends.size() && ends.get(lane) > block.start) lane++;
                if (lane == ends.size()) ends.add(block.end);
                else ends.set(lane, block.end);
                block.lane = lane;
            }
            for (Block block : group) block.lanes = ends.size();
        }

        @Override public void doLayout() {
            for (Block block : blocks) {
                int left = dayX(block.day, getWidth());
                int width = dayX(block.day + 1, getWidth()) - left;
                int x1 = left + width * block.lane / block.lanes;
                int x2 = left + width * (block.lane + 1) / block.lanes;
                int y1 = minuteY(block.start);
                int y2 = minuteY(block.end);
                block.view.setBounds(x1 + 2, y1 + 1,
                    Math.max(1, x2 - x1 - 4), Math.max(1, y2 - y1 - 2));
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0xF8FAFC));
            g.fillRect(0, 0, TIME_WIDTH, getHeight());
            g.setFont(UniMatrixTheme.font(Font.BOLD, 11f));
            FontMetrics fm = g.getFontMetrics();
            for (int minute = START_MINUTE; minute <= END_MINUTE; minute += 30) {
                int y = minuteY(minute);
                boolean hour = minute % 60 == 0;
                g.setColor(new Color(hour ? 0xE3E8EF : 0xF1F5F9));
                g.drawLine(TIME_WIDTH, y, getWidth(), y);
                if (hour) {
                    String text = LocalTime.of(minute / 60, 0)
                        .format(DateTimeFormatter.ofPattern("h a"));
                    g.setColor(UniMatrixTheme.TEXT);
                    g.drawString(text, (TIME_WIDTH - fm.stringWidth(text)) / 2,
                        y + fm.getAscent() / 2);
                }
            }
            g.setColor(new Color(0xE3E8EF));
            for (int day = 0; day <= 7; day++) {
                int x = dayX(day, getWidth());
                g.drawLine(x, 0, x, getHeight());
            }
            g.dispose();
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(TIME_WIDTH + DAY_WIDTH * 7, minuteY(END_MINUTE) + 14);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport v && v.getWidth() >= getPreferredSize().width;
        }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
        @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? HOUR_HEIGHT / 2 : DAY_WIDTH;
        }
        @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL
                ? Math.max(HOUR_HEIGHT, r.height - HOUR_HEIGHT)
                : Math.max(DAY_WIDTH, r.width - DAY_WIDTH);
        }
    }

    private static final class Block {
        final int day, start, end;
        final JPanel view;
        int lane, lanes = 1;
        Block(int day, int start, int end, JPanel view) {
            this.day = day; this.start = start; this.end = end; this.view = view;
        }
    }

    private static int dayX(int day, int width) {
        return TIME_WIDTH + (width - TIME_WIDTH) * day / 7;
    }
    private static int minuteY(int minute) {
        return TOP + (int) Math.round((minute - START_MINUTE) * HOUR_HEIGHT / 60.0);
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
