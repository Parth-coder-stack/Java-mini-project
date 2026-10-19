package ui.common;

import model.Student;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Search for a student by PRN, name, email or phone.
 * Faculty/admin see full contact details and can open a student's record.
 */
public class StudentDirectoryPage extends Page {

    private final StudentService studentService = new StudentService();
    private final JTextField searchField = UIStyles.textField(28);
    private final DataTable<Student> table;
    private final boolean staff;
    private final Timer debounce;

    public StudentDirectoryPage(BaseDashboard dashboard) {
        super(dashboard);
        staff = !session.isStudent();

        if (staff) {
            table = new DataTable<>(new String[]{"PRN", "Name", "Department", "Year", "Email", "Phone", "Contact"},
                    s -> new Object[]{s.getPrn(), s.getName(), s.getDeptName(), s.getYearOfStudy(), s.getEmail(),
                            s.getPhone(), s.isContactVerified() ? "Verified" : "Not verified"});
            table.setColumnWidths(110, 160, 220, 50, 220, 110, 90);
            table.onDoubleClick(this::openDetails);
        } else {
            table = new DataTable<>(new String[]{"PRN", "Name", "Department", "Year", "Email"},
                    s -> new Object[]{s.getPrn(), s.getName(), s.getDeptName(), s.getYearOfStudy(), s.getEmail()});
            table.setColumnWidths(120, 200, 260, 60, 260);
        }

        FlatButton searchBtn = UIStyles.primary("Search");
        searchBtn.addActionListener(e -> refresh());
        searchField.addActionListener(e -> refresh());
        debounce = new Timer(350, e -> refresh());
        debounce.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { debounce.restart(); }
            @Override public void removeUpdate(DocumentEvent e)  { debounce.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { debounce.restart(); }
        });

        JPanel bar = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(UIStyles.label("PRN, name, email or phone:", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(searchField);
        bar.add(searchBtn);
        if (staff) {
            FlatButton details = UIStyles.secondary("View full record");
            details.addActionListener(e -> openDetails(table.getSelected()));
            bar.add(details);
        }
        if (session.isAdmin()) {
            FlatButton delete = UIStyles.danger("Delete student");
            delete.addActionListener(e -> deleteSelected());
            bar.add(delete);
        }

        JPanel card = UIStyles.card();
        card.add(bar, BorderLayout.NORTH);
        card.add(table, BorderLayout.CENTER);
        card.add(UIStyles.muted(staff ? "Tip: double-click a student to see their courses, grades and attendance."
                : "Only public directory details are shown to students."), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        String keyword = searchField.getText();
        Async.run(this, () -> studentService.search(keyword), list -> {
            table.setItems(list);
            status(list.size() + " student(s) found" + (keyword.trim().isEmpty() ? "" : " for \"" + keyword.trim() + "\""));
        });
    }

    private void openDetails(Student s) {
        if (s == null) {
            Dialogs.info(this, "Student record", "Select a student first.");
            return;
        }
        new StudentDetailsDialog(dashboard.getFrame(), s).setVisible(true);
    }

    private void deleteSelected() {
        Student s = table.getSelected();
        if (s == null) {
            Dialogs.info(this, "Delete student", "Select a student first.");
            return;
        }
        if (Dialogs.confirm(this, "Delete student", "Permanently delete " + s + " and all their records?")) {
            Async.exec(this, () -> studentService.deleteStudent(session, s.getId()), () -> {
                status("Deleted " + s);
                refresh();
            });
        }
    }
}
