package ui.student;

import model.Student;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.Avatar;
import ui.components.FlatButton;
import ui.components.FormBuilder;
import ui.components.StatusIcon;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Student profile; phone and address can be edited and confirmed (needed to enroll). */
public class ProfilePage extends Page {

    private final StudentService studentService = new StudentService();
    private final JLabel name = UIStyles.label(" ", UIStyles.H2, UIStyles.TEXT);
    private final JLabel prn = UIStyles.muted(" ");
    private final JLabel email = UIStyles.label(" ", UIStyles.BOLD, UIStyles.TEXT);
    private final JLabel dept = UIStyles.label(" ", UIStyles.BOLD, UIStyles.TEXT);
    private final JLabel year = UIStyles.label(" ", UIStyles.BOLD, UIStyles.TEXT);
    private final JLabel verified = UIStyles.label(" ", UIStyles.BOLD, UIStyles.TEXT);
    private final JTextField phone = UIStyles.textField(16);
    private final JTextField address = UIStyles.textField(30);

    public ProfilePage(BaseDashboard dashboard) {
        super(dashboard);
        JPanel head = UIStyles.clear(new BorderLayout(16, 0));
        head.add(new Avatar(session.getPerson().getName(), 64), BorderLayout.WEST);
        JPanel names = UIStyles.clear(null);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(name);
        names.add(prn);
        names.add(verified);
        head.add(names, BorderLayout.CENTER);

        FlatButton save = UIStyles.primary("Save and confirm contact details");
        save.addActionListener(e -> save());
        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.add(save);

        JPanel form = new FormBuilder()
                .row(head)
                .gap(8)
                .row(FormBuilder.labeled("Email (login ID)", email), FormBuilder.labeled("Department", dept),
                        FormBuilder.labeled("Year of study", year))
                .gap(4)
                .row(UIStyles.heading("Contact details"))
                .row(UIStyles.muted("Keep these up to date - they are checked before every course registration."))
                .row(FormBuilder.labeled("Phone (10 digits)", phone), FormBuilder.labeled("Address", address))
                .row(actions)
                .build();
        JPanel card = UIStyles.card();
        card.add(form, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(820, 420));
        JPanel holder = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.add(card);
        add(holder, BorderLayout.NORTH);
    }

    private void show(Student s) {
        name.setText(s.getName());
        prn.setText("PRN " + s.getPrn());
        email.setText(s.getEmail());
        dept.setText(s.getDeptName());
        year.setText(String.valueOf(s.getYearOfStudy()));
        phone.setText(s.getPhone());
        address.setText(s.getAddress());
        if (s.isContactVerified()) {
            verified.setIcon(StatusIcon.OK);
            verified.setForeground(UIStyles.SUCCESS);
            verified.setText("Contact details confirmed on " + s.getContactVerifiedAt().toLocalDate());
        } else {
            verified.setIcon(StatusIcon.WARN);
            verified.setForeground(UIStyles.WARNING);
            verified.setText("Contact details not confirmed yet");
        }
    }

    private void save() {
        int id = session.getStudent().getId();
        Async.run(this, () -> studentService.updateContact(session, id, phone.getText(), address.getText()), s -> {
            show(s);
            Dialogs.info(this, "Saved", "Your contact details have been saved and confirmed.");
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> studentService.getStudent(session.getStudent().getId()), s -> {
            session.refresh(s);
            show(s);
        });
    }
}
