package ui.faculty;

import model.Enrollment;
import model.Section;
import service.CourseService;
import service.EnrollmentService;
import service.GradeScale;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.SectionChooser;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;

/**
 * Faculty enter final grades. Saving a grade marks the enrollment Completed,
 * which is what later satisfies prerequisites, and writes to the audit log.
 */
public class GradesPage extends Page {

    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final SectionChooser sectionBox = new SectionChooser();
    private final JComboBox<String> gradeBox = new JComboBox<>(GradeScale.grades().toArray(new String[0]));
    private final DataTable<Enrollment> roster;
    private boolean filling;

    public GradesPage(BaseDashboard dashboard) {
        super(dashboard);
        roster = new DataTable<Enrollment>(new String[]{"PRN", "Student", "Email", "Status", "Grade"},
                e -> new Object[]{e.getPrn(), e.getStudentName(), e.getStudentEmail(), e.getStatus(),
                        e.getGrade() == null ? "-" : e.getGrade()}).withFilter("Filter students:");
        roster.setColumnWidths(120, 200, 240, 100, 70);
        roster.onSelect(e -> {
            if (e != null && e.getGrade() != null) {
                gradeBox.setSelectedItem(e.getGrade());
            }
        });
        sectionBox.addActionListener(e -> {
            if (!filling) {
                loadRoster();
            }
        });

        FlatButton save = UIStyles.success("Save grade for selected student");
        save.addActionListener(e -> saveGrade());

        JPanel bar = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(UIStyles.label("Section", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(sectionBox);
        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(UIStyles.muted("Grade scale: A+ 10, A 9, B+ 8, B 7, C+ 6, C 5, D 4, F 0"));
        actions.add(gradeBox);
        actions.add(save);

        JPanel card = UIStyles.card();
        card.add(bar, BorderLayout.NORTH);
        card.add(roster, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> courseService.getSectionsForFaculty(session.getFaculty().getId()), list -> {
            filling = true;
            sectionBox.setSections(list);
            filling = false;
            loadRoster();
        });
    }

    private void loadRoster() {
        Section s = sectionBox.getSelectedSection();
        if (s == null) {
            roster.setItems(Collections.emptyList());
            return;
        }
        Async.run(this, () -> enrollmentService.getRoster(s.getSectionId()), list -> {
            roster.setItems(list);
            status(list.size() + " student(s) in " + s.getKey());
        });
    }

    private void saveGrade() {
        Enrollment e = roster.getSelected();
        String grade = (String) gradeBox.getSelectedItem();
        if (e == null) {
            Dialogs.info(this, "Grade", "Select a student first.");
            return;
        }
        String msg = "Give " + e.getStudentName() + " grade " + grade + " in " + e.getSectionKey() + "?\n"
                + "This marks the course as Completed for the student.";
        if (Dialogs.confirm(this, "Save grade", msg)) {
            Async.exec(this, () -> enrollmentService.saveGrade(session, e, grade), () -> {
                status("Saved grade " + grade + " for " + e.getStudentName());
                loadRoster();
            });
        }
    }
}
