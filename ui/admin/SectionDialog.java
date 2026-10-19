package ui.admin;

import model.Classroom;
import model.Course;
import model.CourseComponent;
import model.Faculty;
import model.Section;
import service.CourseService;
import service.Session;
import service.StudentService;
import ui.Async;
import ui.Dialogs;
import ui.UIStyles;
import ui.components.FlatButton;
import ui.components.FormBuilder;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Create a section of a course component in the current semester, or edit one
 * (re-assign faculty, room, time slot, capacity). The service rejects faculty or
 * classroom double booking and rooms that are too small.
 */
public class SectionDialog extends JDialog {

    private final CourseService courseService = new CourseService();
    private final JComboBox<Course> course = new JComboBox<>();
    private final JComboBox<CourseComponent> component = new JComboBox<>();
    private final JComboBox<Faculty> faculty = new JComboBox<>();
    private final JComboBox<Classroom> room = new JComboBox<>();
    private final JTextField sectionNo = UIStyles.textField(4);
    private final JTextField capacity = UIStyles.textField(4);
    private final JTextField slot = UIStyles.textField(20);
    private boolean saved;

    public SectionDialog(JFrame owner, Session session) {
        this(owner, session, null);
    }

    /** @param existing the section to edit, or null to create a new one */
    public SectionDialog(JFrame owner, Session session, Section existing) {
        super(owner, existing == null ? "Add section (current semester)" : "Edit section " + existing.getKey(), true);
        if (existing == null) {
            sectionNo.setText("B");
            capacity.setText("40");
            slot.setText("Mon/Wed 15:00-16:30");
            course.addActionListener(e -> loadComponents());
        } else {
            sectionNo.setText(existing.getSectionNumber());
            capacity.setText(String.valueOf(existing.getMaxCapacity()));
            slot.setText(existing.getSlot());
            course.setEnabled(false);
            component.setEnabled(false);
            sectionNo.setEnabled(false);
        }

        JPanel form = new FormBuilder()
                .row(FormBuilder.labeled("Course", course), FormBuilder.labeled("Component", component))
                .row(FormBuilder.labeled("Faculty", faculty), FormBuilder.labeled("Room", room))
                .row(FormBuilder.labeled("Section", sectionNo), FormBuilder.labeled("Capacity", capacity))
                .field("Slot  (days separated by /, then HH:MM-HH:MM, e.g. Tue/Thu 11:00-12:30)", slot)
                .build();

        FlatButton save = UIStyles.primary(existing == null ? "Create section" : "Save changes");
        FlatButton cancel = UIStyles.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            Course c = (Course) course.getSelectedItem();
            CourseComponent cc = (CourseComponent) component.getSelectedItem();
            Faculty f = (Faculty) faculty.getSelectedItem();
            Classroom r = (Classroom) room.getSelectedItem();
            if (existing != null && f != null && r != null) {
                Async.exec(this, () -> courseService.updateSection(session, existing, f.getId(), r,
                        capacity.getText(), slot.getText()), () -> {
                    saved = true;
                    dispose();
                });
                return;
            }
            if (c == null || cc == null || f == null || r == null) {
                Dialogs.warn(this, "Section", "Choose course, component, faculty and room.");
                return;
            }
            Async.run(this, () -> courseService.createSection(session, c.getCourseId(), cc.getComponentId(),
                    f.getId(), r, sectionNo.getText(), capacity.getText(), slot.getText()), id -> {
                saved = true;
                dispose();
            });
        });
        JPanel buttons = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(save);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(UIStyles.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(720, 400);
        setLocationRelativeTo(owner);

        Async.run(this, courseService::getCatalog, list -> {
            fill(course, list);
            if (existing != null) {
                for (Course c : list) {
                    if (c.getCourseId() == existing.getCourseId()) {
                        course.setSelectedItem(c);
                    }
                }
                component.addItem(new CourseComponent(existing.getComponentId(), existing.getCourseId(),
                        existing.getComponentType(), existing.getCredits()));
            }
        });
        Async.run(this, () -> new StudentService().getAllFaculty(), list -> {
            fill(faculty, list);
            if (existing != null) {
                for (Faculty f : list) {
                    if (f.getId() == existing.getFacultyId()) {
                        faculty.setSelectedItem(f);
                    }
                }
            }
        });
        Async.run(this, courseService::getRooms, list -> {
            fill(room, list);
            if (existing != null) {
                for (Classroom r : list) {
                    if (r.getRoomId() == existing.getRoomId()) {
                        room.setSelectedItem(r);
                    }
                }
            }
        });
    }

    private static <T> void fill(JComboBox<T> box, List<T> items) {
        for (T item : items) {
            box.addItem(item);
        }
    }

    private void loadComponents() {
        Course c = (Course) course.getSelectedItem();
        component.removeAllItems();
        if (c != null) {
            Async.run(this, () -> courseService.getComponents(c.getCourseId()), list -> {
                for (CourseComponent cc : list) {
                    component.addItem(cc);
                }
            });
        }
    }

    public boolean showDialog() {
        setVisible(true);
        return saved;
    }
}
