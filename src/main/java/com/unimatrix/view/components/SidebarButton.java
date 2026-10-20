package com.unimatrix.view.components;

import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import java.awt.*;

/** Navigation entry for the dark sidebar. Use inside a ButtonGroup so one stays selected. */
public class SidebarButton extends JToggleButton {

    public SidebarButton(String text) {
        super(text);
        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(UniMatrixTheme.font(Font.PLAIN, 14f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        setPreferredSize(new Dimension(200, 42));
        UniMatrixTheme.style(this,
                "background:" + UniMatrixTheme.hex(UniMatrixTheme.SIDEBAR_BG)
                + "; foreground:" + UniMatrixTheme.hex(UniMatrixTheme.SIDEBAR_TEXT)
                + "; hoverBackground:" + UniMatrixTheme.hex(UniMatrixTheme.SIDEBAR_HOVER)
                + "; pressedBackground:" + UniMatrixTheme.hex(UniMatrixTheme.SIDEBAR_HOVER)
                + "; selectedBackground:" + UniMatrixTheme.hex(UniMatrixTheme.SIDEBAR_SELECTED)
                + "; selectedForeground:#FFFFFF"
                + "; borderWidth:0; focusWidth:0; arc:8; margin:8,16,8,16");
    }
}