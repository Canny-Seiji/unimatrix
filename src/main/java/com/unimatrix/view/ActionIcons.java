package com.unimatrix.view;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JButton;

/** Shared presentation for toolbar actions; listeners remain with their panels. */
final class ActionIcons {
    private ActionIcons() { }

    static JButton button(String action, String resource) {
        JButton button = new JButton(action);
        button.setToolTipText(action);
        button.getAccessibleContext().setAccessibleName(action);
        URL url = ActionIcons.class.getResource("/com/unimatrix/icons/" + resource);
        if (url != null) {
            ImageIcon source = new ImageIcon(url);
            button.setIcon(new ImageIcon(
                    source.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            button.setText("");
            button.setMargin(new Insets(6, 6, 6, 6));
            button.setPreferredSize(new Dimension(36, 36));
        }
        return button;
    }
}
