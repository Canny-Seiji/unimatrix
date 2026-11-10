package com.unimatrix.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.unimatrix.view.components.NavIcon;
import com.unimatrix.view.components.SidebarButton;
import com.unimatrix.view.theme.UniMatrixTheme;

/** Application shell: sliding collapsible sidebar + header + CardLayout workspace. */
public class MainDashboardView extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel workspace = new JPanel(cardLayout);
    private final JLabel pageTitle = UniMatrixTheme.title("Dashboard");
    private final Map<Page, SidebarButton> navButtons = new EnumMap<>(Page.class);
    private final Map<Page, JComponent> wrappers = new EnumMap<>(Page.class);
    private final ButtonGroup navGroup = new ButtonGroup();
    private final DashboardHomePanel home;
    private final Runnable onLogout;
    private JLabel headerIcon;

    // Sliding sidebar components
    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final Timer slideTimer;
    private boolean isSidebarExpanded = false;
    private final int COLLAPSED_WIDTH = 64;
    private final int EXPANDED_WIDTH = 230;

    private final Map<SidebarButton, String> buttonLabels = new LinkedHashMap<>();
    private JButton toggleBtn;

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
        root.setBackground(UniMatrixTheme.CONTENT_BG);

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(username), BorderLayout.CENTER);
        setContentPane(root);

        for (Page p : Page.values()) {
            JComponent comp;
            switch (p) {
                case DASHBOARD: comp = home; break;
                case MANAGE_DATA: comp = new ManageDataPanel(); break;
                case MANAGE_SCHEDULES: comp = new ManageSchedulesPanel(); break;
                case ACCOUNT: comp = new AccountPanel(username, ignored -> { dispose(); if(onLogout!=null) onLogout.run(); }, () -> refreshHeaderIcon(username)); break;
                default: comp = placeholder(p); break;
            }
            registerPage(p, comp);
        }

        navigateTo(Page.DASHBOARD);

        slideTimer = new Timer(10, e -> {
            int current = sidebar.getPreferredSize().width;
            int target = isSidebarExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
            int next = current + Integer.signum(target - current) * Math.min(15, Math.abs(target - current));
            sidebar.setPreferredSize(new Dimension(next, 0));
            sidebar.revalidate();
            if (next == target) ((Timer) e.getSource()).stop();
        });
    }

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

    @Override
    public void dispose() {
        if (slideTimer != null) slideTimer.stop();
        super.dispose();
    }

    public void navigateTo(Page page) {
        if (navButtons.containsKey(page)) {
            navButtons.get(page).setSelected(true);
        }
        pageTitle.setText(page.title());
        cardLayout.show(workspace, page.name());
        if (page == Page.DASHBOARD) home.refresh();
        if (page == Page.MANAGE_SCHEDULES) {
            JComponent wrapper = wrappers.get(page);
            if (wrapper != null && wrapper.getComponent(0) instanceof ManageSchedulesPanel schedules) schedules.refresh();
        }
    }

    private JPanel buildSidebar() {
        sidebar.setBackground(UniMatrixTheme.SURFACE);
        sidebar.setPreferredSize(new Dimension(COLLAPSED_WIDTH, 0));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UniMatrixTheme.BORDER),
                new EmptyBorder(24, 10, 20, 10)
        ));

        toggleBtn = new JButton("", new NavIcon(NavIcon.Type.MENU));
        toggleBtn.setIconTextGap(14);
        toggleBtn.setToolTipText("Expand or collapse navigation");
        toggleBtn.getAccessibleContext().setAccessibleName("Expand or collapse navigation");
        styleToggleBrandButton(toggleBtn);
        toggleBtn.addActionListener(e -> toggleSidebar());

        JPanel brandWrapper = new JPanel(new BorderLayout());
        brandWrapper.setOpaque(false);
        brandWrapper.setBorder(new EmptyBorder(0, 0, 24, 0));
        brandWrapper.add(toggleBtn, BorderLayout.CENTER);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

        Map<Page, NavIcon.Type> iconMap = Map.of(
            Page.DASHBOARD, NavIcon.Type.DASHBOARD,
            Page.MANAGE_DATA, NavIcon.Type.DATA,
            Page.MANAGE_SCHEDULES, NavIcon.Type.SCHEDULE,
            Page.ACCOUNT, NavIcon.Type.ACCOUNT
        );

        for (Page p : Page.values()) {
            NavIcon.Type iconType = iconMap.getOrDefault(p, NavIcon.Type.DASHBOARD);
            SidebarButton b = new SidebarButton(new NavIcon(iconType), "");
            b.setToolTipText(p.title());
            b.getAccessibleContext().setAccessibleName(p.title());
            b.addActionListener(e -> navigateTo(p));
            navGroup.add(b);
            navButtons.put(p, b);
            nav.add(b);
            nav.add(Box.createVerticalStrut(4));

            buttonLabels.put(b, p.title());
        }

        SidebarButton logoutBtn = new SidebarButton(new NavIcon(NavIcon.Type.LOGOUT), "");
        logoutBtn.setToolTipText("Log out");
        logoutBtn.getAccessibleContext().setAccessibleName("Log out");
        logoutBtn.addActionListener(e -> { logoutBtn.setSelected(false); confirmLogout(); });
        buttonLabels.put(logoutBtn, "Log out");

        sidebar.add(brandWrapper, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(logoutBtn, BorderLayout.SOUTH);
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

    private void toggleSidebar() {
        isSidebarExpanded = !isSidebarExpanded;

        toggleBtn.setText(isSidebarExpanded ? "UniMatrix" : "");

        for (Map.Entry<SidebarButton, String> entry : buttonLabels.entrySet()) {
            entry.getKey().setText(isSidebarExpanded ? entry.getValue() : "");
        }

        slideTimer.start();
    }

    private void styleToggleBrandButton(JButton btn) {
        btn.setFont(UniMatrixTheme.font(Font.BOLD, 15f));
        btn.setForeground(UniMatrixTheme.TEXT);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(200, 40));

        // Matches SidebarButton styling and margin for alignment
        UniMatrixTheme.style(btn,
                "background:" + UniMatrixTheme.hex(UniMatrixTheme.SURFACE)
                + "; foreground:" + UniMatrixTheme.hex(UniMatrixTheme.TEXT)
                + "; hoverBackground:#F1F5F9"
                + "; pressedBackground:#E2E8F0"
                + "; borderWidth:0; focusWidth:0; arc:8; margin:8,13,8,13");

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
        JPanel userBox=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));userBox.setOpaque(false);headerIcon=new JLabel(UIManager.getIcon("FileView.fileIcon"));JLabel user = UniMatrixTheme.muted("Signed in as " + username);userBox.add(user);userBox.add(headerIcon);refreshHeaderIcon(username);
        header.add(pageTitle, BorderLayout.WEST);
        header.add(userBox, BorderLayout.EAST);

        workspace.setOpaque(false);
        workspace.setBorder(new EmptyBorder(UniMatrixTheme.PAD, UniMatrixTheme.PAD,
                UniMatrixTheme.PAD, UniMatrixTheme.PAD));

        main.add(header, BorderLayout.NORTH);
        main.add(workspace, BorderLayout.CENTER);
        return main;
    }
    private void refreshHeaderIcon(String username){if(headerIcon==null)return;try{var account=com.unimatrix.dao.AccountDAO.find(username);headerIcon.setIcon(account!=null&&account.iconPath()!=null&&!account.iconPath().isBlank()?scaledIcon(account.iconPath(),28,28):UIManager.getIcon("FileView.fileIcon"));headerIcon.setPreferredSize(new Dimension(28,28));headerIcon.revalidate();headerIcon.repaint();}catch(Exception ignored){}}
    private static ImageIcon scaledIcon(String path,int width,int height){ImageIcon source=new ImageIcon(path);Image image=source.getImage();return new ImageIcon(image.getScaledInstance(width,height,Image.SCALE_SMOOTH));}

    private JComponent placeholder(Page page) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.add(UniMatrixTheme.muted(page.title() + " — not built yet"));
        return p;
    }
}
