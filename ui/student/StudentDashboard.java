package ui.student;

import model.Enrollment;
import service.EnrollmentService;
import service.Session;
import ui.BaseDashboard;
import ui.MainFrame;
import ui.common.NotificationsPage;
import ui.common.StudentDirectoryPage;
import ui.common.TimetablePage;
import ui.components.TimetableGrid;

import java.util.ArrayList;
import java.util.List;

/** Dashboard for a logged-in student. */
public class StudentDashboard extends BaseDashboard {

    public StudentDashboard(MainFrame frame, Session session) {
        super(frame, session);
    }

    @Override
    protected void buildPages() {
        addPage("Home", "Your semester at a glance", new StudentHomePage(this));
        addPage("Course Catalog", "All courses, their prerequisites and whether you can register",
                new CatalogPage(this));
        addPage("My Courses", "Your registrations, grades and waitlist", new MyCoursesPage(this));
        addPage("Attendance", "How many classes you have attended in each course", new AttendancePage(this));
        addPage("Timetable", "Your weekly class schedule", new TimetablePage(this, () -> {
            List<TimetableGrid.Entry> entries = new ArrayList<>();
            for (Enrollment e : new EnrollmentService().getEnrollments(session.getStudent().getId())) {
                if (e.isActive()) {
                    entries.add(new TimetableGrid.Entry(e.getSlot(), e.getCourseCode() + " " + e.getComponentType(),
                            e.getRoomNumber() + " - " + e.getFacultyName()));
                }
            }
            return entries;
        }));
        addPage("My Faculty", "Who teaches you this semester and how to reach them", new MyFacultyPage(this));
        addPage("Prerequisite Tasks", "Assignments and videos to finish before you can register", new TasksPage(this));
        addPage("Student Search", "Find a student by PRN, name or email", new StudentDirectoryPage(this));
        addPage("My Profile", "Your details and contact information", new ProfilePage(this));
        addPage(NotificationsPage.TITLE, "Registration results, waitlist moves and grades", new NotificationsPage(this));
    }
}
