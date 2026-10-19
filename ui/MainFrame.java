package ui;

import service.Session;
import service.WaitlistDaemon;
import ui.admin.AdminDashboard;
import ui.faculty.FacultyDashboard;
import ui.student.StudentDashboard;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** The single application window. Swaps between login, register and the role's dashboard. */
public class MainFrame extends JFrame {

    private static final String LOGIN = "login";
    private static final String REGISTER = "register";
    private static final String DASHBOARD = "dashboard";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final LoginPanel loginPanel;
    private final RegisterPanel registerPanel;
    private BaseDashboard dashboard;

    public MainFrame() {
        super("UCMS - University Course Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 820);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);
        root.add(loginPanel, LOGIN);
        root.add(registerPanel, REGISTER);
        setContentPane(root);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                WaitlistDaemon.stop();
            }
        });
        showLogin();
    }

    public void showLogin() {
        loginPanel.reset();
        cards.show(root, LOGIN);
        loginPanel.focusFirstField();
    }

    public void showRegister() {
        registerPanel.reset();
        cards.show(root, REGISTER);
    }

    /** Opens the dashboard that matches the logged-in user's role (polymorphism). */
    public void openDashboard(Session session) {
        closeDashboard();
        switch (session.getRole()) {
            case STUDENT:
                dashboard = new StudentDashboard(this, session);
                break;
            case FACULTY:
                dashboard = new FacultyDashboard(this, session);
                break;
            default:
                dashboard = new AdminDashboard(this, session);
        }
        root.add(dashboard, DASHBOARD);
        cards.show(root, DASHBOARD);
        WaitlistDaemon.start();
    }

    public void logout() {
        if (Dialogs.confirm(this, "Log out", "Do you want to log out?")) {
            closeDashboard();
            showLogin();
        }
    }

    private void closeDashboard() {
        if (dashboard != null) {
            dashboard.dispose();
            root.remove(dashboard);
            dashboard = null;
        }
    }
}
