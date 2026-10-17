package com.unimatrix.view;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

public class MainDashboardView extends JFrame {

    private JPanel contentPanel;
    private CardLayout cardLayout;

    public MainDashboardView() {
        setTitle("UniMatrix - Automated Exam Scheduling System");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center on screen

        initUI();
    }

    private void initUI() {
        // Main layout using BorderLayout
        setLayout(new BorderLayout());

        // --- 1. Sidebar Navigation Panel ---
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new GridLayout(6, 1, 5, 5));
        sidebarPanel.setPreferredSize(new Dimension(220, getHeight()));
        sidebarPanel.setBackground(new Color(33, 37, 41)); // Dark modern sidebar
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        // Brand / Title inside Sidebar
        JLabel brandLabel = new JLabel("UniMatrix Admin", JLabel.CENTER);
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sidebarPanel.add(brandLabel);

        // Navigation Buttons
        JButton btnHome = createSidebarButton("Dashboard Home");
        JButton btnManageData = createSidebarButton("Manage Data (CRUD)");
        JButton btnScheduler = createSidebarButton("Scheduling Engine");
        JButton btnViewer = createSidebarButton("View Timetable");
        JButton btnExport = createSidebarButton("Export Reports");
        JButton btnLogout = createSidebarButton("Logout");

        sidebarPanel.add(btnHome);
        sidebarPanel.add(btnManageData);
        sidebarPanel.add(btnScheduler);
        sidebarPanel.add(btnViewer);
        sidebarPanel.add(btnExport);
        sidebarPanel.add(btnLogout);

        add(sidebarPanel, BorderLayout.WEST);

        // --- 2. Dynamic Content Area (CardLayout) ---
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Add views to cards
        contentPanel.add(createHomePanel(), "HOME");
        contentPanel.add(createPlaceholderPanel("Manage Rooms, Block Sections, Courses & Faculty (CRUD Phase)"), "MANAGE");
        contentPanel.add(createPlaceholderPanel("Scheduling Engine Control Panel (Trigger & Monitor Algorithm)"), "SCHEDULER");
        contentPanel.add(createPlaceholderPanel("Schedule Viewer JTable Grid & Filters"), "VIEWER");
        contentPanel.add(createPlaceholderPanel("Export Timetable to PDF or CSV Options"), "EXPORT");

        add(contentPanel, BorderLayout.CENTER);

        // --- 3. Button Action Listeners ---
        btnHome.addActionListener(e -> cardLayout.show(contentPanel, "HOME"));
        btnManageData.addActionListener(e -> cardLayout.show(contentPanel, "MANAGE"));
        btnScheduler.addActionListener(e -> cardLayout.show(contentPanel, "SCHEDULER"));
        btnViewer.addActionListener(e -> cardLayout.show(contentPanel, "VIEWER"));
        btnExport.addActionListener(e -> cardLayout.show(contentPanel, "EXPORT"));
        
        btnLogout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginView().setVisible(true); // Return to login
            }
        });
    }

    // Helper method to style sidebar buttons consistently
    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(52, 58, 64));
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        return button;
    }

    // Home / Dashboard Landing Panel
    JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 247, 250));
        
        JLabel welcomeLabel = new JLabel("Welcome to UniMatrix Scheduling Dashboard", JLabel.CENTER);
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        welcomeLabel.setForeground(new Color(33, 37, 41));

        JLabel subLabel = new JLabel("Select an option from the sidebar to manage data, run algorithms, or view reports.", JLabel.CENTER);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subLabel.setForeground(new Color(108, 117, 125));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 10, 0);
        panel.add(welcomeLabel, gbc);

        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(subLabel, gbc);

        return panel;
    }

    // Reusable placeholder panel for modules yet to be built
    private JPanel createPlaceholderPanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 247, 250));
        JLabel label = new JLabel(message, JLabel.CENTER);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(new Color(73, 80, 87));
        panel.add(label);
        return panel;
    }

    // Optional main method to test this view independently
    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            new MainDashboardView().setVisible(true);
        });
    }
}