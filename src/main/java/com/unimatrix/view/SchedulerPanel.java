package com.unimatrix.view;

import com.unimatrix.service.SchedulingEngine;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class SchedulerPanel extends JPanel {
    private final SchedulingEngine engine = new SchedulingEngine();
    private final JLabel statusLabel = UniMatrixTheme.muted("Status: Ready to generate master timetable.");
    private final JButton generateBtn = UniMatrixTheme.primaryButton("Run Scheduling Engine");

    public SchedulerPanel() {
        super(new GridBagLayout());
        setOpaque(false);

        Card card = new Card(new BorderLayout(0, 20));
        card.setPreferredSize(new Dimension(500, 250));

        JLabel title = new JLabel("Automated Exam Timetable Generator");
        title.setFont(UniMatrixTheme.font(Font.BOLD, 16f));

        JTextArea desc = new JTextArea("Clicking generate will process all courses, rooms, block sections, "
                + "and faculty while validating all hard constraints (capacity, double-booking prevention).");
        desc.setWrapStyleWord(true);
        desc.setLineWrap(true);
        desc.setOpaque(false);
        desc.setEditable(false);
        desc.setFont(UniMatrixTheme.font(Font.PLAIN, 13f));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.add(desc, BorderLayout.NORTH);
        centerPanel.add(statusLabel, BorderLayout.CENTER);

        generateBtn.addActionListener(e -> runEngine());

        card.add(title, BorderLayout.NORTH);
        card.add(centerPanel, BorderLayout.CENTER);
        card.add(generateBtn, BorderLayout.SOUTH);

        add(card);
    }

    private void runEngine() {
        generateBtn.setEnabled(false);
        statusLabel.setText("Status: Running heuristic engine... please wait.");
        statusLabel.setForeground(UniMatrixTheme.TEXT);

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                LocalDate startDate = LocalDate.now().plusDays(7);
                return engine.generateMasterSchedule(startDate);
            }
            @Override protected void done() {
                generateBtn.setEnabled(true);
                try {
                    if (get()) {
                        statusLabel.setText("Status: Schedule generated successfully!");
                        statusLabel.setForeground(UniMatrixTheme.SUCCESS);
                        JOptionPane.showMessageDialog(SchedulerPanel.this, "Master schedule generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        statusLabel.setText("Status: Generation failed. Verify base data records.");
                        statusLabel.setForeground(UniMatrixTheme.DANGER);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Status: Error during execution.");
                    statusLabel.setForeground(UniMatrixTheme.DANGER);
                }
            }
        }.execute();
    }
}
