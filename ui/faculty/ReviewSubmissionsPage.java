package ui.faculty;

import model.Submission;
import service.TaskService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;

/** Faculty approve or reject answers to prerequisite assignments of their courses (videos need no review). */
public class ReviewSubmissionsPage extends Page {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm");

    private final TaskService taskService = new TaskService();
    private final DataTable<Submission> table;
    private final JLabel heading = UIStyles.heading("Select a submission");
    private final JTextArea content = UIStyles.textArea(8, 40);
    private final JTextField remarks = UIStyles.textField(40);
    private final FlatButton approve = UIStyles.success("Approve");
    private final FlatButton reject = UIStyles.danger("Reject");

    public ReviewSubmissionsPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<Submission>(new String[]{"Course", "Task", "PRN", "Student", "Submitted", "Status"},
                s -> new Object[]{s.getCourseCode(), s.getTaskTitle(), s.getPrn(), s.getStudentName(),
                        s.getSubmittedAt() == null ? "" : s.getSubmittedAt().format(WHEN), s.getStatus()})
                .withFilter("Filter:");
        table.setColumnWidths(70, 220, 110, 160, 140, 100);
        table.onSelect(this::show);
        content.setEditable(false);
        approve.addActionListener(e -> review(true));
        reject.addActionListener(e -> review(false));

        JPanel detail = UIStyles.card();
        detail.add(heading, BorderLayout.NORTH);
        detail.add(UIStyles.scroll(content), BorderLayout.CENTER);
        JPanel actions = UIStyles.clear(new BorderLayout(8, 0));
        actions.add(UIStyles.label("Remarks", UIStyles.BOLD, UIStyles.TEXT), BorderLayout.WEST);
        actions.add(remarks, BorderLayout.CENTER);
        JPanel buttons = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(reject);
        buttons.add(approve);
        actions.add(buttons, BorderLayout.EAST);
        detail.add(actions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UIStyles.card("Submissions for your courses", table), detail);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
        show(null);
    }

    private void show(Submission s) {
        approve.setEnabled(s != null);
        reject.setEnabled(s != null);
        if (s == null) {
            heading.setText("Select a submission to review");
            content.setText("");
            remarks.setText("");
            return;
        }
        heading.setText(s.getStudentName() + " (" + s.getPrn() + ") - " + s.getTaskTitle()
                + "  [" + s.getStatus() + "]");
        content.setText(s.getContent());
        remarks.setText(s.getRemarks() == null ? "" : s.getRemarks());
    }

    private void review(boolean ok) {
        Submission s = table.getSelected();
        if (s == null) {
            return;
        }
        Async.exec(this, () -> taskService.review(session, s, ok, remarks.getText()), () -> {
            status((ok ? "Approved " : "Rejected ") + s.getStudentName() + "'s submission");
            refresh();
        });
    }

    @Override
    public void refresh() {
        Async.run(this, () -> taskService.getSubmissionsForFaculty(session.getFaculty().getId()), list -> {
            table.setItems(list);
            show(null);
            long pending = list.stream().filter(x -> Submission.SUBMITTED.equals(x.getStatus())).count();
            status(pending + " submission(s) waiting for review");
        });
    }
}
