package com.unimatrix.view.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.Icon;

/** Vector line icons rendered directly with Java Graphics2D. */
public class NavIcon implements Icon {

    public enum Type {
        MENU, DASHBOARD, DATA, SCHEDULE, EXPORT, LOGOUT, ACCOUNT
    }

    private final Type type;
    private final int size;

    public NavIcon(Type type) {
        this(type, 18);
    }

    public NavIcon(Type type, int size) {
        this.type = type;
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Detect if parent button is selected or pressed -> force pure White (#FFFFFF)
        Color iconColor = c != null ? c.getForeground() : Color.DARK_GRAY;
        if (c instanceof AbstractButton) {
            AbstractButton btn = (AbstractButton) c;
            if (btn.isSelected() || btn.getModel().isPressed()) {
                iconColor = Color.WHITE;
            }
        }

        g2.setColor(iconColor);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int p = 2; // padding
        int w = size - (p * 2);
        int h = size - (p * 2);

        switch (type) {
            case MENU:
                // 3 horizontal lines
                g2.drawLine(x + p, y + p + 3, x + p + w, y + p + 3);
                g2.drawLine(x + p, y + p + (h / 2), x + p + w, y + p + (h / 2));
                g2.drawLine(x + p, y + p + h - 3, x + p + w, y + p + h - 3);
                break;

            case DASHBOARD:
                // 4 grid squares
                int s = (w - 3) / 2;
                g2.drawRoundRect(x + p, y + p, s, s, 3, 3);
                g2.drawRoundRect(x + p + s + 3, y + p, s, s, 3, 3);
                g2.drawRoundRect(x + p, y + p + s + 3, s, s, 3, 3);
                g2.drawRoundRect(x + p + s + 3, y + p + s + 3, s, s, 3, 3);
                break;

            case ACCOUNT:
                g2.drawOval(x + p + w / 2 - 3, y + p, 6, 6);
                g2.drawArc(x + p + 1, y + p + 8, w - 2, h, 0, 180);
                break;

            case DATA:
                // Rectangular table / rows representation
                g2.drawRoundRect(x + p, y + p + 1, w, h - 2, 4, 4);
                g2.drawLine(x + p, y + p + (h / 2), x + p + w, y + p + (h / 2));
                g2.drawLine(x + p + (w / 2), y + p + (h / 2), x + p + (w / 2), y + p + h - 2);
                break;

            case SCHEDULE:
                // Calendar outline + header line
                g2.drawRoundRect(x + p, y + p + 2, w, h - 2, 4, 4);
                g2.drawLine(x + p, y + p + 6, x + p + w, y + p + 6);
                g2.drawLine(x + p + 4, y + p, x + p + 4, y + p + 3);
                g2.drawLine(x + p + w - 4, y + p, x + p + w - 4, y + p + 3);
                break;

            case EXPORT:
                // Tray + Upward arrow
                g2.drawLine(x + p, y + p + h - 2, x + p + w, y + p + h - 2);
                g2.drawLine(x + p, y + p + h - 6, x + p, y + p + h - 2);
                g2.drawLine(x + p + w, y + p + h - 6, x + p + w, y + p + h - 2);
                // Arrow
                g2.drawLine(x + p + (w / 2), y + p + 2, x + p + (w / 2), y + p + h - 6);
                g2.drawLine(x + p + (w / 2), y + p + 2, x + p + (w / 2) - 4, y + p + 6);
                g2.drawLine(x + p + (w / 2), y + p + 2, x + p + (w / 2) + 4, y + p + 6);
                break;

            case LOGOUT:
                // Power arc + top line
                g2.drawArc(x + p, y + p + 1, w, h - 1, -45, 270);
                g2.drawLine(x + p + (w / 2), y + p, x + p + (w / 2), y + p + (h / 2));
                break;
        }

        g2.dispose();
    }
}
