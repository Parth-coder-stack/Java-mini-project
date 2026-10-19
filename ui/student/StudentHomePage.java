package ui.student;

import model.AcademicStanding;
import model.AttendanceSummary;
import model.Enrollment;
import model.Student;
import model.Submission;
import model.WaitlistEntry;
import service.TaskService;
import service.AcademicService;
import service.AttendanceService;
import service.EnrollmentService;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.StatCard;
import ui.components.StatusIcon;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Student overview: key numbers, current courses and a to-do list. */
public class StudentHomePage extends Page {

    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final AttendanceService attendanceService = new AttendanceService();
    private final StudentService studentService = new StudentService();
    private final TaskService taskService = new TaskService();
    private final AcademicService academicService = new AcademicService();

    private final StatCard coursesCard = new StatCard("Courses this semester", UIStyles.PRIMARY);
    private final StatCard creditsCard = new StatCard("Credits this semester", UIStyles.PALETTE.get(4));
    private final StatCard cgpaCard = new StatCard("CGPA (10-point)", UIStyles.SUCCESS);
    private final StatCard attendanceCard = new StatCard("Overall attendance", UIStyles.WARNING);
    private final StatCard waitlistCard = new StatCard("On waitlist", UIStyles.PALETTE.get(5));
    private final DataTable<Enrollment> currentTable;
    private final JPanel todo = UIStyles.clear(null);
    private final JLabel greeting = UIStyles.label(" ", UIStyles.H2, UIStyles.TEXT);

    /** Everything the page needs, loaded in one background task. */
    private static class Data {
        Student student;
        List<Enrollment> enrollments;
        List<AttendanceSummary> attendance;
        List<WaitlistEntry> waitlist;
        Map<Integer, Submission> submissions;
        AcademicStanding standing;
    }

    public StudentHomePage(BaseDashboard dashboard) {
        super(dashboard);
        JPanel stats = UIStyles.clear(new GridLayout(1, 5, 14, 0));
        stats.add(coursesCard);
        stats.add(creditsCard);
        stats.add(cgpaCard);
        stats.add(attendanceCard);
        stats.add(waitlistCard);

        // SRS: minimal-click access to enrollment, timetable and grades
        JPanel quick = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        quick.add(quickButton(UIStyles.primary("Register for a course"), "Course Catalog"));
        quick.add(quickButton(UIStyles.secondary("My timetable"), "Timetable"));
        quick.add(quickButton(UIStyles.secondary("My grades"), "My Courses"));
        JPanel head = UIStyles.clear(new BorderLayout());
        head.add(greeting, BorderLayout.WEST);
        head.add(quick, BorderLayout.EAST);

        JPanel top = UIStyles.clear(new BorderLayout(0, 12));
        top.add(head, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);

        currentTable = new DataTable<>(new String[]{"Course", "Title", "Type", "Slot", "Faculty", "Room"},
                e -> new Object[]{e.getCourseCode(), e.getCourseTitle(), e.getComponentType(), e.getSlot(),
                        e.getFacultyName(), e.getRoomNumber()});
        currentTable.setColumnWidths(70, 200, 60, 150, 140, 60);

        todo.setLayout(new BoxLayout(todo, BoxLayout.Y_AXIS));
        JPanel todoHolder = new JPanel(new BorderLayout());   // keeps items at the top instead of stretched
        todoHolder.setBackground(UIStyles.CARD);
        todoHolder.add(todo, BorderLayout.NORTH);
        javax.swing.JScrollPane todoScroll = UIStyles.scroll(todoHolder);
        todoScroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel todoCard = UIStyles.card("Things to do", todoScroll);
        todoCard.setPreferredSize(new java.awt.Dimension(380, 300));

        JPanel bottom = UIStyles.clear(new BorderLayout(16, 0));
        bottom.add(UIStyles.card("My current courses", currentTable), BorderLayout.CENTER);
        bottom.add(todoCard, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.CENTER);
    }

    private FlatButton quickButton(FlatButton b, String page) {
        b.addActionListener(e -> dashboard.showPage(page));
        return b;
    }

