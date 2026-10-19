package ui.faculty;

import model.Section;
import service.CourseService;
import service.Session;
import ui.BaseDashboard;
import ui.MainFrame;
import ui.common.CourseListPage;
import ui.common.NotificationsPage;
import ui.common.StudentDirectoryPage;
import ui.common.TimetablePage;
import ui.components.TimetableGrid;

import java.util.ArrayList;
import java.util.List;

/** Dashboard for a logged-in faculty member. */
public class FacultyDashboard extends BaseDashboard {

    public FacultyDashboard(MainFrame frame, Session session) {
        super(frame, session);
    }

    @Override
    protected void buildPages() {
        addPage("Home", "Your teaching this semester", new FacultyHomePage(this));
        addPage("Sections & Rosters", "Your assigned sections, class lists and waitlists", new RosterPage(this));
        addPage("Mark Attendance", "Record Present / Absent / Late for a class", new MarkAttendancePage(this));
        addPage("Grades", "Enter final grades for your students", new GradesPage(this));
        addPage("Review Assignments", "Approve or reject prerequisite assignment answers", new ReviewSubmissionsPage(this));
        addPage("Timetable", "Your weekly teaching schedule", new TimetablePage(this, () -> {
            List<TimetableGrid.Entry> entries = new ArrayList<>();
            for (Section s : new CourseService().getSectionsForFaculty(session.getFaculty().getId())) {
                entries.add(new TimetableGrid.Entry(s.getSlot(), s.getCourseCode() + " " + s.getComponentType(),
                        s.getRoomNumber() + " - Sec " + s.getSectionNumber() + " - " + s.getEnrolledCount() + " students"));
            }
            return entries;
        }));
        addPage("Teaching Load", "Your sections, students and hours across semesters", new TeachingLoadPage(this));
        addPage("Courses", "Course catalog with prerequisites and sections", new CourseListPage(this));
        addPage("Student Search", "Find a student and view their full record", new StudentDirectoryPage(this));
        addPage(NotificationsPage.TITLE, "New submissions to review and other updates", new NotificationsPage(this));
    }
}
