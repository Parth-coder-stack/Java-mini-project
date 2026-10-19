package ui.admin;

import model.Section;
import service.CourseService;
import service.ReportService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.StatCard;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/** Admin overview: totals and how full each section is. */
public class AdminHomePage extends Page {

    private final ReportService reportService = new ReportService();
    private final CourseService courseService = new CourseService();
    private final Map<String, StatCard> cards = new LinkedHashMap<>();
    private final DataTable<Section> sections;

    public AdminHomePage(BaseDashboard dashboard) {
        super(dashboard);
        JPanel stats = UIStyles.clear(new GridLayout(1, 6, 12, 0));
        String[] keys = {"Students", "Faculty", "Courses", "Sections this semester", "Active enrollments", "On waitlist"};
        for (int i = 0; i < keys.length; i++) {
            StatCard c = new StatCard(keys[i], UIStyles.PALETTE.get(i));
            cards.put(keys[i], c);
            stats.add(c);
        }
        sections = new DataTable<Section>(new String[]{"Course", "Title", "Type", "Sec", "Slot", "Faculty", "Room",
                "Enrolled", "Capacity", "Fill"},
                s -> new Object[]{s.getCourseCode(), s.getCourseTitle(), s.getComponentType(), s.getSectionNumber(),
                        s.getSlot(), s.getFacultyName(), s.getRoomNumber(), s.getEnrolledCount(), s.getMaxCapacity(),
                        s.isFull() ? "Full" : (s.getEnrolledCount() * 100 / s.getMaxCapacity()) + "%"})
                .withFilter("Filter:");
        sections.setColumnWidths(70, 200, 60, 40, 150, 140, 60, 60, 60, 60);
        add(stats, BorderLayout.NORTH);
        add(UIStyles.card("Sections this semester", sections), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, reportService::getCounts, counts -> {
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                StatCard c = cards.get(e.getKey());
                if (c != null) {
                    c.setValue(String.valueOf(e.getValue()));
                }
            }
        });
        Async.run(this, courseService::getCurrentSections, list -> {
            sections.setItems(list);
            status(list.size() + " sections this semester");
        });
    }
}
