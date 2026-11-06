package com.unimatrix.view;

import com.unimatrix.view.components.SidebarButton;
import com.unimatrix.view.theme.UniMatrixTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Application shell: dark sidebar + header + CardLayout workspace.
 * Other screens plug in through registerPage(); unregistered pages show a placeholder.
 */
public class MainDashboardView extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel workspace = new JPanel(cardLayout);
    private final JLabel pageTitle = UniMatrixTheme.title("Dashboard");
    private final Map<Page, SidebarButton> navButtons = new EnumMap<>(Page.class);
    private final Map<Page, JComponent> wrappers = new EnumMap<>(Page.class);
    private final ButtonGroup navGroup = new ButtonGroup();
    private final DashboardHomePanel home;
    private final Runnable onLogout;

    public MainDashboardView(String username, DashboardDataProvider provider, Runnable onLogout) {
        super("UniMatrix");
        this.onLogout = onLogout;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) {
                if (canLeave("Close UniMatrix?")) { dispose(); System.exit(0); }
            }
        });
        setMinimumSize(new Dimension(1000, 640));
        setSize(1200, 760);
        setLocationRelativeTo(null);

        home = new DashboardHomePanel(provider, this::navigateTo);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(username), BorderLayout.CENTER);
        setContentPane(root);

        for (Page p : Page.values()) {
            JComponent comp;
            switch (p) {
                case DASHBOARD: comp = home; break;
                case MANAGE_DATA: comp = new ManageDataPanel(); break;
                case MANAGE_SCHEDULES: comp = new ManageSchedulesPanel(); break;
                default: comp = placeholder(p); break;
            }
            registerPage(p, comp);
        }

        navigateTo(Page.DASHBOARD);
    }

    /** Replaces the page (including the placeholder) with a real screen. */
    public void registerPage(Page page, JComponent component) {
        JComponent oldWrapper = wrappers.remove(page);
        if (oldWrapper != null) workspace.remove(oldWrapper);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(component, BorderLayout.CENTER);
        wrappers.put(page, wrapper);
        workspace.add(wrapper, page.name());
        workspace.revalidate();
        workspace.repaint();
    }

    public void navigateTo(Page page) {
        navButtons.get(page).setSelected(true);
        pageTitle.setText(page.title());
        cardLayout.show(workspace, page.name());
        if (page == Page.DASHBOARD) home.refresh();
        if (page == Page.MANAGE_SCHEDULES) {
            JComponent wrapper = wrappers.get(page);
            if (wrapper != null && wrapper.getComponent(0) instanceof ManageSchedulesPanel schedules) schedules.refresh();
        }
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(UniMatrixTheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(24, 15, 20, 15));

        JLabel brand = new JLabel("UniMatrix");
        brand.setFont(UniMatrixTheme.font(Font.BOLD, 20f));
        brand.setForeground(Color.WHITE);
        brand.setBorder(new EmptyBorder(0, 16, 24, 0));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        for (Page p : Page.values()) {
            SidebarButton b = new SidebarButton(p.title());
            b.addActionListener(e -> navigateTo(p));
            navGroup.add(b);
            navButtons.put(p, b);
            nav.add(b);
            nav.add(Box.createVerticalStrut(4));
        }

        SidebarButton logout = new SidebarButton("Log out");
        logout.addActionListener(e -> { logout.setSelected(false); confirmLogout(); });

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(logout, BorderLayout.SOUTH);
        return sidebar;
    }

    private boolean hasState(Component component, String property) {
        if (component instanceof JComponent c && Boolean.TRUE.equals(c.getClientProperty(property))) return true;
        if (component instanceof Container container) for (Component child : container.getComponents())
            if (hasState(child, property)) return true;
        return false;
    }

    private boolean canLeave(String action) {
        if (hasState(workspace, "busy")) {
            JOptionPane.showMessageDialog(this, "Wait for the current load/save/export to finish before closing or logging out.");
            return false;
        }
        boolean unsaved = hasState(workspace, "dirty");
        return JOptionPane.showConfirmDialog(this, unsaved ? action + " Unsaved edits will be discarded." : action,
                "UniMatrix", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void confirmLogout() {
        if (canLeave("Log out of UniMatrix?")) {
            dispose();
            if (onLogout != null) onLogout.run();
        }
    }

    private JPanel buildMain(String username) {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UniMatrixTheme.CONTENT_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UniMatrixTheme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UniMatrixTheme.BORDER),
                new EmptyBorder(16, UniMatrixTheme.PAD, 16, UniMatrixTheme.PAD)));
        JLabel user = UniMatrixTheme.muted("Signed in as " + username);
        header.add(pageTitle, BorderLayout.WEST);
        header.add(user, BorderLayout.EAST);

        workspace.setOpaque(false);
        workspace.setBorder(new EmptyBorder(UniMatrixTheme.PAD, UniMatrixTheme.PAD,
                UniMatrixTheme.PAD, UniMatrixTheme.PAD));

        main.add(header, BorderLayout.NORTH);
        main.add(workspace, BorderLayout.CENTER);
        return main;
    }

    private JComponent placeholder(Page page) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(UniMatrixTheme.muted(page.title() + " — not built yet"));
        return p;
    }
}