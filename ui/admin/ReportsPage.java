package ui.admin;

import model.AcademicStanding;
import model.RoomUsage;
import model.Section;
import model.TeachingLoad;
import service.AcademicService;
import service.ClassroomService;
import service.CourseService;
import service.FacultyService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Admin reports: section fill rates, classroom utilization, faculty teaching load, academic standing. */
public class ReportsPage extends Page {

    private final CourseService courseService = new CourseService();
    private final ClassroomService classroomService = new ClassroomService();
    private final FacultyService facultyService = new FacultyService();
    private final AcademicService academicService = new AcademicService();

    private final DataTable<Section> fill;
    private final DataTable<RoomUsage> rooms;
    private final DataTable<TeachingLoad> load;
    private final DataTable<AcademicStanding> probation;
    private final JTextField threshold = UIStyles.textField(5);
    private final JLabel probationInfo = UIStyles.muted(" ");

    public ReportsPage(BaseDashboard dashboard) {
        super(dashboard);
        fill = new DataTable<Section>(new String[]{"Course", "Type", "Sec", "Faculty", "Room", "Enrolled", "Capacity", "Fill rate"},
                s -> new Object[]{s.getCourseCode(), s.getComponentType(), s.getSectionNumber(), s.getFacultyName(),
                        s.getRoomNumber(), s.getEnrolledCount(), s.getMaxCapacity(),
                        s.isFull() ? "Full" : s.getFillPercent() + "%"}).withFilter("Filter:");
        rooms = new DataTable<>(new String[]{"Room", "Type", "Capacity", "Sections", "Hours booked / week", "Time used", "Seats filled"},
                u -> new Object[]{u.getRoom().getRoomNumber(), u.getRoom().getRoomType(), u.getRoom().getCapacity(),
                        u.getSections(), String.format("%.1f", u.getBookedHours()), u.getTimeUtilization() + "%",
                        u.getSeatUtilization() + "%"});
        load = new DataTable<>(new String[]{"Faculty", "Semester", "Sections", "Students", "Hours / week", "Credits"},
                t -> new Object[]{t.getFacultyName(), t.getSemesterName(), t.getSections(), t.getStudents(),
                        String.format("%.1f", t.getWeeklyHours()), t.getCredits()});
        probation = new DataTable<>(new String[]{"PRN", "Student", "Department", "Year", "CGPA", "Standing"},
                a -> new Object[]{a.getStudent().getPrn(), a.getStudent().getName(), a.getStudent().getDeptName(),
                        a.getStudent().getYearOfStudy(), String.format("%.2f", a.getCgpa()), "On probation"});

        FlatButton save = UIStyles.primary("Save threshold");
        save.addActionListener(e -> Async.exec(this, () -> academicService.setProbationThreshold(session, threshold.getText()),
                this::loadProbation));
        JPanel bar = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.add(UIStyles.label("Probation if CGPA is below", UIStyles.BOLD, UIStyles.TEXT));
        bar.add(threshold);
        bar.add(save);
        bar.add(probationInfo);
        JPanel standing = tab(probation);
        standing.add(bar, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Section fill rates", tab(fill));
        tabs.addTab("Classroom utilization", tab(rooms));
        tabs.addTab("Faculty teaching load", tab(load));
        tabs.addTab("Academic standing", standing);
        add(tabs, BorderLayout.CENTER);
    }

    private static JPanel tab(JPanel content) {
        JPanel p = UIStyles.card();
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        Async.run(this, courseService::getCurrentSections, fill::setItems);
        Async.run(this, classroomService::getUtilization, rooms::setItems);
        Async.run(this, facultyService::getCurrentLoadReport, load::setItems);
        Async.run(this, academicService::getProbationThreshold, t -> threshold.setText(String.format("%.2f", t)));
        loadProbation();
        status("Reports for the current semester");
    }

    private void loadProbation() {
        Async.run(this, academicService::getProbationList, list -> {
            probation.setItems(list);
            probationInfo.setText(list.size() + " student(s) on academic probation (CGPA from MySQL fn_cgpa)");
        });
    }
}