    @Override
    public void refresh() {
        int id = session.getStudent().getId();
        Async.run(this, () -> {
            Data d = new Data();
            d.student = studentService.getStudent(id);
            d.enrollments = enrollmentService.getEnrollments(id);
            d.attendance = attendanceService.getSummaries(id);
            d.waitlist = enrollmentService.getWaitlist(id);
            d.submissions = taskService.getMySubmissions(id);
            d.standing = academicService.getStanding(id);
            return d;
        }, this::show);
    }

    private void show(Data d) {
        session.refresh(d.student);
        greeting.setText("Welcome back, " + d.student.getName().split(" ")[0]);

        List<Enrollment> current = new ArrayList<>();
        Set<Integer> courseIds = new HashSet<>();
        double credits = 0;
        for (Enrollment e : d.enrollments) {
            if (e.isActive()) {
                current.add(e);
                courseIds.add(e.getCourseId());
                credits += e.getCredits();
            }
        }
        currentTable.setItems(current);
        coursesCard.setValue(String.valueOf(courseIds.size()));
        creditsCard.setValue(String.format("%.1f", credits));
        cgpaCard.setValue(String.format("%.2f", d.standing.getCgpa()),
                d.standing.isOnProbation() ? UIStyles.DANGER : UIStyles.TEXT);
        cgpaCard.setCaption("CGPA - " + d.standing.getStandingText());
        double overall = attendanceService.overallPercent(d.attendance);
        attendanceCard.setValue(String.format("%.0f%%", overall),
                overall < AttendanceService.MIN_PERCENT ? UIStyles.DANGER : UIStyles.TEXT);
        waitlistCard.setValue(String.valueOf(d.waitlist.size()));

        todo.removeAll();
        if (d.standing.isOnProbation()) {
            addTodo(StatusIcon.FAIL, "You are on academic probation",
                    String.format("CGPA %.2f is below %.2f - please meet your academic advisor.",
                            d.standing.getCgpa(), d.standing.getProbationThreshold()));
        }
        if (!d.student.isContactVerified()) {
            addTodo(StatusIcon.WARN, "Confirm your contact details", "Required before you can register for courses.");
        }
        for (AttendanceSummary a : d.attendance) {
            if (a.getTotal() > 0 && a.getPercent() < AttendanceService.MIN_PERCENT) {
                addTodo(StatusIcon.FAIL, "Low attendance in " + a.getCourseCode() + " " + a.getComponentType(),
                        String.format("%.1f%% - minimum is %.0f%%", a.getPercent(), AttendanceService.MIN_PERCENT));
            }
        }
        for (Submission s : d.submissions.values()) {
            if (Submission.REJECTED.equals(s.getStatus())) {
                addTodo(StatusIcon.FAIL, "Resubmit \"" + s.getTaskTitle() + "\"",
                        s.getCourseCode() + (s.getRemarks() == null ? "" : " - " + s.getRemarks()));
            }
        }
        for (WaitlistEntry w : d.waitlist) {
            addTodo(StatusIcon.WARN, "Waitlisted for " + w.getSectionKey(), "Position " + w.getPosition()
                    + " - you will be enrolled automatically when a seat opens.");
        }
        if (todo.getComponentCount() == 0) {
            addTodo(StatusIcon.OK, "You're all caught up", "Nothing needs your attention right now.");
        }
        todo.revalidate();
        todo.repaint();
        status("Dashboard updated");
    }

    private void addTodo(StatusIcon icon, String title, String detail) {
        JPanel row = UIStyles.clear(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel i = new JLabel(icon);
        i.setVerticalAlignment(JLabel.TOP);
        row.add(i, BorderLayout.WEST);
        row.add(UIStyles.label("<html><div style='width:230px'><b>" + title + "</b><br>"
                + "<span style='color:#6b7280'>" + detail + "</span></div></html>", UIStyles.BODY, UIStyles.TEXT),
                BorderLayout.CENTER);
        todo.add(row);
    }
}
