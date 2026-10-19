package ui.admin;

import model.PrerequisiteTask;
import model.Course;
import model.Prerequisite;
import service.TaskService;
import service.CourseService;
import service.StudentService;
import ui.Async;
import ui.BaseDashboard;
import ui.Dialogs;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;
import ui.components.FlatButton;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Admin CRUD for courses, their prerequisite courses and their prerequisite tasks (assignments / videos). */
public class CourseManagementPage extends Page {

    private final CourseService courseService = new CourseService();
    private final TaskService taskService = new TaskService();
    private final StudentService studentService = new StudentService();

    private final DataTable<Course> courses;
    private final DataTable<Prerequisite> prereqs;
    private final DataTable<PrerequisiteTask> tasks;
    private final JComboBox<Course> requiredBox = new JComboBox<>();
    private final JComboBox<String> minGradeBox = new JComboBox<>(new String[]{"D", "C", "C+", "B", "B+", "A", "A+"});
    private final JCheckBox mandatoryBox = new JCheckBox("Mandatory", true);
    private final JComboBox<String> taskType = new JComboBox<>(new String[]{PrerequisiteTask.ASSIGNMENT, PrerequisiteTask.VIDEO});
    private final JComboBox<String> taskFor = new JComboBox<>(new String[]{"Both", "Theory", "Lab"});
    private final JTextField taskTitle = UIStyles.textField(14);
    private final JTextField taskDetail = UIStyles.textField(28);
    private final JLabel taskDetailLabel = UIStyles.muted("Question");
    private final List<PrerequisiteTask> allTasks = new ArrayList<>();

