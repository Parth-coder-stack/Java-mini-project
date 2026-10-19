package ui.student;

import model.Course;
import model.EligibilityReport;
import model.Section;
import service.CourseService;
import service.EnrollmentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.common.CourseListPage;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Course catalog with prerequisites and the student's eligibility for each course. */
public class CatalogPage extends Page {

    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final Map<Integer, EligibilityReport> reports = new HashMap<>();

    private final DataTable<Course> courses;
    private final DataTable<Section> sections = new DataTable<>(CourseListPage.sectionColumns(), CourseListPage::sectionRow);
    private final ChecklistPanel checklist = new ChecklistPanel();
    private final JLabel courseHeading = UIStyles.heading("Select a course");
    private final JLabel courseDescription = UIStyles.muted(" ");
    private final FlatButton registerButton = UIStyles.primary("Register for this course...");

    public CatalogPage(BaseDashboard dashboard) {
        super(dashboard);
        courses = new DataTable<Course>(new String[]{"Code", "Title", "Dept", "Credits", "Prerequisites", "Your status"},
                c -> new Object[]{c.getCourseCode(), c.getTitle(), c.getDeptName(), c.getCredits(),
                        c.getPrerequisiteText(), reports.containsKey(c.getCourseId())
                        ? reports.get(c.getCourseId()).getSummary() : "-"}).withFilter("Filter courses:");
        courses.setColumnWidths(70, 230, 50, 60, 240, 260);
        courses.onSelect(this::showCourse);
        courses.onDoubleClick(c -> openWizard());

        registerButton.setEnabled(false);
        registerButton.addActionListener(e -> openWizard());

        JPanel detailTop = UIStyles.clear(new BorderLayout(0, 4));
        detailTop.add(courseHeading, BorderLayout.NORTH);
        detailTop.add(courseDescription, BorderLayout.CENTER);
        JPanel eligibility = UIStyles.card();
        eligibility.add(detailTop, BorderLayout.NORTH);
        eligibility.add(UIStyles.scroll(checklist), BorderLayout.CENTER);
        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.add(registerButton);
        eligibility.add(actions, BorderLayout.SOUTH);

        JPanel lower = UIStyles.clear(new GridLayout(1, 2, 16, 0));
        lower.add(eligibility);
        lower.add(UIStyles.card("Sections this semester", sections));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UIStyles.card("Course catalog", courses), lower);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        int studentId = session.getStudent().getId();
        Async.run(this, () -> {
            List<Course> list = courseService.getCatalog();
            Map<Integer, EligibilityReport> r = enrollmentService.checkCatalog(studentId, list);
            reports.clear();
            reports.putAll(r);
            return list;
        }, list -> {
            courses.setItems(list);
            showCourse(null);
            long eligible = reports.values().stream().filter(r -> r.isEligible() && !r.isEnrolledNow()).count();
            status(list.size() + " courses, " + eligible + " you can register for right now");
        });
    }

    private void showCourse(Course c) {
        registerButton.setEnabled(c != null);
        if (c == null) {
            courseHeading.setText("Select a course to see whether you can register");
            courseDescription.setText(" ");
            checklist.setChecks(Collections.emptyList(), "No course selected");
            sections.setItems(Collections.emptyList());
            return;
        }
        courseHeading.setText(c.getCourseCode() + " - " + c.getTitle() + "  (" + c.getCredits() + " credits)");
        courseDescription.setText(c.getDescription() == null ? " " : c.getDescription());
        EligibilityReport r = reports.get(c.getCourseId());
        checklist.setChecks(r == null ? Collections.emptyList() : r.getChecks(), "No requirements");
        Async.run(this, () -> courseService.getSectionsForCourse(c.getCourseId()), sections::setItems);
    }

    private void openWizard() {
        Course c = courses.getSelected();
        if (c == null) {
            Dialogs.info(this, "Register", "Select a course first.");
            return;
        }
        new EnrollmentWizard(dashboard.getFrame(), session, c).setVisible(true);
        refresh();
    }
}
