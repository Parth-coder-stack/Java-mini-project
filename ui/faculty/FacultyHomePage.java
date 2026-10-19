package ui.faculty;

import model.Section;
import model.Submission;
import service.TaskService;
import service.CourseService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.StatCard;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/** Faculty overview: sections taught, number of students, pending assignment reviews. */
public class FacultyHomePage extends Page {

    private final CourseService courseService = new CourseService();
    private final TaskService taskService = new TaskService();
    private final StatCard sectionsCard = new StatCard("Sections this semester", UIStyles.PRIMARY);
    private final StatCard studentsCard = new StatCard("Students taught", UIStyles.SUCCESS);
    private final StatCard reviewsCard = new StatCard("Submissions to review", UIStyles.WARNING);
    private final DataTable<Section> sections;

    public FacultyHomePage(BaseDashboard dashboard) {
        super(dashboard);
        JPanel stats = UIStyles.clear(new GridLayout(1, 3, 14, 0));
        stats.add(sectionsCard);
        stats.add(studentsCard);
        stats.add(reviewsCard);
        sections = new DataTable<>(new String[]{"Course", "Title", "Type", "Sec", "Slot", "Room", "Enrolled", "Capacity"},
                s -> new Object[]{s.getCourseCode(), s.getCourseTitle(), s.getComponentType(), s.getSectionNumber(),
                        s.getSlot(), s.getRoomNumber(), s.getEnrolledCount(), s.getMaxCapacity()});
        sections.setColumnWidths(70, 220, 60, 40, 160, 60, 70, 70);
        add(stats, BorderLayout.NORTH);
        add(UIStyles.card("My sections", sections), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        int id = session.getFaculty().getId();
        Async.run(this, () -> courseService.getSectionsForFaculty(id), (List<Section> list) -> {
            sections.setItems(list);
            sectionsCard.setValue(String.valueOf(list.size()));
            int students = 0;
            for (Section s : list) {
                students += s.getEnrolledCount();
            }
            studentsCard.setValue(String.valueOf(students));
        });
        Async.run(this, () -> taskService.getSubmissionsForFaculty(id), subs -> {
            long pending = subs.stream().filter(s -> Submission.SUBMITTED.equals(s.getStatus())).count();
            reviewsCard.setValue(String.valueOf(pending), pending > 0 ? UIStyles.WARNING : UIStyles.TEXT);
            status("Welcome, " + session.getPerson().getName());
        });
    }
}
