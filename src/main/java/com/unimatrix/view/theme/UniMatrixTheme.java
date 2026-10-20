package com.unimatrix.view.theme;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** Central place for FlatLaf setup, palette, fonts and style helpers. Call install() once, before any UI is created. */
public final class UniMatrixTheme {

    private UniMatrixTheme() {}

    // Palette
    public static final Color SIDEBAR_BG       = new Color(0x212529);
    public static final Color SIDEBAR_HOVER    = new Color(0x2B3035);
    public static final Color SIDEBAR_SELECTED = new Color(0x343A40);
    public static final Color SIDEBAR_TEXT     = new Color(0xADB5BD);
    public static final Color CONTENT_BG       = new Color(0xF5F7FA);
    public static final Color SURFACE          = Color.WHITE;
    public static final Color BORDER           = new Color(0xE3E8EF);
    public static final Color ACCENT           = new Color(0x2F6FED);
    public static final Color ACCENT_HOVER     = new Color(0x2559C7);
    public static final Color TEXT             = new Color(0x1F2937);
    public static final Color TEXT_MUTED       = new Color(0x6B7280);
    public static final Color SUCCESS          = new Color(0x198754);
    public static final Color DANGER           = new Color(0xDC3545);

    // Spacing
    public static final int PAD = 24;
    public static final int GAP = 16;
    public static final int CARD_ARC = 12;

    private static String fontFamily = Font.SANS_SERIF;

    public static void install() {
        fontFamily = resolveFontFamily();
        UIManager.put("defaultFont", new Font(fontFamily, Font.PLAIN, 13));
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);
        FlatLightLaf.setup();
    }

    private static String resolveFontFamily() {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        return Arrays.asList(installed).contains("Segoe UI") ? "Segoe UI" : Font.SANS_SERIF;
    }

    public static Font font(int style, float size) {
        return new Font(fontFamily, style, 1).deriveFont(style, size);
    }

    public static String hex(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }

    public static void style(JComponent c, String flatStyle) {
        c.putClientProperty(FlatClientProperties.STYLE, flatStyle);
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        style(b, "background:" + hex(ACCENT) + "; foreground:#FFFFFF; hoverBackground:" + hex(ACCENT_HOVER)
                + "; pressedBackground:" + hex(ACCENT_HOVER) + "; borderWidth:0; focusWidth:0; arc:8; margin:8,18,8,18");
        b.setFont(font(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        style(b, "background:#FFFFFF; foreground:" + hex(TEXT) + "; borderColor:" + hex(BORDER)
                + "; hoverBackground:" + hex(CONTENT_BG) + "; focusWidth:0; arc:8; margin:8,18,8,18");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 22f));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        return l;
    }
}