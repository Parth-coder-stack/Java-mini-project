package ui.common;

import model.Course;
import model.Section;
import service.CourseService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;

import javax.swing.JSplitPane;
import java.awt.BorderLayout;

/** Read-only catalog: every course with its prerequisites, and this semester's sections. */
public class CourseListPage extends Page {

    private final CourseService courseService = new CourseService();
    private final DataTable<Course> courses;
    private final DataTable<Section> sections;

    public CourseListPage(BaseDashboard dashboard) {
        super(dashboard);
        courses = new DataTable<Course>(new String[]{"Code", "Title", "Dept", "Credits", "Prerequisites"},
                c -> new Object[]{c.getCourseCode(), c.getTitle(), c.getDeptName(), c.getCredits(),
                        c.getPrerequisiteText()}).withFilter("Filter:");
        courses.setColumnWidths(80, 260, 60, 60, 300);
        sections = new DataTable<>(sectionColumns(), CourseListPage::sectionRow);
        courses.onSelect(c -> {
            if (c != null) {
                Async.run(this, () -> courseService.getSectionsForCourse(c.getCourseId()), sections::setItems);
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UIStyles.card("All courses", courses),
                UIStyles.card("Sections this semester (select a course above)", sections));
        split.setResizeWeight(0.6);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    public static String[] sectionColumns() {
        return new String[]{"Course", "Type", "Section", "Slot", "Faculty", "Room", "Seats"};
    }

    public static Object[] sectionRow(Section s) {
        return new Object[]{s.getCourseCode(), s.getComponentType(), s.getSectionNumber(), s.getSlot(),
                s.getFacultyName(), s.getRoomNumber(),
                s.isFull() ? "Full" : s.getSeatsLeft() + " of " + s.getMaxCapacity() + " free"};
    }

    @Override
    public void refresh() {
        Async.run(this, courseService::getCatalog, list -> {
            courses.setItems(list);
            sections.setItems(java.util.Collections.emptyList());
            status(list.size() + " courses in the catalog");
        });
    }
}
