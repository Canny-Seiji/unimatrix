package com.unimatrix.view;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.Border;
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

        // --- 1. Sidebar Container Setup ---
        JPanel sidebarContainer = new JPanel(new BorderLayout());
        sidebarContainer.setPreferredSize(new Dimension(200, getHeight()));
        sidebarContainer.setBackground(Color.WHITE); 

        // FIX: Changed side padding from 15 to 0 so buttons can touch the edges!
        // Parameters: createEmptyBorder(top, left, bottom, right)
        Border padding = BorderFactory.createEmptyBorder(20, 0, 20, 0); 
        Border rightLine = BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY); 
        sidebarContainer.setBorder(BorderFactory.createCompoundBorder(rightLine, padding));

        // --- 2. Sidebar Navigation Panel ---
        JPanel sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new GridLayout(0, 1, 0, 0)); 
        sidebarPanel.setOpaque(false); 

        // Brand / Title inside Sidebar
        // FIX: Because we removed the panel margins, add a small left-margin directly 
        // to the label border so the "UniMatrix" text doesn't look glued to the left screen edge.
        JLabel brandLabel = new JLabel("UniMatrix", JLabel.LEFT);
        brandLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 0)); // Spacing just for the text title
        brandLabel.setForeground(new Color(33, 37, 41)); 
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sidebarPanel.add(brandLabel);

        // Navigation Buttons
        JButton btnHome = createSidebarButton("Dashboard Home");
        JButton btnManageData = createSidebarButton("Manage Data (CRUD)");
        JButton btnScheduler = createSidebarButton("Scheduling Engine");
        JButton btnViewer = createSidebarButton("View Timetable");
        JButton btnExport = createSidebarButton("Export Reports");
        JButton btnLogout = createSidebarButton("Logout");

        // Add buttons to the layout panel
        sidebarPanel.add(btnHome);
        sidebarPanel.add(btnManageData);
        sidebarPanel.add(btnScheduler);
        sidebarPanel.add(btnViewer);
        sidebarPanel.add(btnExport);
        sidebarPanel.add(btnLogout);

        // Anchor to the top and load into frame
        sidebarContainer.add(sidebarPanel, BorderLayout.NORTH);
        add(sidebarContainer, BorderLayout.WEST);

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
            }
        });
    }

    // Helper method to style sidebar buttons consistently
    private JButton createSidebarButton(String text) {
        JButton button = new JButton(text);
        
        // 1. Text & Alignment Styling
        button.setForeground(Color.BLACK); // Dark color for text legibility on light background
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        
        // 2. Button Color (Resting State Background matches the main background)
        button.setBackground(Color.WHITE); 
        
        // 3. Button Roundness (Arc Radius)
        button.putClientProperty("JButton.arc", 40); 
        
        // 4. FIX: Button Borderline Color Configuration
        // Do NOT use button.setBorder(). Let FlatLaf handle the border lines and pass inner margins like this instead:
        button.putClientProperty("JButton.margin", new Insets(10, 15, 10, 15)); // Handles padding internally
        button.putClientProperty("JButton.squareSize", false);
        button.putClientProperty("JButton.borderColor", Color.BLACK); // Clean, solid black edge lines will now show up!
        
        // 5. Interaction / Hover Colors
        button.putClientProperty("JButton.hoverBackground", Color.WHITE); 
        button.putClientProperty("JButton.hoverBorderColor", Color.GRAY);  
        button.putClientProperty("JButton.focusedBackground", new Color(245, 247, 250));
        
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

    public static void main(String[] args) {
        // 1. Setup the Look and Feel FIRST
        FlatLightLaf.setup();
        
        // 2. ONLY THEN initialize your GUI views
        SwingUtilities.invokeLater(() -> {
            new MainDashboardView().setVisible(true);
        });
    }
}
