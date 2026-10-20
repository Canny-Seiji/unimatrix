package com.unimatrix.view;

import com.unimatrix.dao.RoomDAO;
import com.unimatrix.model.Room;
import com.unimatrix.view.components.Card;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ManageDataPanel extends JPanel {
    private final RoomDAO roomDAO = new RoomDAO();
    private DefaultTableModel roomTableModel;

    public ManageDataPanel() {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        setOpaque(false);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UniMatrixTheme.font(Font.BOLD, 13f));
        tabbedPane.addTab("Rooms", createRoomTab());
        tabbedPane.addTab("Block Sections", createPlaceholderTab("Block Sections Management coming soon..."));
        tabbedPane.addTab("Courses", createPlaceholderTab("Courses Management coming soon..."));
        tabbedPane.addTab("Faculty", createPlaceholderTab("Faculty Management coming soon..."));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createRoomTab() {
        JPanel panel = new JPanel(new BorderLayout(0, UniMatrixTheme.GAP));
        panel.setOpaque(false);

        String[] columns = {"ID", "Room Name", "Building", "Capacity"};
        roomTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(roomTableModel);
        table.setRowHeight(28);
        loadRooms();

        Card tableCard = new Card(new BorderLayout());
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        // Add form at bottom
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
                String name = nameField.getText().trim();
                String bldg = bldgField.getText().trim();
                int cap = Integer.parseInt(capField.getText().trim());
                if(roomDAO.addRoom(new Room(0, name, bldg, cap))) {
                    loadRooms();
                    nameField.setText(""); bldgField.setText(""); capField.setText("");
                    JOptionPane.showMessageDialog(this, "Room added successfully!");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input. Check capacity number.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        form.add(addBtn);

        panel.add(tableCard, BorderLayout.CENTER);
        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private void loadRooms() {
        roomTableModel.setRowCount(0);
        List<Room> rooms = roomDAO.getAllRooms();
        for (Room r : rooms) {
            roomTableModel.addRow(new Object[]{r.getId(), r.getRoomName(), r.getBuilding(), r.getCapacity()});
        }
    }

    private JPanel createPlaceholderTab(String text) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(UniMatrixTheme.muted(text));
        return p;
    }
}
