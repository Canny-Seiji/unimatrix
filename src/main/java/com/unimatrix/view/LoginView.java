package com.unimatrix.view;

import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginView extends JFrame {
    private final JTextField userField = new JTextField();
    private final JPasswordField passField = new JPasswordField();

    public LoginView() {
        super("UniMatrix - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(420, 380);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Panel using GridBagLayout for strict control over alignment and sizing
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UniMatrixTheme.CONTENT_BG);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; 
        gbc.gridy = 0; 
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 12, 0);

        // Title
        JLabel title = UniMatrixTheme.title("UniMatrix Login");
        panel.add(title, gbc);

        // Subtitle
        gbc.gridy++;
        panel.add(UniMatrixTheme.muted("Enter credentials to access system"), gbc);

        // Username Label
        gbc.gridy++;
        gbc.insets = new Insets(8, 0, 4, 0);
        panel.add(new JLabel("Username:"), gbc);

        // Username TextField (Explicit size to ensure focus and typing work)
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 12, 0);
        userField.setPreferredSize(new Dimension(300, 35));
        userField.setFont(UniMatrixTheme.font(Font.PLAIN, 14f));
        panel.add(userField, gbc);

        // Password Label
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        panel.add(new JLabel("Password:"), gbc);

        // Password Field (Explicit size)
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 20, 0);
        passField.setPreferredSize(new Dimension(300, 35));
        passField.setFont(UniMatrixTheme.font(Font.PLAIN, 14f));
        panel.add(passField, gbc);

        // Login Button
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        JButton loginBtn = UniMatrixTheme.primaryButton("Sign In");
        loginBtn.setPreferredSize(new Dimension(300, 38));
        loginBtn.addActionListener(e -> handleLogin());
        panel.add(loginBtn, gbc);
        gbc.gridy++;gbc.insets=new Insets(8,0,0,0);JButton create=UniMatrixTheme.secondaryButton("Create Account");create.setPreferredSize(new Dimension(300,38));create.addActionListener(e->createAccount());panel.add(create,gbc);

        // Allow pressing Enter on password field to trigger login
        passField.addActionListener(e -> handleLogin());

        setContentPane(panel);
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
