package com.unimatrix.view;

import com.unimatrix.dao.ExamScheduleDAO;
import com.unimatrix.model.ExamSchedule;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;

public class ExportPanel extends JPanel {
    private final ExamScheduleDAO scheduleDAO = new ExamScheduleDAO();

    public ExportPanel() {
        super(new GridBagLayout());
        setOpaque(false);

        Card card = new Card(new BorderLayout(0, 20));
        card.setPreferredSize(new Dimension(450, 220));

        JLabel title = new JLabel("Export Timetable Reports");
        title.setFont(UniMatrixTheme.font(Font.BOLD, 16f));

        JButton exportCsvBtn = UniMatrixTheme.primaryButton("Export to CSV File");
        exportCsvBtn.addActionListener(e -> exportToCsv());

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 10));
        content.setOpaque(false);
        content.add(UniMatrixTheme.muted("Save the finalized exam schedule sheet to your local drive."));
        content.add(exportCsvBtn);

        card.add(title, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        add(card);
    }

    private void exportToCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("unimatrix_schedule.csv"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("ID,CourseID,RoomID,BlockID,FacultyID,ExamDate,TimeSlot");
                List<ExamSchedule> schedules = scheduleDAO.getAllSchedules();
                for (ExamSchedule s : schedules) {
                    writer.printf("%d,%d,%d,%d,%d,%s,%s\n",
                            s.getId(), s.getCourseId(), s.getRoomId(),
                            s.getBlockSectionId(), s.getFacultyId(),
                            s.getExamDate(), s.getTimeSlot());
                }
                JOptionPane.showMessageDialog(this, "Exported successfully to " + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error exporting file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
