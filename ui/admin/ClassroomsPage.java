package ui.admin;

import model.Classroom;
import model.RoomUsage;
import service.ClassroomService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.FormBuilder;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Admin: add / edit / delete classrooms, with this semester's utilization. */
public class ClassroomsPage extends Page {

    private final ClassroomService classroomService = new ClassroomService();
    private final DataTable<RoomUsage> table;

    public ClassroomsPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<RoomUsage>(new String[]{"Room", "Type", "Capacity", "Sections", "Hours booked / week",
                "Time used", "Seats filled"},
                u -> new Object[]{u.getRoom().getRoomNumber(), u.getRoom().getRoomType(), u.getRoom().getCapacity(),
                        u.getSections(), String.format("%.1f of %.0f", u.getBookedHours(), RoomUsage.AVAILABLE_HOURS_PER_WEEK),
                        u.getTimeUtilization() + "%", u.getSeatUtilization() + "%"}).withFilter("Filter:");

        FlatButton add = UIStyles.primary("Add classroom");
        FlatButton edit = UIStyles.secondary("Edit");
        FlatButton delete = UIStyles.danger("Delete");
        add.addActionListener(e -> openDialog(null));
        edit.addActionListener(e -> {
            RoomUsage u = table.getSelected();
            if (u == null) {
                Dialogs.info(this, "Classrooms", "Select a classroom first.");
            } else {
                openDialog(u.getRoom());
            }
        });
        delete.addActionListener(e -> deleteSelected());
        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(UIStyles.muted("Time used = booked hours out of Mon-Sat 08:00-18:00 (60 h)."));

        JPanel card = UIStyles.card("Classrooms and utilization this semester", table);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, classroomService::getUtilization, list -> {
            table.setItems(list);
            status(list.size() + " classrooms");
        });
    }

    private void openDialog(Classroom existing) {
        JTextField number = UIStyles.textField(10);
        JComboBox<String> type = new JComboBox<>(new String[]{"Theory", "Lab"});
        JTextField capacity = UIStyles.textField(6);
        if (existing != null) {
            number.setText(existing.getRoomNumber());
            type.setSelectedItem(existing.getRoomType());
            capacity.setText(String.valueOf(existing.getCapacity()));
        }
        JPanel form = new FormBuilder()
                .row(FormBuilder.labeled("Room number (e.g. D-101)", number), FormBuilder.labeled("Type", type),
                        FormBuilder.labeled("Seats", capacity))
                .build();
        FormDialog dialog = new FormDialog(dashboard.getFrame(), existing == null ? "Add classroom" : "Edit classroom",
                form, () -> {
            if (existing == null) {
                classroomService.create(session, number.getText(), (String) type.getSelectedItem(), capacity.getText());
            } else {
                classroomService.update(session, existing, number.getText(), (String) type.getSelectedItem(),
                        capacity.getText());
            }
        });
        if (dialog.showDialog()) {
            status("Classroom saved");
            refresh();
        }
    }

    private void deleteSelected() {
        RoomUsage u = table.getSelected();
        if (u == null) {
            Dialogs.info(this, "Classrooms", "Select a classroom first.");
            return;
        }
        if (Dialogs.confirm(this, "Delete classroom", "Delete room " + u.getRoom().getRoomNumber() + "?\n"
                + "Rooms that have sections cannot be deleted.")) {
            Async.exec(this, () -> classroomService.delete(session, u.getRoom()), () -> {
                status("Deleted " + u.getRoom().getRoomNumber());
                refresh();
            });
        }
    }
}
