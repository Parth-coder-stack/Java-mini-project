package ui;

import exception.AuthException;
import exception.ValidationException;
import service.AuthService;
import service.ConnectionService;
import ui.components.BrandPanel;
import ui.components.FlatButton;
import ui.components.FormBuilder;
import ui.components.StatusIcon;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/** Sign-in screen: email or PRN + password, with a live database status indicator. */
public class LoginPanel extends JPanel {

    private final MainFrame frame;
    private final AuthService authService = new AuthService();
    private final ConnectionService connectionService = new ConnectionService();

    private final JTextField idField = UIStyles.textField(22);
    private final JPasswordField passwordField = UIStyles.passwordField(22);
    private final JLabel errorLabel = UIStyles.label(" ", UIStyles.BOLD, UIStyles.DANGER);
    private final JLabel dbStatus = UIStyles.muted("Checking database connection...");
    private final FlatButton loginButton = UIStyles.primary("Sign in");

    public LoginPanel(MainFrame frame) {
        super(new BorderLayout());
        this.frame = frame;
        setBackground(UIStyles.BG);

        add(new BrandPanel("Everything for your semester in one place",
                "Browse courses and their prerequisites",
                "Register for sections with automatic eligibility checks",
                "Track attendance, faculty and your weekly timetable",
                "Search the student directory"), BorderLayout.WEST);

        JPanel center = UIStyles.clear(new GridBagLayout());
        center.add(buildForm());
        add(center, BorderLayout.CENTER);

        loginButton.addActionListener(e -> doLogin());
        idField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> doLogin());
    }

    private JPanel buildForm() {
        JPanel card = UIStyles.card();
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(14, 14, 6, 14)));
        card.setPreferredSize(new Dimension(440, 600));

        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        show.setFont(UIStyles.SMALL);
        char echo = passwordField.getEchoChar();
        show.addActionListener(e -> passwordField.setEchoChar(show.isSelected() ? (char) 0 : echo));

        FlatButton registerLink = UIStyles.link("Create a student account");
        registerLink.addActionListener(e -> frame.showRegister());
        JPanel registerRow = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        registerRow.add(UIStyles.muted("New student?  "));
        registerRow.add(registerLink);

        FlatButton settings = UIStyles.link("Database settings");
        settings.setFont(UIStyles.SMALL);
        settings.addActionListener(e -> {
            if (new DBSettingsDialog(frame).showDialog()) {
                checkDatabase();
            }
        });
        JPanel dbRow = UIStyles.clear(new BorderLayout(6, 0));
        dbRow.add(dbStatus, BorderLayout.CENTER);
        dbRow.add(settings, BorderLayout.EAST);

        JLabel demo = UIStyles.muted("<html>Demo logins &nbsp;(password)<br>"
                + "Student: parth.vohra@student.edu or 25070122259 &nbsp;(stud123)<br>"
                + "Faculty: priya.sharma@university.edu &nbsp;(fac123)<br>"
                + "Admin: admin@ucms.edu &nbsp;(admin123)</html>");

        JPanel form = new FormBuilder()
                .row(UIStyles.label("Welcome back", UIStyles.H1, UIStyles.TEXT))
                .row(UIStyles.muted("Sign in with your email address or PRN"))
                .gap(8)
                .field("Email or PRN", idField)
                .field("Password", passwordField)
                .row(show)
                .row(errorLabel)
                .row(loginButton)
                .row(registerRow)
                .gap(4)
                .row(new JSeparator())
                .row(dbRow)
                .row(demo)
                .build();
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private void doLogin() {
        errorLabel.setText(" ");
        loginButton.setEnabled(false);
        String id = idField.getText();
        String password = new String(passwordField.getPassword());
        Async.run(this, () -> authService.login(id, password),
                session -> {
                    loginButton.setEnabled(true);
                    frame.openDashboard(session);
                },
                error -> {
                    loginButton.setEnabled(true);
                    if (error instanceof AuthException || error instanceof ValidationException) {
                        errorLabel.setText(error.getMessage());
                    } else {
                        Dialogs.error(this, error);
                    }
                });
    }

    /** Pings MySQL in the background and shows a green/red indicator. */
    private void checkDatabase() {
        dbStatus.setIcon(null);
        dbStatus.setText("Checking database connection...");
        Async.run(this, connectionService::test, error -> {
            if (error == null) {
                dbStatus.setIcon(StatusIcon.OK);
                dbStatus.setText("Connected: " + connectionService.describe());
                dbStatus.setToolTipText(null);
            } else {
                dbStatus.setIcon(StatusIcon.FAIL);
                dbStatus.setText("Database offline - click Database settings");
                dbStatus.setToolTipText("<html>" + error.replace("\n", "<br>") + "</html>");
            }
        });
    }

    public void reset() {
        passwordField.setText("");
        errorLabel.setText(" ");
        loginButton.setEnabled(true);
        checkDatabase();
    }

    public void focusFirstField() {
        idField.requestFocusInWindow();
    }
}
