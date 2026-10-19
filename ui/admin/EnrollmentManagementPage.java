package ui.admin;

import model.Enrollment;
import model.Section;
import model.Student;
import service.CourseService;
import service.EnrollmentService;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.SectionChooser;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Admin: view/drop current enrollments and register a student (optionally overriding prerequisites). */
public class EnrollmentManagementPage extends Page {

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final StudentService studentService = new StudentService();
    private final CourseService courseService = new CourseService();
    private final JTextField search = UIStyles.textField(20);
    private final DataTable<Enrollment> table;
    private final JTextField prnField = UIStyles.textField(12);
    private final SectionChooser sectionBox = new SectionChooser();
    private final JCheckBox override = new JCheckBox("Override prerequisites / tasks / contact");
    private final JCheckBox ignoreCapacity = new JCheckBox("Ignore seat capacity");

    public EnrollmentManagementPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<>(new String[]{"PRN", "Student", "Course", "Type", "Sec", "Slot", "Faculty", "Status",
                "Override"},
                e -> new Object[]{e.getPrn(), e.getStudentName(), e.getCourseCode(), e.getComponentType(),
                        e.getSectionNumber(), e.getSlot(), e.getFacultyName(), e.getStatus(),
                        e.isOverridden() ? "Yes" : ""});
        table.setColumnWidths(110, 160, 70, 60, 40, 150, 140, 90, 60);
        override.setOpaque(false);
        ignoreCapacity.setOpaque(false);
        ignoreCapacity.setToolTipText("Exceptional case: enroll even if the section is full");

        FlatButton find = UIStyles.primary("Search");
        find.addActionListener(e -> refresh());
        search.addActionListener(e -> refresh());
        FlatButton drop = UIStyles.danger("Drop selected");
        drop.addActionListener(e -> dropSelected());
        JPanel bar = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(UIStyles.label("PRN, name or course:", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(search);
        bar.add(find);
        bar.add(drop);

        FlatButton enroll = UIStyles.success("Register student");
        enroll.addActionListener(e -> enrollStudent());
        JPanel row1 = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.add(UIStyles.label("Register PRN", UIStyles.BOLD, UIStyles.TEXT));
        row1.add(prnField);
        row1.add(UIStyles.label("in", UIStyles.BOLD, UIStyles.TEXT));
        row1.add(sectionBox);
        JPanel row2 = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.add(override);
        row2.add(ignoreCapacity);
        row2.add(enroll);
        JPanel enrollBar = UIStyles.clear(new java.awt.GridLayout(2, 1, 0, 6));
        enrollBar.add(row1);
        enrollBar.add(row2);

        JPanel card = UIStyles.card();
        card.add(bar, BorderLayout.NORTH);
        card.add(table, BorderLayout.CENTER);
        card.add(enrollBar, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String keyword = search.getText();
        Async.run(this, () -> enrollmentService.searchCurrentEnrollments(keyword), list -> {
            table.setItems(list);
            status(list.size() + " enrollment(s) this semester");
        });
        Async.run(this, courseService::getCurrentSections, sectionBox::setSections);
    }

    private void dropSelected() {
        Enrollment e = table.getSelected();
        if (e == null || !e.isActive()) {
            Dialogs.info(this, "Drop", "Select an active (Enrolled) row first.");
            return;
        }
        if (Dialogs.confirm(this, "Drop", "Drop " + e.getStudentName() + " from " + e.getSectionKey() + "?")) {
            Async.run(this, () -> enrollmentService.drop(session, e.getEnrollmentId()), msg -> {
                Dialogs.info(this, "Dropped", msg);
                refresh();
            });
        }
    }

    private void enrollStudent() {
        Section s = sectionBox.getSelectedSection();
        String prn = prnField.getText().trim();
        if (s == null || prn.isEmpty()) {
            Dialogs.info(this, "Register", "Enter a PRN and choose a section.");
            return;
        }
        boolean force = override.isSelected();
        boolean overCapacity = ignoreCapacity.isSelected();
        Async.run(this, () -> {
            Student st = null;
            for (Student candidate : studentService.search(prn)) {
                if (candidate.getPrn().equals(prn)) {
                    st = candidate;
                }
            }
            if (st == null) {
                throw new exception.ValidationException("No student with PRN " + prn);
            }
            return enrollmentService.enroll(session, st.getId(), s.getSectionId(), force, overCapacity);
        }, result -> {
            Dialogs.info(this, result.isEnrolled() ? "Registered" : "Waitlisted", result.getMessage());
            refresh();
        });
    }
}