    public CourseManagementPage(BaseDashboard dashboard) {
        super(dashboard);
        courses = new DataTable<Course>(new String[]{"Code", "Title", "Dept", "Credits", "Prerequisites", "Description"},
                c -> new Object[]{c.getCourseCode(), c.getTitle(), c.getDeptName(), c.getCredits(),
                        c.getPrerequisiteText(), c.getDescription()}).withFilter("Filter:");
        courses.setColumnWidths(70, 220, 50, 60, 220, 260);
        prereqs = new DataTable<>(new String[]{"Required course", "Title", "Min grade", "Type"},
                p -> new Object[]{p.getRequiredCode(), p.getRequiredTitle(), p.getMinGrade(),
                        p.isMandatory() ? "Mandatory" : "Recommended"});
        tasks = new DataTable<>(new String[]{"Type", "Needed for", "Title", "Question / video link"},
                t -> new Object[]{t.getTaskType(), t.getAppliesToText(), t.getTitle(),
                        t.isVideo() ? t.getVideoUrl() : t.getDescription()});
        courses.onSelect(this::showCourse);
        mandatoryBox.setOpaque(false);

        FlatButton add = UIStyles.primary("Add course");
        FlatButton edit = UIStyles.secondary("Edit");
        FlatButton delete = UIStyles.danger("Delete");
        add.addActionListener(e -> openDialog(null));
        edit.addActionListener(e -> {
            Course c = selectedCourse();
            if (c != null) {
                openDialog(c);
            }
        });
        delete.addActionListener(e -> deleteCourse());
        JPanel courseActions = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        courseActions.add(add);
        courseActions.add(edit);
        courseActions.add(delete);
        JPanel courseCard = UIStyles.card("Courses", courses);
        courseCard.add(courseActions, BorderLayout.SOUTH);

        FlatButton addPrereq = UIStyles.primary("Add");
        FlatButton removePrereq = UIStyles.danger("Remove selected");
        addPrereq.addActionListener(e -> addPrerequisite());
        removePrereq.addActionListener(e -> removePrerequisite());
        JPanel prereqInputs = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        prereqInputs.add(UIStyles.muted("Requires"));
        prereqInputs.add(requiredBox);
        prereqInputs.add(UIStyles.muted("min grade"));
        prereqInputs.add(minGradeBox);
        prereqInputs.add(mandatoryBox);
        JPanel prereqButtons = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        prereqButtons.add(addPrereq);
        prereqButtons.add(removePrereq);
        JPanel prereqActions = UIStyles.clear(new GridLayout(2, 1, 0, 8));
        prereqActions.add(prereqInputs);
        prereqActions.add(prereqButtons);
        JPanel prereqCard = UIStyles.card("Prerequisites of selected course", prereqs);
        prereqCard.add(prereqActions, BorderLayout.SOUTH);

        FlatButton addTask = UIStyles.primary("Add");
        FlatButton removeTask = UIStyles.danger("Delete selected");
        addTask.addActionListener(e -> addTask());
        removeTask.addActionListener(e -> deleteTask());
        taskType.addActionListener(e -> taskDetailLabel.setText(
                PrerequisiteTask.VIDEO.equals(taskType.getSelectedItem()) ? "Video link" : "Question"));
        JPanel taskRow1 = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        taskRow1.add(taskType);
        taskRow1.add(UIStyles.muted("for"));
        taskRow1.add(taskFor);
        taskRow1.add(UIStyles.muted("Title"));
        taskRow1.add(taskTitle);
        JPanel taskRow2 = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        taskRow2.add(taskDetailLabel);
        taskRow2.add(taskDetail);
        JPanel taskRow3 = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 6, 0));
        taskRow3.add(addTask);
        taskRow3.add(removeTask);
        JPanel taskActions = UIStyles.clear(new GridLayout(3, 1, 0, 6));
        taskActions.add(taskRow1);
        taskActions.add(taskRow2);
        taskActions.add(taskRow3);
        JPanel taskCard = UIStyles.card("Prerequisite tasks of selected course (assignments / videos)", tasks);
        taskCard.add(taskActions, BorderLayout.SOUTH);

        JPanel lower = UIStyles.clear(new GridLayout(1, 2, 16, 0));
        lower.add(prereqCard);
        lower.add(taskCard);
        requiredBox.setPreferredSize(new java.awt.Dimension(170, 32));
        taskType.setPreferredSize(new java.awt.Dimension(120, 32));
        taskFor.setPreferredSize(new java.awt.Dimension(90, 32));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, courseCard, lower);
        split.setResizeWeight(0.45);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> {
            allTasks.clear();
            allTasks.addAll(taskService.getAll());
            return courseService.getCatalog();
        }, list -> {
            courses.setItems(list);
            requiredBox.removeAllItems();
            for (Course c : list) {
                requiredBox.addItem(c);
            }
            showCourse(null);
            status(list.size() + " courses");
        });
    }

    private void showCourse(Course c) {
        if (c == null) {
            prereqs.setItems(Collections.emptyList());
            tasks.setItems(Collections.emptyList());
            return;
        }
        prereqs.setItems(c.getPrerequisites());
        List<PrerequisiteTask> mine = new ArrayList<>();
        for (PrerequisiteTask t : allTasks) {
            if (t.getCourseId() == c.getCourseId()) {
                mine.add(t);
            }
        }
        tasks.setItems(mine);
    }

    private Course selectedCourse() {
        Course c = courses.getSelected();
        if (c == null) {
            Dialogs.info(this, "Courses", "Select a course first.");
        }
        return c;
    }

    private void openDialog(Course existing) {
        Async.run(this, studentService::getDepartments, depts -> {
            if (new CourseDialog(dashboard.getFrame(), session, existing, depts).showDialog()) {
                status(existing == null ? "Course added" : "Course updated");
                refresh();
            }
        });
    }

    private void deleteCourse() {
        Course c = selectedCourse();
        if (c != null && Dialogs.confirm(this, "Delete course", "Delete " + c + "?\n"
                + "Courses that already have sections cannot be deleted.")) {
            Async.exec(this, () -> courseService.deleteCourse(session, c.getCourseId()), () -> {
                status("Deleted " + c.getCourseCode());
                refresh();
            });
        }
    }

    private void addPrerequisite() {
        Course c = selectedCourse();
        Course required = (Course) requiredBox.getSelectedItem();
        if (c == null || required == null) {
            return;
        }
        Async.exec(this, () -> courseService.addPrerequisite(session, c.getCourseId(), required.getCourseId(),
                (String) minGradeBox.getSelectedItem(), mandatoryBox.isSelected()), () -> {
            status(c.getCourseCode() + " now requires " + required.getCourseCode());
            refresh();
        });
    }

    private void removePrerequisite() {
        Prerequisite p = prereqs.getSelected();
        if (p == null) {
            Dialogs.info(this, "Prerequisites", "Select a prerequisite first.");
            return;
        }
        Async.exec(this, () -> courseService.removePrerequisite(session, p), () -> {
            status("Removed prerequisite " + p.getRequiredCode());
            refresh();
        });
    }

    private void addTask() {
        Course c = selectedCourse();
        if (c == null) {
            return;
        }
        String type = (String) taskType.getSelectedItem();
        boolean video = PrerequisiteTask.VIDEO.equals(type);
        String detail = taskDetail.getText();
        String description = video ? "Watch: " + taskTitle.getText().trim() : detail;
        Async.exec(this, () -> taskService.create(session, c.getCourseId(), type, (String) taskFor.getSelectedItem(),
                taskTitle.getText(), description, video ? detail : null), () -> {
            taskTitle.setText("");
            taskDetail.setText("");
            status(type + " added as a prerequisite of " + c.getCourseCode());
            refresh();
        });
    }

    private void deleteTask() {
        PrerequisiteTask t = tasks.getSelected();
        if (t == null) {
            Dialogs.info(this, "Prerequisite tasks", "Select a task first.");
            return;
        }
        if (Dialogs.confirm(this, "Delete task", "Delete \"" + t.getTitle() + "\" and all students' progress on it?")) {
            Async.exec(this, () -> taskService.delete(session, t.getTaskId()), () -> {
                status("Deleted " + t.getTaskType().toLowerCase() + " " + t.getTitle());
                refresh();
            });
        }
    }
}
