package com.unimatrix.view;

import com.unimatrix.view.theme.UniMatrixTheme;
import javax.swing.*;
import com.unimatrix.dao.AccountDAO;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

/** Minimal account screen for the current hardcoded-login flow. */
final class AccountPanel extends JPanel {
    @SuppressWarnings("unused") private final Consumer<Void> onDeleted;@SuppressWarnings("unused") private final Runnable onChanged;
    AccountPanel(String username, Consumer<Void> onDeleted, Runnable onChanged) {
        super(new GridBagLayout());
        this.onDeleted=onDeleted;this.onChanged=onChanged;
        setOpaque(false);
        JPanel card=new JPanel(new GridBagLayout());
        card.setBackground(UniMatrixTheme.SURFACE);
        card.setBorder(new EmptyBorder(28,32,28,32));
        GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.anchor=GridBagConstraints.WEST;g.fill=GridBagConstraints.HORIZONTAL;g.weightx=1;g.insets=new Insets(4,4,12,4);
        JLabel icon=new JLabel(UIManager.getIcon("FileView.fileIcon"));icon.setHorizontalAlignment(SwingConstants.CENTER);g.anchor=GridBagConstraints.CENTER;card.add(icon,g);
        g.gridy=1;card.add(UniMatrixTheme.title("Account"),g);
        g.gridy=2;card.add(UniMatrixTheme.muted("Signed-in account details"),g);
        g.gridy=3;g.insets=new Insets(22,4,4,4);card.add(new JLabel("Username"),g);
        g.gridy=4;g.insets=new Insets(0,4,12,4);JTextField user=new JTextField(username);user.setEditable(true);card.add(user,g);
        g.gridy=5;g.insets=new Insets(4,4,4,4);card.add(new JLabel("Role"),g);
        g.gridy=6;g.insets=new Insets(0,4,4,4);JTextField role=new JTextField("Administrator");role.setEditable(false);card.add(role,g);
        g.gridy=7;JPanel actions=new JPanel();JButton password=new JButton("Change Password"),iconButton=new JButton("Change Account Icon"),save=new JButton("Save Username"),delete=new JButton("Delete Account");actions.add(password);actions.add(iconButton);actions.add(save);actions.add(delete);card.add(actions,g);password.addActionListener(e->{JPasswordField p=new JPasswordField();if(JOptionPane.showConfirmDialog(this,new Object[]{"New password",p},"Change Password",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)try{AccountDAO.updateByUsername(username,username,new String(p.getPassword()),null);onChanged.run();JOptionPane.showMessageDialog(this,"Password changed.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Account Error",JOptionPane.ERROR_MESSAGE);}});iconButton.addActionListener(e->{JFileChooser chooser=new JFileChooser();if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)try{AccountDAO.updateByUsername(username,username,null,chooser.getSelectedFile().getAbsolutePath());onChanged.run();JOptionPane.showMessageDialog(this,"Account icon updated.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Account Error",JOptionPane.ERROR_MESSAGE);}});save.addActionListener(e->{String next=user.getText().trim();try{AccountDAO.updateByUsername(username,next,null,null);onChanged.run();JOptionPane.showMessageDialog(this,"Username updated. Log in again with the new username.");}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Account Error",JOptionPane.ERROR_MESSAGE);}});delete.addActionListener(e->{if(JOptionPane.showConfirmDialog(this,"Delete this account permanently?","Delete Account",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;try{AccountDAO.deleteByUsername(username);JOptionPane.showMessageDialog(this,"Account deleted.");onDeleted.accept(null);}catch(Exception ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Account Error",JOptionPane.ERROR_MESSAGE);}});add(card);
    }
}
