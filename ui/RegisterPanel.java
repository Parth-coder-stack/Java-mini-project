package ui;

import exception.ValidationException;
import model.Department;
import service.AuthService;
import service.StudentService;
import service.Validator;
import ui.components.BrandPanel;
import ui.components.FlatButton;
import ui.components.FormBuilder;
import ui.components.InlineValidator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/** Student self-registration with PRN, email, phone, address and password. */
public class RegisterPanel extends JPanel {

    private final MainFrame frame;
    private final AuthService authService = new AuthService();
    private final StudentService studentService = new StudentService();

    private final JTextField nameField = UIStyles.textField(16);
    private final JTextField prnField = UIStyles.textField(16);
    private final JTextField emailField = UIStyles.textField(16);
    private final JTextField phoneField = UIStyles.textField(16);
    private final JTextField addressField = UIStyles.textField(30);
    private final JComboBox<Department> deptBox = new JComboBox<>();
    private final JComboBox<Integer> yearBox = new JComboBox<>(new Integer[]{1, 2, 3, 4});
    private final JPasswordField passwordField = UIStyles.passwordField(16);
    private final JPasswordField confirmField = UIStyles.passwordField(16);
    private final JCheckBox confirmContact = new JCheckBox("I confirm that my contact details above are correct");
    private final JLabel errorLabel = UIStyles.label(" ", UIStyles.BOLD, UIStyles.DANGER);
    private final FlatButton createButton = UIStyles.primary("Create account");
    private final InlineValidator inline = new InlineValidator();

    public RegisterPanel(MainFrame frame) {
        super(new BorderLayout());
        this.frame = frame;
        setBackground(UIStyles.BG);

        add(new BrandPanel("How registration works",
                "Create your account with your PRN",
                "Pick a course - prerequisites are checked for you",
                "Finish its prerequisite tasks - assignments or videos",
                "Confirm your contact details and choose a section"), BorderLayout.WEST);

        JPanel center = UIStyles.clear(new GridBagLayout());
        center.add(buildForm());
        center.setOpaque(true);
        center.setBackground(UIStyles.BG);
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(center);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        createButton.addActionListener(e -> doRegister());
    }

    private JPanel buildForm() {
        JPanel card = UIStyles.card();
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(), BorderFactory.createEmptyBorder(10, 10, 4, 10)));
        card.setPreferredSize(new Dimension(640, 760));

        confirmContact.setOpaque(false);
        prnField.setToolTipText("11-digit Permanent Registration Number");
        phoneField.setToolTipText("10-digit mobile number");

        FlatButton back = UIStyles.link("Sign in");
        back.addActionListener(e -> frame.showLogin());
        JPanel backRow = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.add(UIStyles.muted("Already have an account?  "));
        backRow.add(back);

        JPanel form = new FormBuilder()
                .row(UIStyles.label("Create your student account", UIStyles.H1, UIStyles.TEXT))
                .row(UIStyles.muted("All fields are required. Your email will be your login ID."))
                .gap(4)
                .row(inline.field("Full name", nameField, v -> Validator.required(v, "Full name")),
                        inline.field("PRN (11 digits)", prnField, Validator::prn))
                .row(inline.field("Email", emailField, Validator::email),
                        inline.field("Phone (10 digits)", phoneField, Validator::phone))
                .row(FormBuilder.labeled("Department", deptBox), FormBuilder.labeled("Year of study", yearBox))
                .row(inline.field("Address", addressField, Validator::address))
                .row(inline.field("Password (6+ chars, a letter and a digit)", passwordField,
                                v -> Validator.password(v, v)),
                        inline.field("Confirm password", confirmField,
                                v -> Validator.password(new String(passwordField.getPassword()), v)))
                .row(confirmContact)
                .row(errorLabel)
                .row(createButton)
                .row(backRow)
                .build();
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private void loadDepartments() {
        Async.run(this, studentService::getDepartments, depts -> {
            deptBox.removeAllItems();
            for (Department d : depts) {
                deptBox.addItem(d);
            }
        }, error -> errorLabel.setText("Could not load departments - is the database running?"));
    }

    private void doRegister() {
        errorLabel.setText(" ");
        if (!inline.validateAll()) {
            errorLabel.setText("Please fix the fields marked in red.");
            return;
        }
        if (!confirmContact.isSelected()) {
            errorLabel.setText("Please confirm that your contact details are correct.");
            return;
        }
        Department dept = (Department) deptBox.getSelectedItem();
        int deptId = dept == null ? 0 : dept.getDeptId();
        int year = (Integer) yearBox.getSelectedItem();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());
        createButton.setEnabled(false);
        Async.run(this, () -> authService.registerStudent(nameField.getText(), prnField.getText(),
                        emailField.getText(), phoneField.getText(), addressField.getText(),
                        deptId, year, password, confirm),
                session -> {
                    createButton.setEnabled(true);
                    Dialogs.info(frame, "Welcome!", "Your account has been created, "
                            + session.getPerson().getName() + ".\nYou are now signed in.");
                    frame.openDashboard(session);
                },
                error -> {
                    createButton.setEnabled(true);
                    if (error instanceof ValidationException) {
                        errorLabel.setText(error.getMessage());
                    } else {
                        Dialogs.error(this, error);
                    }
                });
    }

    public void reset() {
        for (JTextField f : new JTextField[]{nameField, prnField, emailField, phoneField, addressField,
                passwordField, confirmField}) {
            f.setText("");
        }
        confirmContact.setSelected(false);
        errorLabel.setText(" ");
        inline.reset();
        loadDepartments();
    }
}
