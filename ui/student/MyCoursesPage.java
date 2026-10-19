package ui.student;

import model.Course;
import model.Enrollment;
import model.WaitlistEntry;
import service.AcademicService;
import service.CourseService;
import service.EnrollmentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Current registrations (with prerequisites), completed courses with grades, and waitlist. */
public class MyCoursesPage extends Page {

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final CourseService courseService = new CourseService();
    private final AcademicService academicService = new AcademicService();
    private final Map<Integer, String> prereqText = new HashMap<>();

    private final DataTable<Enrollment> current;
    private final DataTable<Enrollment> completed;
    private final DataTable<WaitlistEntry> waitlist;
    private final JLabel creditsLabel = UIStyles.heading(" ");
    private final JLabel cgpaLabel = UIStyles.heading(" ");

    public MyCoursesPage(BaseDashboard dashboard) {
        super(dashboard);
        current = new DataTable<>(new String[]{"Course", "Title", "Type", "Sec", "Slot", "Faculty", "Room",
                "Credits", "Prerequisites"},
                e -> new Object[]{e.getCourseCode(), e.getCourseTitle(), e.getComponentType(), e.getSectionNumber(),
                        e.getSlot(), e.getFacultyName(), e.getRoomNumber(), e.getCredits(),
                        prereqText.getOrDefault(e.getCourseId(), "None")});
        current.setColumnWidths(70, 190, 60, 40, 150, 140, 60, 60, 200);
        completed = new DataTable<>(new String[]{"Semester", "Course", "Title", "Type", "Credits", "Status", "Grade"},
                e -> new Object[]{e.getSemesterName(), e.getCourseCode(), e.getCourseTitle(), e.getComponentType(),
                        e.getCredits(), e.getStatus(), e.getGrade() == null ? "-" : e.getGrade()});
        waitlist = new DataTable<>(new String[]{"Section", "Slot", "Queue position", "Waiting since"},
                w -> new Object[]{w.getSectionKey(), w.getSlot(), w.getPosition(), w.getWaitlistDate()});

        FlatButton drop = UIStyles.danger("Drop selected");
        drop.addActionListener(e -> dropSelected());
        FlatButton leave = UIStyles.secondary("Leave waitlist");
        leave.addActionListener(e -> leaveSelected());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Current semester", tab(current, creditsLabel, drop));
        tabs.addTab("History & grades", tab(completed, cgpaLabel, null));
        tabs.addTab("Waitlist", tab(waitlist, UIStyles.muted("You are enrolled automatically when a seat frees up."), leave));
        add(tabs, BorderLayout.CENTER);
    }

    private static JPanel tab(JPanel table, JLabel caption, FlatButton action) {
        JPanel p = UIStyles.card();
        JPanel bar = UIStyles.clear(new BorderLayout());
        bar.add(caption, BorderLayout.WEST);
        if (action != null) {
            JPanel right = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.add(action);
            bar.add(right, BorderLayout.EAST);
        }
        p.add(bar, BorderLayout.NORTH);
        p.add(table, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        int id = session.getStudent().getId();
        Async.run(this, () -> {
            prereqText.clear();
            for (Course c : courseService.getCatalog()) {
                prereqText.put(c.getCourseId(), c.getPrerequisiteText());
            }
            return enrollmentService.getEnrollments(id);
        }, list -> {
            List<Enrollment> now = new ArrayList<>();
            List<Enrollment> past = new ArrayList<>();
            double credits = 0;
            for (Enrollment e : list) {
                if (e.isActive()) {
                    now.add(e);
                    credits += e.getCredits();
                } else {
                    past.add(e);
                }
            }
            current.setItems(now);
            completed.setItems(past);
            creditsLabel.setText(now.size() + " section(s), " + credits + " credits");
            status("Loaded " + list.size() + " enrollment record(s)");
        });
        Async.run(this, () -> enrollmentService.getWaitlist(id), waitlist::setItems);
        Async.run(this, () -> academicService.getStanding(id), st -> {
            StringBuilder sb = new StringBuilder(String.format("CGPA %.2f  (%s)", st.getCgpa(), st.getStandingText()));
            for (java.util.Map.Entry<String, Double> e : st.getSgpaBySemester().entrySet()) {
                sb.append(String.format("     %s SGPA %.2f", e.getKey(), e.getValue()));
            }
            cgpaLabel.setText(sb.toString());
            cgpaLabel.setForeground(st.isOnProbation() ? UIStyles.DANGER : UIStyles.TEXT);
        });
    }

    private void dropSelected() {
        Enrollment e = current.getSelected();
        if (e == null) {
            Dialogs.info(this, "Drop", "Select a course in the table first.");
            return;
        }
        if (Dialogs.confirm(this, "Drop course", "Drop " + e.getCourseCode() + " " + e.getComponentType() + "?")) {
            Async.run(this, () -> enrollmentService.drop(session, e.getEnrollmentId()), msg -> {
                Dialogs.info(this, "Dropped", msg);
                refresh();
            });
        }
    }

    private void leaveSelected() {
        WaitlistEntry w = waitlist.getSelected();
        if (w == null) {
            Dialogs.info(this, "Waitlist", "Select a waitlist entry first.");
            return;
        }
        Async.exec(this, () -> enrollmentService.leaveWaitlist(session, w), () -> {
            status("Left the waitlist for " + w.getSectionKey());
            refresh();
        });
    }
}
