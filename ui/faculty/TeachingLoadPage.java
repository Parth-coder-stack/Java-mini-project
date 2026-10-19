package ui.faculty;

import model.Section;
import model.TeachingLoad;
import service.CourseService;
import service.FacultyService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.StatCard;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/** Faculty: own teaching load per semester (sections, students, weekly hours, credits). */
public class TeachingLoadPage extends Page {

    private final FacultyService facultyService = new FacultyService();
    private final CourseService courseService = new CourseService();
    private final StatCard semestersCard = new StatCard("Semesters taught", UIStyles.PRIMARY);
    private final StatCard sectionsCard = new StatCard("Sections in total", UIStyles.SUCCESS);
    private final StatCard studentsCard = new StatCard("Students taught", UIStyles.WARNING);
    private final StatCard hoursCard = new StatCard("Hours / week, latest semester", UIStyles.PALETTE.get(5));
    private final DataTable<TeachingLoad> perSemester;
    private final DataTable<Section> sections;

    public TeachingLoadPage(BaseDashboard dashboard) {
        super(dashboard);
        JPanel stats = UIStyles.clear(new GridLayout(1, 4, 14, 0));
        stats.add(semestersCard);
        stats.add(sectionsCard);
        stats.add(studentsCard);
        stats.add(hoursCard);
        perSemester = new DataTable<>(new String[]{"Semester", "Sections", "Students", "Hours / week", "Credits"},
                t -> new Object[]{t.getSemesterName(), t.getSections(), t.getStudents(),
                        String.format("%.1f", t.getWeeklyHours()), t.getCredits()});
        sections = new DataTable<>(new String[]{"Semester", "Course", "Type", "Sec", "Slot", "Room", "Students", "Hours / week"},
                s -> new Object[]{s.getSemesterName(), s.getCourseCode(), s.getComponentType(), s.getSectionNumber(),
                        s.getSlot(), s.getRoomNumber(), s.getEnrolledCount(), String.format("%.1f", s.getWeeklyHours())});
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UIStyles.card("Load per semester", perSemester), UIStyles.card("All sections taught", sections));
        split.setResizeWeight(0.4);
        split.setBorder(null);
        add(stats, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> facultyService.getTeachingLoad(session.getFaculty()), (List<TeachingLoad> rows) -> {
            perSemester.setItems(rows);
            int sec = 0;
            int stu = 0;
            for (TeachingLoad t : rows) {
                sec += t.getSections();
                stu += t.getStudents();
            }
            semestersCard.setValue(String.valueOf(rows.size()));
            sectionsCard.setValue(String.valueOf(sec));
            studentsCard.setValue(String.valueOf(stu));
            hoursCard.setValue(rows.isEmpty() ? "0" : String.format("%.1f", rows.get(0).getWeeklyHours()));
            status("Teaching load across " + rows.size() + " semester(s)");
        });
        Async.run(this, () -> courseService.getAllSectionsForFaculty(session.getFaculty().getId()),
                sections::setItems);
    }
}
