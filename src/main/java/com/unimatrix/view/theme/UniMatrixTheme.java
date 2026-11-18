package com.unimatrix.view.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLightLaf;

/** Central place for FlatLaf setup, palette, fonts and style helpers. Call install() once, before any UI is created. */
public final class UniMatrixTheme {

    private UniMatrixTheme() {}

    // Palette
    public static final Color SIDEBAR_BG       = new Color(0x172B4D);
    public static final Color SIDEBAR_HOVER    = new Color(0x233F65);
    public static final Color SIDEBAR_SELECTED = new Color(0x2457D6);
    public static final Color SIDEBAR_TEXT     = new Color(0xD5E1F2);
    public static final Color CONTENT_BG       = new Color(0xF4F7FB);
    public static final Color SURFACE          = Color.WHITE;
    public static final Color BORDER           = new Color(0xDCE4EF);
    public static final Color ACCENT           = new Color(0x2457D6);
    public static final Color ACCENT_HOVER     = new Color(0x1D46B0);
    public static final Color TEXT             = new Color(0x172B4D);
    public static final Color TEXT_MUTED       = new Color(0x526176);
    public static final Color SUCCESS          = new Color(0x0F766E);
    public static final Color DANGER           = new Color(0xB42338);

    public static final Color ACCENT_SOFT      = new Color(0xEAF0FC);
    public static final Color TABLE_HEADER     = new Color(0xEDF2FA);
    public static final Color ROW_ALTERNATE    = new Color(0xF5F8FC);
    public static final Color DANGER_SOFT      = new Color(0xFDE8EC);
    public static final Color GRID_MINOR       = new Color(0xEDF1F7);

    // Spacing
    public static final int PAD = 24;
    public static final int GAP = 16;
    public static final int CARD_ARC = 12;

    private static String fontFamily = Font.SANS_SERIF;

    public static void install() {
        fontFamily = resolveFontFamily();
        UIManager.put("defaultFont", new Font(fontFamily, Font.PLAIN, 13));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.borderColor", BORDER);
        UIManager.put("Button.hoverBackground", ACCENT_SOFT);
        UIManager.put("Button.pressedBackground", TABLE_HEADER);
        UIManager.put("Button.focusedBorderColor", ACCENT);
        UIManager.put("Button.margin", new java.awt.Insets(7, 12, 7, 12));
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.focusedBorderColor", ACCENT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.inactiveBackground", CONTENT_BG);
        UIManager.put("TextField.margin", new java.awt.Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.margin", new java.awt.Insets(6, 10, 6, 10));
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.padding", new java.awt.Insets(5, 8, 5, 8));
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", ACCENT_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.selectionInactiveBackground", TABLE_HEADER);
        UIManager.put("Table.selectionInactiveForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", TABLE_HEADER);
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("TableHeader.font", font(Font.BOLD, 13f));
        UIManager.put("ScrollPane.borderColor", BORDER);
        UIManager.put("ScrollBar.track", CONTENT_BG);
        UIManager.put("ScrollBar.thumb", new Color(0xB4C3D8));
        UIManager.put("Tree.background", SURFACE);
        UIManager.put("Tree.foreground", TEXT);
        UIManager.put("Tree.selectionBackground", ACCENT_SOFT);
        UIManager.put("Tree.selectionForeground", TEXT);
        UIManager.put("List.selectionBackground", ACCENT_SOFT);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ToolTip.background", TABLE_HEADER);
        UIManager.put("ToolTip.foreground", TEXT);
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
                + "; pressedBackground:" + hex(ACCENT_HOVER) + "; borderWidth:0; focusWidth:0; arc:12; margin:8,18,8,18");
        b.setFont(font(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        style(b, "background:#FFFFFF; foreground:" + hex(TEXT) + "; borderColor:" + hex(BORDER)
                + "; hoverBackground:" + hex(CONTENT_BG) + "; focusWidth:0; arc:12; margin:8,18,8,18");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // Alias for secondaryButton to prevent method reference collisions
    public static JButton button(String text) {
        return secondaryButton(text);
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 22f));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(Font.BOLD, 13f));
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        return l;
    }
}
