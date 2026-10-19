package ui.faculty;

import model.AttendanceSummary;
import model.Enrollment;
import model.Section;
import model.WaitlistEntry;
import service.AttendanceService;
import service.CourseService;
import service.EnrollmentService;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.common.StudentDetailsDialog;
import ui.components.DataTable;
import ui.components.SectionChooser;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Faculty: each assigned section with its class roster, attendance so far and waitlist. */
public class RosterPage extends Page {

    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final AttendanceService attendanceService = new AttendanceService();
    private final StudentService studentService = new StudentService();
    private final SectionChooser sectionBox = new SectionChooser();
    private final Map<Integer, AttendanceSummary> attendance = new HashMap<>();
    private final DataTable<Enrollment> roster;
    private final DataTable<WaitlistEntry> waitlist;
    private final JLabel info = UIStyles.muted(" ");
    private boolean filling;

    public RosterPage(BaseDashboard dashboard) {
        super(dashboard);
        roster = new DataTable<Enrollment>(new String[]{"PRN", "Student", "Email", "Status", "Grade", "Attendance"},
                e -> {
                    AttendanceSummary a = attendance.get(e.getEnrollmentId());
                    String att = a == null || a.getTotal() == 0 ? "No classes yet"
                            : String.format("%.1f%%", a.getPercent()) + (a.getPercent() < AttendanceService.MIN_PERCENT ? " (low)" : "");
                    return new Object[]{e.getPrn(), e.getStudentName(), e.getStudentEmail(), e.getStatus(),
                            e.getGrade() == null ? "-" : e.getGrade(), att};
                }).withFilter("Filter students:");
        roster.setColumnWidths(110, 180, 240, 90, 60, 110);
        roster.onDoubleClick(e -> Async.run(this, () -> studentService.getStudent(e.getStudentId()),
                st -> new StudentDetailsDialog(dashboard.getFrame(), st).setVisible(true)));
        waitlist = new DataTable<>(new String[]{"Position", "Student", "Waiting since"},
                w -> new Object[]{w.getPosition(), w.getStudentName(), w.getWaitlistDate()});
        sectionBox.addActionListener(e -> {
            if (!filling) {
                loadSection();
            }
        });

        JPanel bar = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(UIStyles.label("Section", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(sectionBox);
        bar.add(info);

        JPanel top = UIStyles.card();
        top.add(bar, BorderLayout.NORTH);
        top.add(roster, BorderLayout.CENTER);
        top.add(UIStyles.muted("Double-click a student to see their full record."), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, UIStyles.card("Waitlist", waitlist));
        split.setResizeWeight(0.75);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> courseService.getSectionsForFaculty(session.getFaculty().getId()), list -> {
            filling = true;
            sectionBox.setSections(list);
            filling = false;
            loadSection();
        });
    }

    private void loadSection() {
        Section s = sectionBox.getSelectedSection();
        if (s == null) {
            roster.setItems(Collections.emptyList());
            waitlist.setItems(Collections.emptyList());
            return;
        }
        info.setText(s.getRoomNumber() + "  -  " + s.getEnrolledCount() + " / " + s.getMaxCapacity() + " seats");
        Async.run(this, () -> {
            Map<Integer, AttendanceSummary> a = attendanceService.getSectionSummaries(s.getSectionId());
            attendance.clear();
            attendance.putAll(a);
            return enrollmentService.getRoster(s.getSectionId());
        }, list -> {
            roster.setItems(list);
            status(list.size() + " student(s) in " + s.getKey());
        });
        Async.run(this, () -> enrollmentService.getSectionWaitlist(s.getSectionId()), waitlist::setItems);
    }
}
