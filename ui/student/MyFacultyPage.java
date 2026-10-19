package ui.student;

import model.Faculty;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Map;

/** Faculty teaching the student this semester, with their contact details. */
public class MyFacultyPage extends Page {

    private final StudentService studentService = new StudentService();
    private final DataTable<Map.Entry<Faculty, String>> table;

    public MyFacultyPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<>(new String[]{"Name", "Designation", "Department", "Email", "Phone", "Teaches you"},
                e -> new Object[]{e.getKey().getName(), e.getKey().getDesignation(), e.getKey().getDeptName(),
                        e.getKey().getEmail(), e.getKey().getPhone(), e.getValue()});
        table.setColumnWidths(160, 150, 200, 220, 100, 200);
        add(UIStyles.card("Faculty for your current courses", table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> studentService.getMyFaculty(session.getStudent().getId()), map -> {
            table.setItems(new ArrayList<>(map.entrySet()));
            status(map.size() + " faculty member(s) teach you this semester");
        });
    }
}
