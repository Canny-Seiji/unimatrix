package com.unimatrix.view;

import com.unimatrix.view.components.Card;
import com.unimatrix.view.components.StatCard;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Landing page: metric cards, quick actions, last schedule status. */
public class DashboardHomePanel extends JPanel {

    private final DashboardDataProvider provider;
    private final StatCard rooms = new StatCard("Rooms");
    private final StatCard blocks = new StatCard("Block sections");
    private final StatCard faculty = new StatCard("Faculty");
    private final StatCard courses = new StatCard("Courses");
    private final JLabel scheduleStatus = new JLabel();

    public DashboardHomePanel(DashboardDataProvider provider, Consumer<Page> navigator) {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        this.provider = provider;
        setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 4, UniMatrixTheme.GAP, 0));
        stats.setOpaque(false);
        stats.add(rooms);
        stats.add(blocks);
        stats.add(faculty);
        stats.add(courses);

        JPanel bottom = new JPanel(new GridLayout(1, 2, UniMatrixTheme.GAP, 0));
        bottom.setOpaque(false);
        bottom.add(buildQuickActions(navigator));
        bottom.add(buildScheduleCard());

        add(stats, BorderLayout.NORTH);
        add(bottom, BorderLayout.CENTER);
    }

    private Card buildQuickActions(Consumer<Page> navigator) {
        Card card = new Card(new BorderLayout(0, 14));
        JLabel heading = new JLabel("Quick actions");
        heading.setFont(UniMatrixTheme.font(Font.BOLD, 15f));

        JButton generate = UniMatrixTheme.primaryButton("Generate schedule");
        generate.addActionListener(e -> navigator.accept(Page.SCHEDULER));
        JButton manage = UniMatrixTheme.secondaryButton("Manage data");
        manage.addActionListener(e -> navigator.accept(Page.MANAGE_DATA));
        JButton export = UniMatrixTheme.secondaryButton("Export timetable");
        export.addActionListener(e -> navigator.accept(Page.EXPORT));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.add(generate);
        row.add(manage);
        row.add(export);

        card.add(heading, BorderLayout.NORTH);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private Card buildScheduleCard() {
        Card card = new Card(new BorderLayout(0, 14));
        JLabel heading = new JLabel("Last schedule");
        heading.setFont(UniMatrixTheme.font(Font.BOLD, 15f));
        scheduleStatus.setVerticalAlignment(SwingConstants.TOP);
        scheduleStatus.setForeground(UniMatrixTheme.TEXT_MUTED);
        card.add(heading, BorderLayout.NORTH);
        card.add(scheduleStatus, BorderLayout.CENTER);
        return card;
    }

    /** Reloads metrics in the background; safe to call from the EDT. */
    public void refresh() {
        rooms.setLoading(); blocks.setLoading(); faculty.setLoading(); courses.setLoading();
        setScheduleText("Loading…", UniMatrixTheme.TEXT_MUTED);

        new SwingWorker<DashboardStats, Void>() {
            @Override protected DashboardStats doInBackground() throws Exception {
                return provider.load();
            }
            @Override protected void done() {
                try {
                    DashboardStats s = get();
                    rooms.setValue(s.rooms());
                    blocks.setValue(s.blocks());
                    faculty.setValue(s.faculty());
                    courses.setValue(s.courses());
                    if (s.lastScheduleSummary() == null || s.lastScheduleSummary().isBlank()) {
                        setScheduleText("No schedule generated yet.", UniMatrixTheme.TEXT_MUTED);
                    } else {
                        setScheduleText(s.lastScheduleSummary(), UniMatrixTheme.TEXT);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    rooms.setError(); blocks.setError(); faculty.setError(); courses.setError();
                    setScheduleText("Could not load data. Check the database connection.", UniMatrixTheme.DANGER);
                }
            }
        }.execute();
    }

    private void setScheduleText(String text, Color color) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        scheduleStatus.setText("<html><div style='width:260px'>" + safe + "</div></html>");
        scheduleStatus.setForeground(color);
    }
}