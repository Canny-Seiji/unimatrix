package com.unimatrix.view.components;

import com.unimatrix.view.theme.UniMatrixTheme;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Full-width underline navigation around the existing page components. */
public final class UnderlineTabs extends JPanel {
    private final JPanel header = new JPanel(new GridLayout(1, 0));
    private final CardLayout layout = new CardLayout();
    private final JPanel content = new JPanel(layout);
    private final List<JButton> buttons = new ArrayList<>();
    private int selectedIndex = -1;

    public UnderlineTabs() {
        super(new BorderLayout(0, UniMatrixTheme.GAP));
        setOpaque(false);
        header.setOpaque(false);
        content.setOpaque(false);
        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    public void addTab(String title, Component component) {
        int index = buttons.size();
        JButton button = new JButton(title);
        button.setFont(UniMatrixTheme.font(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 42));
        button.setToolTipText(title);
        button.addActionListener(e -> setSelectedIndex(index));
        buttons.add(button);
        header.add(button);
        content.add(component, Integer.toString(index));
        if (selectedIndex < 0) setSelectedIndex(0);
        else updateStyles();
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= buttons.size()) throw new IndexOutOfBoundsException(index);
        selectedIndex = index;
        layout.show(content, Integer.toString(index));
        updateStyles();
    }

    public int getSelectedIndex() { return selectedIndex; }
    public int getTabCount() { return buttons.size(); }
    public Component getComponentAt(int index) { return content.getComponent(index); }

    private void updateStyles() {
        for (int i = 0; i < buttons.size(); i++) {
            JButton button = buttons.get(i);
            boolean selected = i == selectedIndex;
            button.setBackground(selected ? Color.WHITE : new Color(0xF1F5F9));
            button.setForeground(selected ? new Color(0x0F172A) : new Color(0x64748B));
            button.setBorder(BorderFactory.createMatteBorder(0, 0, selected ? 3 : 1, 0,
                    selected ? new Color(0x3B82F6) : UniMatrixTheme.BORDER));
        }
    }
}
