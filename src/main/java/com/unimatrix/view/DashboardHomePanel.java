package com.unimatrix.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.unimatrix.view.components.Card;
import com.unimatrix.view.components.StatCard;
import com.unimatrix.view.theme.UniMatrixTheme;

/** Dashboard with a full-height calendar and a timeline/actions sidebar. */
public class DashboardHomePanel extends JPanel {

    private final DashboardDataProvider provider;

    private final StatCard rooms = new StatCard("Rooms");
    private final StatCard blocks = new StatCard("Block sections");
    private final StatCard faculty = new StatCard("Faculty");
    private final StatCard courses = new StatCard("Courses");

    private final ScheduleOverview overview = new ScheduleOverview();

    public DashboardHomePanel(
            DashboardDataProvider provider,
            Consumer<Page> navigator) {

        super(new BorderLayout(0, UniMatrixTheme.GAP));
        this.provider = provider;
        setOpaque(false);

        // Top statistics.
        JPanel stats = new JPanel(
            new GridLayout(1, 4, UniMatrixTheme.GAP, 0)
        );
        stats.setOpaque(false);

        stats.add(rooms);
        stats.add(blocks);
        stats.add(faculty);
        stats.add(courses);

        // Right column: timeline above Quick actions.
        JPanel right = new JPanel(
            new BorderLayout(0, UniMatrixTheme.GAP)
        );
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(300, 0));

        right.add(overview.timeline(), BorderLayout.CENTER);
        right.add(
            buildQuickActions(navigator),
            BorderLayout.SOUTH
        );

        // Calendar fills the remaining area on the left.
        JPanel content = new JPanel(
            new BorderLayout(UniMatrixTheme.GAP, 0)
        );
        content.setOpaque(false);

        content.add(buildCalendarCard(), BorderLayout.CENTER);
        content.add(right, BorderLayout.EAST);

        add(stats, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    private Card buildCalendarCard() {
        Card card = overview.calendar();

        // Preserve the existing week navigation controls.
        BorderLayout layout = (BorderLayout) card.getLayout();
        Component controls =
            layout.getLayoutComponent(BorderLayout.NORTH);

        if (controls != null) {
            card.remove(controls);
        }

        JLabel heading = new JLabel("Examination Calendar");
        heading.setFont(
            UniMatrixTheme.font(Font.BOLD, 15f)
        );
        heading.setForeground(UniMatrixTheme.TEXT);

        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);

        header.add(heading, BorderLayout.NORTH);

        if (controls != null) {
            header.add(controls, BorderLayout.CENTER);
        }

        card.add(header, BorderLayout.NORTH);

        return card;
    }

    private Card buildQuickActions(Consumer<Page> navigator) {
        Card card = new Card(new BorderLayout(0, 14));

        JLabel heading = new JLabel("Quick actions");
        heading.setFont(
            UniMatrixTheme.font(Font.BOLD, 15f)
        );
        heading.setForeground(UniMatrixTheme.TEXT);

        JButton generate =
            UniMatrixTheme.primaryButton("Generate schedule");

        generate.addActionListener(
            e -> navigator.accept(Page.MANAGE_SCHEDULES)
        );

        JButton manage =
            UniMatrixTheme.secondaryButton("Manage data");

        manage.addActionListener(
            e -> navigator.accept(Page.MANAGE_DATA)
        );

        JButton export =
            UniMatrixTheme.secondaryButton("Export timetable");

        export.addActionListener(
            e -> navigator.accept(Page.MANAGE_SCHEDULES)
        );

        JPanel buttons = new JPanel(
            new GridLayout(0, 1, 0, 6)
        );
        buttons.setOpaque(false);

        buttons.add(generate);
        buttons.add(manage);
        buttons.add(export);

        card.add(heading, BorderLayout.NORTH);
        card.add(buttons, BorderLayout.CENTER);

        return card;
    }

    /** Reloads the calendar and metrics in the background. */
    public void refresh() {
        if (provider instanceof DatabaseDashboardProvider) {
            overview.refresh();
        }

        rooms.setLoading();
        blocks.setLoading();
        faculty.setLoading();
        courses.setLoading();

        new SwingWorker<DashboardStats, Void>() {

            @Override
            protected DashboardStats doInBackground()
                    throws Exception {
                return provider.load();
            }

            @Override
            protected void done() {
                try {
                    DashboardStats stats = get();

                    rooms.setValue(stats.rooms());
                    blocks.setValue(stats.blocks());
                    faculty.setValue(stats.faculty());
                    courses.setValue(stats.courses());

                } catch (InterruptedException | ExecutionException ex) {
                    rooms.setError();
                    blocks.setError();
                    faculty.setError();
                    courses.setError();
                }
            }
        }.execute();
    }
}