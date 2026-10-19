package ui.common;

import model.AttendanceSummary;
import model.Enrollment;
import model.Student;
import service.AcademicService;
import service.AttendanceService;
import service.EnrollmentService;
import ui.Async;
import ui.UIStyles;
import ui.components.Avatar;
import ui.components.DataTable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/** Full record of one student for faculty/admin: profile, courses with grades, attendance. */
public class StudentDetailsDialog extends JDialog {

    private final JLabel cgpaLabel = UIStyles.label("CGPA: -", UIStyles.H3, UIStyles.PRIMARY);

    public StudentDetailsDialog(JFrame owner, Student s) {
        super(owner, "Student record - " + s.getName(), true);

        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(UIStyles.CARD);
        header.setBorder(BorderFactory.createEmptyBorder(18, 20, 14, 20));
        header.add(new Avatar(s.getName(), 56), BorderLayout.WEST);
        JPanel names = UIStyles.clear(null);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(UIStyles.label(s.getName(), UIStyles.H2, UIStyles.TEXT));
        names.add(UIStyles.muted("PRN " + s.getPrn() + "   |   " + s.getDeptName() + "   |   Year " + s.getYearOfStudy()));
        names.add(cgpaLabel);
        header.add(names, BorderLayout.CENTER);

        JPanel info = new JPanel(new GridLayout(2, 3, 16, 8));
        info.setBackground(UIStyles.CARD);
        info.setBorder(BorderFactory.createEmptyBorder(0, 20, 14, 20));
        info.add(infoCell("Email", s.getEmail()));
        info.add(infoCell("Phone", s.getPhone()));
        info.add(infoCell("Address", s.getAddress()));
        info.add(infoCell("Department", s.getDeptName()));
        info.add(infoCell("Year", String.valueOf(s.getYearOfStudy())));
        info.add(infoCell("Contact details", s.isContactVerified()
                ? "Verified " + s.getContactVerifiedAt().toLocalDate() : "Not verified"));

        DataTable<Enrollment> courses = new DataTable<>(
                new String[]{"Semester", "Course", "Title", "Type", "Section", "Faculty", "Status", "Grade"},
                e -> new Object[]{e.getSemesterName(), e.getCourseCode(), e.getCourseTitle(), e.getComponentType(),
                        e.getSectionNumber(), e.getFacultyName(), e.getStatus(), e.getGrade() == null ? "-" : e.getGrade()});
        DataTable<AttendanceSummary> attendance = new DataTable<>(
                new String[]{"Course", "Type", "Present", "Late", "Absent", "Total", "Attendance"},
                a -> new Object[]{a.getCourseCode(), a.getComponentType(), a.getPresent(), a.getLate(),
                        a.getAbsent(), a.getTotal(), percent(a.getPercent())});

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Courses & grades", wrap(courses));
        tabs.addTab("Attendance", wrap(attendance));

        JPanel top = new JPanel(new BorderLayout());
        top.add(header, BorderLayout.NORTH);
        top.add(info, BorderLayout.CENTER);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIStyles.BG);
        content.add(top, BorderLayout.NORTH);
        content.add(tabs, BorderLayout.CENTER);
        setContentPane(content);
        setSize(new Dimension(980, 620));
        setLocationRelativeTo(owner);

        EnrollmentService enrollmentService = new EnrollmentService();
        Async.run(this, () -> enrollmentService.getEnrollments(s.getId()), (List<Enrollment> list) -> courses.setItems(list));
        Async.run(this, () -> new AcademicService().getStanding(s.getId()), st -> {
            cgpaLabel.setText(String.format("CGPA: %.2f   -   %s", st.getCgpa(), st.getStandingText()));
            cgpaLabel.setForeground(st.isOnProbation() ? UIStyles.DANGER : UIStyles.PRIMARY);
        });
        Async.run(this, () -> new AttendanceService().getSummaries(s.getId()), attendance::setItems);
    }

    static String percent(double p) {
        return String.format("%.1f%%", p) + (p < AttendanceService.MIN_PERCENT ? " (low)" : "");
    }

    private static JPanel infoCell(String caption, String value) {
        JPanel p = UIStyles.clear(new BorderLayout());
        p.add(UIStyles.muted(caption), BorderLayout.NORTH);
        p.add(UIStyles.label(value == null || value.isEmpty() ? "-" : value, UIStyles.BOLD, UIStyles.TEXT),
                BorderLayout.CENTER);
        return p;
    }

    private static JPanel wrap(JPanel content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UIStyles.CARD);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.add(content);
        return p;
    }
}
