package ui.admin;

import model.Section;
import model.WaitlistEntry;
import service.CourseService;
import service.EnrollmentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;

/**
 * Admin: create, edit (faculty assignment / room / slot / capacity) and delete sections;
 * manage each section's waitlist, including the exceptional-case override.
 */
public class SectionManagementPage extends Page {

    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final DataTable<Section> sections;
    private final DataTable<WaitlistEntry> waitlist;

    public SectionManagementPage(BaseDashboard dashboard) {
        super(dashboard);
        sections = new DataTable<Section>(new String[]{"Course", "Title", "Type", "Sec", "Slot", "Faculty", "Room",
                "Enrolled", "Capacity"},
                s -> new Object[]{s.getCourseCode(), s.getCourseTitle(), s.getComponentType(), s.getSectionNumber(),
                        s.getSlot(), s.getFacultyName(), s.getRoomNumber(), s.getEnrolledCount(), s.getMaxCapacity()})
                .withFilter("Filter:");
        sections.setColumnWidths(70, 200, 60, 40, 150, 140, 60, 60, 60);
        waitlist = new DataTable<>(new String[]{"Position", "Student", "Waiting since", "Status"},
                w -> new Object[]{w.getPosition(), w.getStudentName(), w.getWaitlistDate(), w.getStatus()});
        sections.onSelect(s -> {
            if (s == null) {
                waitlist.setItems(Collections.emptyList());
            } else {
                Async.run(this, () -> enrollmentService.getSectionWaitlist(s.getSectionId()), waitlist::setItems);
            }
        });

        FlatButton add = UIStyles.primary("Add section");
        FlatButton edit = UIStyles.secondary("Edit / re-assign");
        FlatButton delete = UIStyles.danger("Delete section");
        FlatButton promote = UIStyles.secondary("Promote waitlist now");
        add.addActionListener(e -> {
            if (new SectionDialog(dashboard.getFrame(), session).showDialog()) {
                status("Section created");
                refresh();
            }
        });
        edit.addActionListener(e -> {
            Section s = sections.getSelected();
            if (s == null) {
                Dialogs.info(this, "Sections", "Select a section first.");
            } else if (new SectionDialog(dashboard.getFrame(), session, s).showDialog()) {
                status("Section " + s.getKey() + " updated");
                refresh();
            }
        });
        delete.addActionListener(e -> deleteSelected());
        promote.addActionListener(e -> promoteSelected());
        JPanel actions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        actions.add(promote);

        FlatButton forceEnroll = UIStyles.success("Enroll now (override capacity & queue)");
        FlatButton removeWait = UIStyles.danger("Remove from waitlist");
        forceEnroll.addActionListener(e -> forceEnrollSelected());
        removeWait.addActionListener(e -> removeWaitlistSelected());
        JPanel waitActions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        waitActions.add(forceEnroll);
        waitActions.add(removeWait);
        waitActions.add(UIStyles.muted("For exceptional cases only - skips the seat limit and queue order."));

        JPanel top = UIStyles.card("Sections this semester", sections);
        top.add(actions, BorderLayout.SOUTH);
        JPanel waitCard = UIStyles.card("Waitlist of selected section", waitlist);
        waitCard.add(waitActions, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, waitCard);
        split.setResizeWeight(0.65);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, courseService::getCurrentSections, list -> {
            sections.setItems(list);
            waitlist.setItems(Collections.emptyList());
            status(list.size() + " sections");
        });
    }

    private void deleteSelected() {
        Section s = sections.getSelected();
        if (s == null) {
            Dialogs.info(this, "Sections", "Select a section first.");
            return;
        }
        if (Dialogs.confirm(this, "Delete section", "Delete " + s.getKey() + "?\n"
                + "Sections with enrollments cannot be deleted.")) {
            Async.exec(this, () -> courseService.deleteSection(session, s.getSectionId()), () -> {
                status("Deleted " + s.getKey());
                refresh();
            });
        }
    }

    private void forceEnrollSelected() {
        WaitlistEntry w = waitlist.getSelected();
        if (w == null) {
            Dialogs.info(this, "Waitlist", "Select a waitlisted student first.");
            return;
        }
        if (Dialogs.confirm(this, "Override", "Enroll " + w.getStudentName() + " in " + w.getSectionKey()
                + " now, even if the section is full and others are ahead in the queue?")) {
            Async.run(this, () -> enrollmentService.enroll(session, w.getStudentId(), w.getSectionId(), true, true), r -> {
                Dialogs.info(this, "Enrolled", r.getMessage());
                refresh();
            });
        }
    }

    private void removeWaitlistSelected() {
        WaitlistEntry w = waitlist.getSelected();
        if (w == null) {
            Dialogs.info(this, "Waitlist", "Select a waitlisted student first.");
            return;
        }
        Async.exec(this, () -> enrollmentService.leaveWaitlist(session, w), () -> {
            status("Removed " + w.getStudentName() + " from the waitlist");
            refresh();
        });
    }

    private void promoteSelected() {
        Section s = sections.getSelected();
        if (s == null) {
            Dialogs.info(this, "Waitlist", "Select a section first.");
            return;
        }
        Async.run(this, () -> enrollmentService.promoteWaitlist(s.getSectionId()), names -> {
            Dialogs.info(this, "Waitlist", names.isEmpty() ? "No one could be promoted (no free seats or empty queue)."
                    : "Enrolled from waitlist: " + String.join(", ", names));
            refresh();
        });
    }
}
