package ui.student;

import model.PrerequisiteTask;
import model.Submission;
import service.TaskService;
import ui.Async;
import ui.BaseDashboard;
import ui.Browser;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

/**
 * Every prerequisite task (assignments and videos) with the student's status.
 * Assignments are answered here; videos are opened in the browser and then marked as watched.
 */
public class TasksPage extends Page {

    private final TaskService taskService = new TaskService();
    private final Map<Integer, Submission> mine = new HashMap<>();
    private final DataTable<PrerequisiteTask> table;

    private final JLabel title = UIStyles.heading("Select a task");
    private final JLabel description = UIStyles.muted(" ");
    private final CardLayout actionCards = new CardLayout();
    private final JPanel actionHost = UIStyles.clear(actionCards);
    private final JTextArea answer = UIStyles.textArea(6, 40);
    private final FlatButton submit = UIStyles.primary("Submit answer");
    private final FlatButton openVideo = UIStyles.primary("Open video");
    private final FlatButton watched = UIStyles.success("I have watched it");

    public TasksPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<PrerequisiteTask>(new String[]{"Course", "Type", "Needed for", "Task", "Status", "Remarks"},
                t -> {
                    Submission s = mine.get(t.getTaskId());
                    return new Object[]{t.getCourseCode(), t.getTaskType(), t.getAppliesToText(), t.getTitle(),
                            s == null ? (t.isVideo() ? "Not watched" : "Not submitted") : s.getStatus(),
                            s == null || s.getRemarks() == null ? "" : s.getRemarks()};
                }).withFilter("Filter:");
        table.setColumnWidths(70, 90, 110, 260, 110, 220);
        table.onSelect(this::show);

        submit.addActionListener(e -> doSubmit());
        openVideo.addActionListener(e -> {
            PrerequisiteTask t = table.getSelected();
            if (t != null && Browser.open(this, t.getVideoUrl())) {
                watched.setEnabled(true);   // can only confirm after opening the video
            }
        });
        watched.addActionListener(e -> doWatched());

        JPanel assignmentPanel = UIStyles.clear(new BorderLayout(0, 8));
        assignmentPanel.add(UIStyles.scroll(answer), BorderLayout.CENTER);
        JPanel submitRow = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        submitRow.add(submit);
        assignmentPanel.add(submitRow, BorderLayout.SOUTH);

        JPanel videoPanel = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        videoPanel.add(openVideo);
        videoPanel.add(watched);
        videoPanel.add(UIStyles.muted("Open the video first, then confirm you watched it."));

        actionHost.add(UIStyles.clear(new BorderLayout()), "none");
        actionHost.add(assignmentPanel, PrerequisiteTask.ASSIGNMENT);
        actionHost.add(videoPanel, PrerequisiteTask.VIDEO);

        JPanel detail = UIStyles.card();
        JPanel head = UIStyles.clear(new BorderLayout(0, 4));
        head.add(title, BorderLayout.NORTH);
        head.add(description, BorderLayout.CENTER);
        detail.add(head, BorderLayout.NORTH);
        detail.add(actionHost, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                UIStyles.card("Tasks you must finish before registering", table), detail);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    private void show(PrerequisiteTask t) {
        if (t == null) {
            title.setText("Select a task");
            description.setText(" ");
            actionCards.show(actionHost, "none");
            return;
        }
        Submission s = mine.get(t.getTaskId());
        title.setText(t.getCourseCode() + " - " + t.getTaskType() + ": " + t.getTitle()
                + "   (needed for " + t.getAppliesToText() + ")");
        description.setText("<html><div style='width:800px'>" + (t.getDescription() == null ? "" : t.getDescription())
                + "</div></html>");
        if (t.isVideo()) {
            boolean done = s != null && Submission.WATCHED.equals(s.getStatus());
            watched.setEnabled(false);
            watched.setText(done ? "Already watched" : "I have watched it");
            actionCards.show(actionHost, PrerequisiteTask.VIDEO);
        } else {
            boolean approved = s != null && Submission.APPROVED.equals(s.getStatus());
            answer.setText(s == null || s.getContent() == null ? "" : s.getContent());
            answer.setEditable(!approved);
            submit.setEnabled(!approved);
            submit.setText(s == null ? "Submit answer" : approved ? "Approved" : "Resubmit");
            actionCards.show(actionHost, PrerequisiteTask.ASSIGNMENT);
        }
    }

    private void doSubmit() {
        PrerequisiteTask t = table.getSelected();
        if (t != null) {
            Async.exec(this, () -> taskService.submitAnswer(session, t, answer.getText()), () -> {
                Dialogs.info(this, "Submitted", "Your answer was submitted for faculty review.");
                refresh();
            });
        }
    }

    private void doWatched() {
        PrerequisiteTask t = table.getSelected();
        if (t != null) {
            Async.exec(this, () -> taskService.markVideoWatched(session, t), () -> {
                status("Marked \"" + t.getTitle() + "\" as watched");
                refresh();
            });
        }
    }

    @Override
    public void refresh() {
        int id = session.getStudent().getId();
        Async.run(this, () -> {
            Map<Integer, Submission> subs = taskService.getMySubmissions(id);
            mine.clear();
            mine.putAll(subs);
            return taskService.getAll();
        }, list -> {
            table.setItems(list);
            show(null);
            status(list.size() + " prerequisite task(s)");
        });
    }
}
