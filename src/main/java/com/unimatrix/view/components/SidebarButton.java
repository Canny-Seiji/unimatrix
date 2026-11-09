package com.unimatrix.view.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.unimatrix.view.theme.UniMatrixTheme;

/** Navigation entry for the sidebar with white icon and white text on active dark blue background. */
public class SidebarButton extends JToggleButton {

    public SidebarButton(Icon icon, String text) {
        super(text, icon);
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(14);
        setFont(UniMatrixTheme.font(Font.PLAIN, 13f));
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setPreferredSize(new Dimension(200, 40));

        // Style button with FlatLaf properties
        UniMatrixTheme.style(this,
                "background:" + UniMatrixTheme.hex(UniMatrixTheme.SURFACE)
                + "; foreground:" + UniMatrixTheme.hex(UniMatrixTheme.TEXT_MUTED)
                + "; hoverBackground:#F1F5F9"
                + "; hoverForeground:#1E293B"
                + "; pressedBackground:#1D4ED8"
                + "; pressedForeground:#FFFFFF"
                + "; selectedBackground:#2563EB"
                + "; selectedForeground:#FFFFFF"
                + "; borderWidth:0; focusWidth:0; arc:8; margin:8,13,8,13");

        // Dynamically update text color on toggle selection
        addItemListener(e -> updateColors());
        updateColors();
    }

    public SidebarButton(String text) {
        this(null, text);
    }

    public SidebarButton(Icon icon) {
        this(icon, "");
    }

    private void updateColors() {
        if (isSelected()) {
            setForeground(Color.WHITE);
        } else {
            setForeground(UniMatrixTheme.TEXT_MUTED);
        }
    }

    @Override
    public void setSelected(boolean b) {
        super.setSelected(b);
        updateColors();
    }
}
