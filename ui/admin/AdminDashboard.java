package ui.admin;

import service.Session;
import ui.BaseDashboard;
import ui.MainFrame;
import ui.common.StudentDirectoryPage;

/** Dashboard for the administrator. */
public class AdminDashboard extends BaseDashboard {

    public AdminDashboard(MainFrame frame, Session session) {
        super(frame, session);
    }

    @Override
    protected void buildPages() {
        addPage("Home", "University-wide numbers for this semester", new AdminHomePage(this));
        addPage("Courses", "Courses, prerequisite courses and prerequisite tasks (assignments / videos)",
                new CourseManagementPage(this));
        addPage("Sections", "Schedule sections, assign faculty and rooms, manage waitlists",
                new SectionManagementPage(this));
        addPage("Faculty", "Add and edit faculty; see each member's teaching load", new FacultyPage(this));
        addPage("Classrooms", "Add and edit classrooms; see how much each is used", new ClassroomsPage(this));
        addPage("Enrollments", "Register or drop students; override prerequisites if needed",
                new EnrollmentManagementPage(this));
        addPage("Student Search", "Find any student and view or delete their record", new StudentDirectoryPage(this));
        addPage("Reports", "Fill rates, classroom utilization, teaching load, academic standing",
                new ReportsPage(this));
        addPage("Audit Log", "History of every grade change (written by a database trigger)", new AuditLogPage(this));
    }
}
