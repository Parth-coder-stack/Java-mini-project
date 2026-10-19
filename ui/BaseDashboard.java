package ui;

import model.Person;
import service.CourseService;
import service.NotificationService;
import service.Session;
import service.WaitlistDaemon;
import ui.common.NotificationsPage;
import ui.components.Avatar;
import ui.components.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Common dashboard frame: dark sidebar with navigation, page header and status bar.
 * StudentDashboard, FacultyDashboard and AdminDashboard extend this and only add pages.
 */
public abstract class BaseDashboard extends JPanel {

    protected final MainFrame frame;
    protected final Session session;

    private final CardLayout cards = new CardLayout();
    private final JPanel pageHost = new JPanel(cards);
    private final JPanel nav = new JPanel();
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();
    private final Map<String, String> descriptions = new LinkedHashMap<>();
    private final JLabel pageTitle = UIStyles.label(" ", UIStyles.H1, UIStyles.TEXT);
    private final JLabel pageDescription = UIStyles.muted(" ");
    private final JLabel semesterLabel = UIStyles.muted(" ");
    private final JLabel statusLabel = UIStyles.muted("Ready");
    private final Consumer<String> waitlistListener;
    private String currentPage;

    protected BaseDashboard(MainFrame frame, Session session) {
        super(new BorderLayout());
        this.frame = frame;
        this.session = session;
        setBackground(UIStyles.BG);

        add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UIStyles.BG);
        main.add(buildHeader(), BorderLayout.NORTH);
        pageHost.setBackground(UIStyles.BG);
        main.add(pageHost, BorderLayout.CENTER);
        main.add(buildStatusBar(), BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);

        buildPages();
        if (!pages.isEmpty()) {
            showPage(pages.keySet().iterator().next());
        }
        loadSemester();
        refreshNotificationBadge();

        // The waitlist daemon runs on its own thread, so hop back to the EDT before touching Swing.
        waitlistListener = msg -> SwingUtilities.invokeLater(() -> {
            setStatus("Waitlist: " + msg);
            refreshCurrentPage();
            refreshNotificationBadge();
        });
        WaitlistDaemon.addListener(waitlistListener);
    }

    /** Subclasses call {@link #addPage} for each page they want in the sidebar. */
    protected abstract void buildPages();

    protected void addPage(String title, String description, Page page) {
        pages.put(title, page);
        descriptions.put(title, description);
        pageHost.add(page, title);
        NavButton b = new NavButton(title);
        b.addActionListener(e -> showPage(title));
        navButtons.put(title, b);
        nav.add(b);
        nav.add(Box.createVerticalStrut(2));
    }

    public void showPage(String title) {
        Page page = pages.get(title);
        if (page == null) {
            return;
        }
        currentPage = title;
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(title));
        }
        pageTitle.setText(title);
        pageDescription.setText(descriptions.get(title));
        cards.show(pageHost, title);
        page.refresh();
    }

    public void refreshCurrentPage() {
        if (currentPage != null) {
            pages.get(currentPage).refresh();
        }
    }

    public Session getSession() {
        return session;
    }

    public MainFrame getFrame() {
        return frame;
    }

    /** Shows e.g. "Notifications (3)" in the sidebar; 0 removes the count. */
    public void setNavBadge(String title, int count) {
        NavButton b = navButtons.get(title);
        if (b != null) {
            b.setText(count > 0 ? title + "  (" + count + ")" : title);
        }
    }

    /** Loads the unread-notification count, if this dashboard has a Notifications page. */
    public void refreshNotificationBadge() {
        if (navButtons.containsKey(NotificationsPage.TITLE)) {
            Async.run(this, () -> new NotificationService().unreadCount(session),
                    n -> setNavBadge(NotificationsPage.TITLE, n), error -> { });
        }
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
    }

    /** Called when the user logs out. */
    public void dispose() {
        WaitlistDaemon.removeListener(waitlistListener);
    }

    // ------------------------------------------------------------------ layout pieces

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(UIStyles.SIDEBAR);
        side.setPreferredSize(new Dimension(250, 10));
        side.setBorder(BorderFactory.createEmptyBorder(22, 14, 18, 14));

        JPanel top = UIStyles.clear(null);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel brand = UIStyles.label("UCMS", UIStyles.font(java.awt.Font.BOLD, 24), Color.WHITE);
        JLabel brandSub = UIStyles.label("Course Management", UIStyles.SMALL, new Color(156, 163, 175));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        brandSub.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        top.add(brand);
        top.add(brandSub);
        top.add(Box.createVerticalStrut(22));
        top.add(buildUserCard());
        top.add(Box.createVerticalStrut(20));
        JLabel menu = UIStyles.label("MENU", UIStyles.font(java.awt.Font.BOLD, 11), new Color(107, 114, 128));
        menu.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 0));
        top.add(menu);
        for (Component c : top.getComponents()) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        JPanel navWrap = UIStyles.clear(new BorderLayout());
        navWrap.add(nav, BorderLayout.NORTH);

        FlatButton logout = new FlatButton("Log out", UIStyles.SIDEBAR_HOVER, new Color(55, 65, 81), Color.WHITE);
        logout.addActionListener(e -> frame.logout());

        side.add(top, BorderLayout.NORTH);
        side.add(navWrap, BorderLayout.CENTER);
        side.add(logout, BorderLayout.SOUTH);
        return side;
    }

    private JPanel buildUserCard() {
        Person p = session.getPerson();
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(UIStyles.SIDEBAR_HOVER);
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        card.setMaximumSize(new Dimension(230, 80));
        card.add(new Avatar(p.getName(), 40), BorderLayout.WEST);
        JPanel text = UIStyles.clear(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UIStyles.label(p.getName(), UIStyles.BOLD, Color.WHITE));
        text.add(UIStyles.label(p.getSubtitle(), UIStyles.SMALL, new Color(156, 163, 175)));
        text.add(UIStyles.label(p.getRole().name(), UIStyles.font(java.awt.Font.BOLD, 10), new Color(165, 180, 252)));
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIStyles.BG);
        header.setBorder(BorderFactory.createEmptyBorder(20, 24, 12, 24));
        JPanel left = UIStyles.clear(null);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(pageTitle);
        left.add(pageDescription);
        header.add(left, BorderLayout.WEST);
        header.add(semesterLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIStyles.BORDER),
                BorderFactory.createEmptyBorder(6, 24, 6, 24)));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(UIStyles.muted("Signed in as " + session.getAccount().getEmail()), BorderLayout.EAST);
        return bar;
    }

    private void loadSemester() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"));
        semesterLabel.setText(today);
        Async.run(this, () -> new CourseService().getCurrentSemester(), sem -> {
            if (sem != null) {
                semesterLabel.setText("<html><div style='text-align:right'><b>" + sem.getName() + "</b><br>"
                        + today + "</div></html>");
            }
        }, error -> { });
    }

    /** Sidebar navigation entry with hover and "active" highlight. */
    private static class NavButton extends JButton {
        private boolean active;
        private boolean hover;

        NavButton(String text) {
            super(text);
            setUI(new BasicButtonUI());
            setFont(UIStyles.BODY);
            setForeground(UIStyles.SIDEBAR_TEXT);
            setHorizontalAlignment(LEFT);
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : UIStyles.SIDEBAR_TEXT);
            setFont(active ? UIStyles.BOLD : UIStyles.BODY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active || hover) {
                g.setColor(active ? UIStyles.PRIMARY : UIStyles.SIDEBAR_HOVER);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            super.paintComponent(g);
        }
    }
}
