package com.unimatrix.view.components;

import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** White rounded surface with a subtle border. */
public class Card extends JPanel {

    public Card(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(20, 20, 20, 20));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = UniMatrixTheme.CARD_ARC;
        g2.setColor(UniMatrixTheme.SURFACE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(UniMatrixTheme.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}