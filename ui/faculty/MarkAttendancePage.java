package ui.faculty;

import model.AttendanceSummary;
import model.Enrollment;
import model.Section;
import service.AttendanceService;
import service.CourseService;
import service.EnrollmentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.SectionChooser;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Faculty marks Present / Absent / Late for a whole section on a date. */
public class MarkAttendancePage extends Page {

    private static final String[] STATUSES = {"Present", "Absent", "Late"};

    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final AttendanceService attendanceService = new AttendanceService();

    private final SectionChooser sectionBox = new SectionChooser();
    private final JTextField dateField = UIStyles.textField(10);
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"PRN", "Student", "Status", "Attendance so far"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2;
        }
    };
    private final JTable table = new JTable(model);
    private final List<Enrollment> roster = new ArrayList<>();
    private boolean filling;

    public MarkAttendancePage(BaseDashboard dashboard) {
        super(dashboard);
        DataTable.style(table);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(STATUSES)));
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        dateField.setText(LocalDate.now().toString());

        FlatButton prev = UIStyles.secondary("<");
        FlatButton next = UIStyles.secondary(">");
        FlatButton load = UIStyles.secondary("Load");
        FlatButton allPresent = UIStyles.secondary("Mark all present");
        FlatButton save = UIStyles.success("Save attendance");
        prev.addActionListener(e -> shiftDate(-1));
        next.addActionListener(e -> shiftDate(1));
        load.addActionListener(e -> loadRoster());
        dateField.addActionListener(e -> loadRoster());
        sectionBox.addActionListener(e -> {
            if (!filling) {
                loadRoster();
            }
        });
        allPresent.addActionListener(e -> {
            for (int r = 0; r < model.getRowCount(); r++) {
                model.setValueAt("Present", r, 2);
            }
        });
        save.addActionListener(e -> save());

        JPanel bar = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(UIStyles.label("Section", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(sectionBox);
        bar.add(UIStyles.label("Date (yyyy-mm-dd)", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(prev);
        bar.add(dateField);
        bar.add(next);
        bar.add(load);

        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.add(UIStyles.muted("Click a Status cell to change it."));
        actions.add(allPresent);
        actions.add(save);

        JPanel card = UIStyles.card();
        card.add(bar, BorderLayout.NORTH);
        card.add(UIStyles.scroll(table), BorderLayout.CENTER);
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

    private LocalDate date() {
        try {
            return LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void shiftDate(int days) {
        LocalDate d = date();
        dateField.setText((d == null ? LocalDate.now() : d.plusDays(days)).toString());
        loadRoster();
    }

    private void loadRoster() {
        Section section = sectionBox.getSelectedSection();
        LocalDate date = date();
        if (section == null) {
            model.setRowCount(0);
            return;
        }
        if (date == null) {
            Dialogs.warn(this, "Date", "Enter the date as yyyy-mm-dd, e.g. " + LocalDate.now());
            return;
        }
        Async.run(this, () -> {
            RosterData d = new RosterData();
            for (Enrollment e : enrollmentService.getRoster(section.getSectionId())) {
                if (e.isActive()) {
                    d.students.add(e);
                }
            }
            d.marked = attendanceService.getStatuses(section.getSectionId(), date);
            d.totals = attendanceService.getSectionSummaries(section.getSectionId());
            return d;
        }, d -> {
            roster.clear();
            roster.addAll(d.students);
            model.setRowCount(0);
            for (Enrollment e : d.students) {
                AttendanceSummary s = d.totals.get(e.getEnrollmentId());
                model.addRow(new Object[]{e.getPrn(), e.getStudentName(),
                        d.marked.getOrDefault(e.getEnrollmentId(), "Present"),
                        s == null || s.getTotal() == 0 ? "No classes yet"
                                : String.format("%.1f%% (%d classes)", s.getPercent(), s.getTotal())});
            }
            status(d.students.size() + " student(s) in " + section.getKey()
                    + (d.marked.isEmpty() ? "" : " - attendance already saved for " + date + " (you can edit it)"));
        });
    }

    /** Roster plus attendance already recorded, loaded together in the background. */
    private static class RosterData {
        final List<Enrollment> students = new ArrayList<>();
        Map<Integer, String> marked;
        Map<Integer, AttendanceSummary> totals;
    }

    private void save() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        Section section = sectionBox.getSelectedSection();
        LocalDate date = date();
        if (section == null || date == null) {
            Dialogs.warn(this, "Attendance", "Choose a section and a valid date first.");
            return;
        }
        Map<Integer, String> statuses = new LinkedHashMap<>();
        for (int r = 0; r < model.getRowCount() && r < roster.size(); r++) {
            statuses.put(roster.get(r).getEnrollmentId(), (String) model.getValueAt(r, 2));
        }
        Async.exec(this, () -> attendanceService.saveAttendance(session, section.getSectionId(), date, statuses), () -> {
            Dialogs.info(this, "Saved", "Attendance saved for " + statuses.size() + " student(s) on " + date + ".");
            loadRoster();
        });
    }
}
