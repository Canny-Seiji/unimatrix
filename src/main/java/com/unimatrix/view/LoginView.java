package com.unimatrix.view;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.unimatrix.view.theme.UniMatrixTheme;

public class LoginView extends JFrame {

    private final JTextField userField = new JTextField();
    private final JPasswordField passField = new JPasswordField();

    public LoginView() {
        super("UniMatrix - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        // Root panel holding Left Hero Section (60%) and Right Form Section (40%)
        JPanel rootPanel = new JPanel(new GridBagLayout());
        rootPanel.setBackground(UniMatrixTheme.SURFACE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // --- LEFT HERO PANEL (~60% width) ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.58;
        rootPanel.add(buildHeroPanel(), gbc);

        // --- RIGHT FORM PANEL (~40% width) ---
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.42;
        rootPanel.add(buildFormPanel(), gbc);

        setContentPane(rootPanel);
    }

    private JPanel buildHeroPanel() {
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Deep primary blue background matching UniMatrix accent
                g2.setPaint(new Color(0x25, 0x63, 0xEB));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle vector geometric background curves
                g2.setColor(new Color(255, 255, 255, 18));
                g2.setStroke(new BasicStroke(1.5f));
                for (int i = 0; i < 5; i++) {
                    int offset = i * 42;
                    g2.drawRoundRect(90 + offset, -60 + offset, 400, 600, 90, 90);
                }

                g2.dispose();
            }
        };
        hero.setBorder(new EmptyBorder(44, 48, 44, 48));

        // Center vertical container for Star Emblem + Quotes
        JPanel centerStack = new JPanel();
        centerStack.setOpaque(false);
        centerStack.setLayout(new BoxLayout(centerStack, BoxLayout.Y_AXIS));

        // Vector Star Emblem Icon
        JLabel starLogo = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = 22, cy = 22, r = 16;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45);
                    int x2 = cx + (int) (r * Math.cos(angle));
                    int y2 = cy + (int) (r * Math.sin(angle));
                    g2.drawLine(cx, cy, x2, y2);
                }
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(44, 44);
            }
        };
        starLogo.setMinimumSize(new Dimension(44, 44));
        starLogo.setMaximumSize(new Dimension(44, 44));
        starLogo.setAlignmentX(LEFT_ALIGNMENT);

        // Headline
        JLabel heading = new JLabel("<html>Hello<br>UniMatrix!</html>");
        heading.setFont(UniMatrixTheme.font(Font.BOLD, 36f));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        // Slogan / Quote
        JLabel quote = new JLabel("<html><div style='width:300px'>Skip repetitive and manual conflict checks. Experience automated timetable optimization, smart faculty allocation, and instant matrix generation!</div></html>");
        quote.setFont(UniMatrixTheme.font(Font.PLAIN, 13f));
        quote.setForeground(new Color(0xE0, 0xE7, 0xFF));
        quote.setAlignmentX(LEFT_ALIGNMENT);
        quote.setPreferredSize(new Dimension(380, 75));
        quote.setMaximumSize(new Dimension(380, 100));

        centerStack.add(starLogo);
        centerStack.add(Box.createVerticalStrut(32));
        centerStack.add(heading);
        centerStack.add(Box.createVerticalStrut(18));
        centerStack.add(quote);

        // Footer copyright
        JLabel footer = new JLabel("© 2026 UniMatrix. All rights reserved.");
        footer.setFont(UniMatrixTheme.font(Font.PLAIN, 11f));
        footer.setForeground(new Color(255, 255, 255, 160));

        hero.add(centerStack, BorderLayout.CENTER);
        hero.add(footer, BorderLayout.SOUTH);

        return hero;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(36, 44, 36, 44));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Brand Name
        JLabel brand = new JLabel("UniMatrix");
        brand.setFont(UniMatrixTheme.font(Font.BOLD, 18f));
        brand.setForeground(UniMatrixTheme.TEXT);
        gbc.insets = new Insets(0, 0, 32, 0);
        form.add(brand, gbc);

        // Title
        gbc.gridy++;
        JLabel welcome = new JLabel("Welcome Back!");
        welcome.setFont(UniMatrixTheme.font(Font.BOLD, 22f));
        welcome.setForeground(UniMatrixTheme.TEXT);
        gbc.insets = new Insets(0, 0, 4, 0);
        form.add(welcome, gbc);

        // Subtitle
        gbc.gridy++;
        JLabel sub = UniMatrixTheme.muted("Enter credentials to access system");
        sub.setFont(UniMatrixTheme.font(Font.PLAIN, 12f));
        gbc.insets = new Insets(0, 0, 24, 0);
        form.add(sub, gbc);

        // Username Label
        gbc.gridy++;
        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(UniMatrixTheme.font(Font.BOLD, 12f));
        userLabel.setForeground(UniMatrixTheme.TEXT);
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(userLabel, gbc);

        // Username Input
        gbc.gridy++;
        userField.setPreferredSize(new Dimension(280, 38));
        userField.setFont(UniMatrixTheme.font(Font.PLAIN, 13f));
        gbc.insets = new Insets(0, 0, 16, 0);
        form.add(userField, gbc);

        // Password Label
        gbc.gridy++;
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UniMatrixTheme.font(Font.BOLD, 12f));
        passLabel.setForeground(UniMatrixTheme.TEXT);
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(passLabel, gbc);

        // Password Input
        gbc.gridy++;
        passField.setPreferredSize(new Dimension(280, 38));
        passField.setFont(UniMatrixTheme.font(Font.PLAIN, 13f));
        gbc.insets = new Insets(0, 0, 24, 0);
        form.add(passField, gbc);

        // Sign In Button
        gbc.gridy++;
        JButton loginBtn = UniMatrixTheme.primaryButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(280, 40));
        loginBtn.addActionListener(e -> handleLogin());
        gbc.insets = new Insets(0, 0, 10, 0);
        form.add(loginBtn, gbc);
        gbc.gridy++;
        JButton create = UniMatrixTheme.secondaryButton("Create Account");
        create.setPreferredSize(new Dimension(280, 38));
        create.addActionListener(e -> createAccount());
        gbc.insets = new Insets(0, 0, 0, 0);
        form.add(create, gbc);

        // Pressing Enter on fields triggers login
        passField.addActionListener(e -> handleLogin());
        userField.addActionListener(e -> handleLogin());

        return form;
    }

    private void handleLogin() {
        String username = userField.getText().trim();
        String password = new String(passField.getPassword());

        BackgroundTask.run((JComponent)getContentPane(), () -> { com.unimatrix.dao.SchemaCompatibility.check(); return username.equals("admin")&&password.equals("unimatrix123")||com.unimatrix.dao.AccountDAO.authenticate(username,password)!=null; }, valid -> {
            if(valid){
                dispose();
                new MainDashboardView(username,new DatabaseDashboardProvider(),()->new LoginView().setVisible(true)).setVisible(true);
            } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Try admin / unimatrix123", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    private void createAccount(){JTextField username=new JTextField();JPasswordField password=new JPasswordField();if(JOptionPane.showConfirmDialog(this,new Object[]{"Username",username,"Password",password},"Create Account",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;try{com.unimatrix.dao.AccountDAO.create(username.getText().trim(),new String(password.getPassword()));JOptionPane.showMessageDialog(this,"Account created. You can now sign in.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Could not create account",JOptionPane.ERROR_MESSAGE);}}

    public static void main(String[] args) {
        UniMatrixTheme.install();
        EventQueue.invokeLater(() -> new LoginView().setVisible(true));
    }
}
