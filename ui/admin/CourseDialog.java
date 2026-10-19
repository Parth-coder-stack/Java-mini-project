package ui.admin;

import model.Course;
import model.Department;
import service.CourseService;
import service.Session;
import ui.Async;
import ui.UIStyles;
import ui.components.FlatButton;
import ui.components.FormBuilder;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** Add a new course (with credits) or edit an existing one. */
public class CourseDialog extends JDialog {

    private final CourseService courseService = new CourseService();
    private final JTextField code = UIStyles.textField(10);
    private final JTextField title = UIStyles.textField(28);
    private final JTextField description = UIStyles.textField(28);
    private final JComboBox<Department> dept = new JComboBox<>();
    private final JTextField theory = UIStyles.textField(5);
    private final JTextField lab = UIStyles.textField(5);
    private final JLabel error = UIStyles.label(" ", UIStyles.BOLD, UIStyles.DANGER);
    private boolean saved;

    /** @param existing null to add a new course */
    public CourseDialog(JFrame owner, Session session, Course existing, List<Department> departments) {
        super(owner, existing == null ? "Add course" : "Edit " + existing.getCourseCode(), true);
        for (Department d : departments) {
            dept.addItem(d);
            if (existing != null && d.getDeptId() == existing.getDeptId()) {
                dept.setSelectedItem(d);
            }
        }
        theory.setText("3");
        lab.setText("0");
        if (existing != null) {
            code.setText(existing.getCourseCode());
            title.setText(existing.getTitle());
            description.setText(existing.getDescription());
        }

        FormBuilder fb = new FormBuilder()
                .row(FormBuilder.labeled("Course code (e.g. CS601)", code), FormBuilder.labeled("Department", dept))
                .field("Title", title)
                .field("Description", description);
        if (existing == null) {
            fb.row(FormBuilder.labeled("Theory credits", theory), FormBuilder.labeled("Lab credits (0 = no lab)", lab));
        }
        fb.row(error);

        FlatButton save = UIStyles.primary("Save");
        FlatButton cancel = UIStyles.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            Department d = (Department) dept.getSelectedItem();
            int deptId = d == null ? 0 : d.getDeptId();
            Async.exec(this, () -> {
                if (existing == null) {
                    courseService.createCourse(session, code.getText(), title.getText(), description.getText(),
                            deptId, theory.getText(), lab.getText());
                } else {
                    courseService.updateCourse(session, existing.getCourseId(), code.getText(), title.getText(),
                            description.getText(), deptId);
                }
            }, () -> {
                saved = true;
                dispose();
            });
        });
        JPanel buttons = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(save);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(UIStyles.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        content.add(fb.build(), BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean showDialog() {
        setVisible(true);
        return saved;
    }
}
