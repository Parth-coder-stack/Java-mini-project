package ui.admin;

import model.Department;
import model.Faculty;
import model.TeachingLoad;
import service.FacultyService;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.FormBuilder;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.List;

/** Admin: add / edit / delete faculty and see each member's teaching load. */
public class FacultyPage extends Page {

    private final FacultyService facultyService = new FacultyService();
    private final StudentService studentService = new StudentService();
    private final DataTable<Faculty> table;
    private final DataTable<TeachingLoad> load;

    public FacultyPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<Faculty>(new String[]{"Name", "Designation", "Department", "Email", "Phone"},
                f -> new Object[]{f.getName(), f.getDesignation(), f.getDeptName(), f.getEmail(), f.getPhone()})
                .withFilter("Filter:");
        table.setColumnWidths(180, 160, 220, 240, 110);
        load = new DataTable<>(new String[]{"Semester", "Sections", "Students", "Hours / week", "Credits"},
                t -> new Object[]{t.getSemesterName(), t.getSections(), t.getStudents(),
                        String.format("%.1f", t.getWeeklyHours()), t.getCredits()});
        table.onSelect(f -> {
            if (f == null) {
                load.setItems(Collections.emptyList());
            } else {
                Async.run(this, () -> facultyService.getTeachingLoad(f), load::setItems);
            }
        });

        FlatButton add = UIStyles.primary("Add faculty");
        FlatButton edit = UIStyles.secondary("Edit");
        FlatButton delete = UIStyles.danger("Delete");
        add.addActionListener(e -> openDialog(null));
        edit.addActionListener(e -> {
            Faculty f = table.getSelected();
            if (f == null) {
                Dialogs.info(this, "Faculty", "Select a faculty member first.");
            } else {
                openDialog(f);
            }
        });
        delete.addActionListener(e -> deleteSelected());
        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        JPanel top = UIStyles.card("Faculty", table);
        top.add(actions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top,
                UIStyles.card("Teaching load of selected faculty (all semesters)", load));
        split.setResizeWeight(0.6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, facultyService::getAll, list -> {
            table.setItems(list);
            status(list.size() + " faculty members");
        });
    }

    private void openDialog(Faculty existing) {
        Async.run(this, studentService::getDepartments, (List<Department> depts) -> {
            JTextField name = UIStyles.textField(20);
            JTextField email = UIStyles.textField(20);
            JTextField phone = UIStyles.textField(12);
            JTextField designation = UIStyles.textField(18);
            JPasswordField password = UIStyles.passwordField(12);
            JComboBox<Department> dept = new JComboBox<>(depts.toArray(new Department[0]));
            FormBuilder fb = new FormBuilder()
                    .row(FormBuilder.labeled("Full name", name), FormBuilder.labeled("Department", dept))
                    .row(FormBuilder.labeled("Email (login ID)", email), FormBuilder.labeled("Phone (10 digits)", phone))
                    .field("Designation (e.g. Assistant Professor)", designation);
            if (existing == null) {
                fb.field("Initial password (letters + digits, min 6)", password);
            } else {
                name.setText(existing.getName());
                email.setText(existing.getEmail());
                email.setEnabled(false);
                phone.setText(existing.getPhone());
                designation.setText(existing.getDesignation());
                for (Department d : depts) {
                    if (d.getDeptId() == existing.getDeptId()) {
                        dept.setSelectedItem(d);
                    }
                }
            }
            FormDialog dialog = new FormDialog(dashboard.getFrame(), existing == null ? "Add faculty" : "Edit faculty",
                    fb.build(), () -> {
                Department d = (Department) dept.getSelectedItem();
                int deptId = d == null ? 0 : d.getDeptId();
                if (existing == null) {
                    facultyService.create(session, name.getText(), email.getText(), phone.getText(),
                            designation.getText(), deptId, new String(password.getPassword()));
                } else {
                    facultyService.update(session, existing, name.getText(), phone.getText(), designation.getText(), deptId);
                }
            });
            if (dialog.showDialog()) {
                status(existing == null ? "Faculty added - they can log in with their email" : "Faculty updated");
                refresh();
            }
        });
    }

    private void deleteSelected() {
        Faculty f = table.getSelected();
        if (f == null) {
            Dialogs.info(this, "Faculty", "Select a faculty member first.");
            return;
        }
        if (Dialogs.confirm(this, "Delete faculty", "Delete " + f.getName() + " and their login?")) {
            Async.exec(this, () -> facultyService.delete(session, f), () -> {
                status("Deleted " + f.getName());
                refresh();
            });
        }
    }
}
