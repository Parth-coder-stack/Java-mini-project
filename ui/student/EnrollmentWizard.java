package ui.student;

import model.Course;
import model.EligibilityReport;
import model.EligibilityReport.Check;
import model.PrerequisiteTask;
import model.Section;
import model.Student;
import model.Submission;
import service.CourseService;
import service.EnrollmentService;
import service.Session;
import service.StudentService;
import service.TaskService;
import ui.Async;
import ui.Browser;
import ui.Dialogs;
import ui.UIStyles;
import ui.common.CourseListPage;
import ui.components.DataTable;
import ui.components.FlatButton;
import ui.components.FormBuilder;
import ui.components.StatusIcon;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Step-by-step course registration:
 * 1 prerequisite courses  ->  2 prerequisite tasks (assignments / videos)  ->  3 confirm contact details
 * ->  4 choose a Theory or Lab section.
 * Tasks marked "Theory only" / "Lab only" are needed only for that kind of section; the
 * section table in step 4 shows which ones you are ready for. The service re-checks everything.
 */
public class EnrollmentWizard extends JDialog {

    private static final String[] STEPS = {"Prerequisite courses", "Prerequisite tasks", "Contact details",
            "Choose section"};

    private final Session session;
    private final Course course;
    private final EnrollmentService enrollmentService = new EnrollmentService();
    private final TaskService taskService = new TaskService();
    private final StudentService studentService = new StudentService();
    private final CourseService courseService = new CourseService();

    private final CardLayout cards = new CardLayout();
    private final JPanel stepHost = new JPanel(cards);
    private final List<JLabel> stepLabels = new ArrayList<>();
    private final FlatButton backButton = UIStyles.secondary("Back");
    private final FlatButton nextButton = UIStyles.primary("Next");
    private final JLabel footerMessage = UIStyles.label(" ", UIStyles.BOLD, UIStyles.DANGER);
    private int step;
    private EligibilityReport report;

    // step 1
    private final ChecklistPanel prereqList = new ChecklistPanel();
    // step 2
    private final JPanel taskList = UIStyles.clear(null);
    // step 3
    private final JLabel contactName = UIStyles.label(" ", UIStyles.BOLD, UIStyles.TEXT);
    private final JLabel contactEmail = UIStyles.label(" ", UIStyles.BOLD, UIStyles.TEXT);
    private final JTextField phoneField = UIStyles.textField(16);
    private final JTextField addressField = UIStyles.textField(30);
    private final JCheckBox confirmBox = new JCheckBox("I confirm that these contact details are correct");
    // step 4: "Theory"/"Lab" -> eligibility for that component
    private final Map<String, EligibilityReport> componentReports = new HashMap<>();
    private final DataTable<Section> sections = new DataTable<>(
            new String[]{"Type", "Section", "Slot", "Faculty", "Room", "Seats", "Your requirements"},
            s -> {
                Object[] base = CourseListPage.sectionRow(s);
                return new Object[]{base[1], base[2], base[3], base[4], base[5], base[6], readiness(s)};
            });
    private final JLabel resultLabel = UIStyles.label(" ", UIStyles.BOLD, UIStyles.SUCCESS);

    public EnrollmentWizard(JFrame owner, Session session, Course course) {
        super(owner, "Register for " + course.getCourseCode() + " - " + course.getTitle(), true);
        this.session = session;
        this.course = course;

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIStyles.CARD);
        content.add(buildStepBar(), BorderLayout.NORTH);
        stepHost.setBackground(UIStyles.CARD);
        stepHost.add(wrapStep("Step 1 - Have you completed the prerequisite courses?", UIStyles.scroll(prereqList)), "0");
        taskList.setLayout(new BoxLayout(taskList, BoxLayout.Y_AXIS));
        JPanel taskHolder = new JPanel(new BorderLayout());
        taskHolder.setBackground(UIStyles.CARD);
        taskHolder.add(taskList, BorderLayout.NORTH);
        stepHost.add(wrapStep("Step 2 - Finish the prerequisite tasks (assignments / videos)",
                noHorizontalScroll(UIStyles.scroll(taskHolder))), "1");
        stepHost.add(wrapStep("Step 3 - Check and confirm your contact details", buildContactStep()), "2");
        stepHost.add(wrapStep("Step 4 - Pick a section (do it twice if the course has Theory and Lab)",
                buildSectionStep()), "3");
        content.add(stepHost, BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(content);
        setSize(new Dimension(1000, 640));
        setLocationRelativeTo(owner);

        backButton.addActionListener(e -> goTo(step - 1));
        nextButton.addActionListener(e -> next());
        reloadReport(() -> goTo(0));
    }

    // ------------------------------------------------------------------ layout

    private JPanel buildStepBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        bar.setBackground(UIStyles.PRIMARY_LIGHT);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        for (int i = 0; i < STEPS.length; i++) {
            JLabel l = UIStyles.label((i + 1) + ".  " + STEPS[i], UIStyles.BODY, UIStyles.MUTED);
            stepLabels.add(l);
            bar.add(l);
        }
        return bar;
    }

    private static javax.swing.JScrollPane noHorizontalScroll(javax.swing.JScrollPane sp) {
        sp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private JPanel wrapStep(String title, Component body) {
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(UIStyles.CARD);
        p.setBorder(BorderFactory.createEmptyBorder(18, 22, 10, 22));
        p.add(UIStyles.title(title), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildContactStep() {
        confirmBox.setOpaque(false);
        return new FormBuilder()
                .row(UIStyles.muted("The university must be able to reach you before you are registered."))
                .row(FormBuilder.labeled("Name / PRN", contactName), FormBuilder.labeled("Email (login ID)", contactEmail))
                .field("Phone number (10 digits)", phoneField)
                .field("Address", addressField)
                .row(confirmBox)
                .build();
    }

    private JPanel buildSectionStep() {
        JPanel p = UIStyles.clear(new BorderLayout(0, 10));
        p.add(sections, BorderLayout.CENTER);
        p.add(resultLabel, BorderLayout.SOUTH);
        sections.onDoubleClick(s -> register());
        sections.setColumnWidths(55, 60, 150, 140, 60, 90, 330);
        return p;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UIStyles.CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIStyles.BORDER),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));
        FlatButton close = UIStyles.secondary("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(close);
        buttons.add(backButton);
        buttons.add(nextButton);
        footer.add(footerMessage, BorderLayout.CENTER);
        footer.add(buttons, BorderLayout.EAST);
        return footer;
    }

    // ------------------------------------------------------------------ navigation

    private void reloadReport(Runnable then) {
        int studentId = session.getStudent().getId();
        Async.run(this, () -> enrollmentService.checkEligibility(studentId, course.getCourseId()), r -> {
            report = r;
            then.run();
        });
    }

    private void goTo(int newStep) {
        step = Math.max(0, Math.min(STEPS.length - 1, newStep));
        for (int i = 0; i < stepLabels.size(); i++) {
            JLabel l = stepLabels.get(i);
            l.setFont(i == step ? UIStyles.BOLD : UIStyles.BODY);
            l.setForeground(i == step ? UIStyles.PRIMARY : i < step ? UIStyles.SUCCESS : UIStyles.MUTED);
        }
        footerMessage.setText(" ");
        backButton.setEnabled(step > 0);
        nextButton.setText(step == STEPS.length - 1 ? "Register in selected section" : "Next");
        switch (step) {
            case 0: showPrerequisites(); break;
            case 1: showTasks(); break;
            case 2: showContact(); break;
            default: showSections();
        }
        cards.show(stepHost, String.valueOf(step));
    }

    private void next() {
        switch (step) {
            case 0:
            case 1:
                goTo(step + 1);
                break;
            case 2:
                saveContact();
                break;
            default:
                register();
        }
    }

    // ------------------------------------------------------------------ step 1

    private void showPrerequisites() {
        List<Check> checks = new ArrayList<>();
        for (Check c : report.getChecks()) {
            if (c.getCategory().equals(EligibilityReport.PREREQUISITE) || c.getCategory().equals(EligibilityReport.STATUS)) {
                checks.add(c);
            }
        }
        prereqList.setChecks(checks, course.getCourseCode() + " has no prerequisites - you can continue.");
        boolean ok = report.isCategoryPassed(EligibilityReport.PREREQUISITE) && report.isCategoryPassed(EligibilityReport.STATUS);
        nextButton.setEnabled(ok);
        if (!ok) {
            footerMessage.setText("Complete the missing prerequisite(s) first - you cannot register yet.");
        }
    }

    // ------------------------------------------------------------------ step 2

    private void showTasks() {
        int studentId = session.getStudent().getId();
        Async.run(this, () -> {
            // task -> my submission (null if not done yet), in display order
            Map<PrerequisiteTask, Submission> result = new LinkedHashMap<>();
            Map<Integer, Submission> mine = taskService.getMySubmissions(studentId);
            for (PrerequisiteTask t : taskService.getForCourse(course.getCourseId())) {
                result.put(t, mine.get(t.getTaskId()));
            }
            return result;
        }, items -> {
            taskList.removeAll();
            if (items.isEmpty()) {
                JLabel none = UIStyles.label("This course has no prerequisite tasks - you can continue.",
                        UIStyles.BOLD, UIStyles.SUCCESS);
                none.setIcon(StatusIcon.OK);
                taskList.add(none);
            }
            for (Map.Entry<PrerequisiteTask, Submission> item : items.entrySet()) {
                taskList.add(taskCard(item.getKey(), item.getValue()));
                taskList.add(javax.swing.Box.createVerticalStrut(10));
            }
            taskList.revalidate();
            taskList.repaint();
            boolean ok = report.isCategoryPassed(EligibilityReport.TASK);
            nextButton.setEnabled(ok);
            footerMessage.setText(ok ? " " : "Finish the tasks marked \"Theory & Lab\" to continue.");
        });
    }

    /** One card per task: an answer box for assignments, Open/Watched buttons for videos. */
    private JPanel taskCard(PrerequisiteTask t, Submission sub) {
        JPanel card = UIStyles.card();
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean ok = sub != null && sub.satisfiesRequirement();
        String state = sub == null ? (t.isVideo() ? "Not watched" : "Not submitted")
                : sub.getStatus() + (sub.getRemarks() == null ? "" : " - " + sub.getRemarks());
        JLabel title = UIStyles.label(t.getTaskType() + ": " + t.getTitle() + "   [" + state + "]",
                UIStyles.H3, UIStyles.TEXT);
        title.setIcon(ok ? StatusIcon.OK : StatusIcon.FAIL);
        JPanel head = UIStyles.clear(new BorderLayout(0, 4));
        head.add(title, BorderLayout.NORTH);
        head.add(UIStyles.label("<html><div style='width:780px'>Needed for: <b>" + t.getAppliesToText()
                + "</b><br>" + (t.getDescription() == null ? "" : t.getDescription()) + "</div></html>",
                UIStyles.BODY, UIStyles.MUTED), BorderLayout.CENTER);
        card.add(head, BorderLayout.NORTH);

        if (t.isVideo() && !ok) {
            FlatButton open = UIStyles.primary("Open video");
            FlatButton watched = UIStyles.success("I have watched it");
            watched.setEnabled(false);
            open.addActionListener(e -> watched.setEnabled(Browser.open(this, t.getVideoUrl())));
            watched.addActionListener(e -> Async.exec(this, () -> taskService.markVideoWatched(session, t),
                    () -> reloadReport(this::showTasks)));
            JPanel row = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
            row.add(open);
            row.add(watched);
            card.add(row, BorderLayout.CENTER);
        } else if (!t.isVideo() && (sub == null || !Submission.APPROVED.equals(sub.getStatus()))) {
            JTextArea answer = UIStyles.textArea(4, 30);
            if (sub != null && sub.getContent() != null) {
                answer.setText(sub.getContent());
            }
            FlatButton submit = UIStyles.primary(sub == null ? "Submit answer" : "Resubmit");
            submit.addActionListener(e -> Async.exec(this,
                    () -> taskService.submitAnswer(session, t, answer.getText()),
                    () -> reloadReport(() -> {
                        Dialogs.info(this, "Submitted", "Your answer for \"" + t.getTitle() + "\" was submitted.");
                        showTasks();
                    })));
            JPanel bottom = UIStyles.clear(new BorderLayout(0, 8));
            bottom.add(UIStyles.scroll(answer), BorderLayout.CENTER);
            JPanel row = UIStyles.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.add(submit);
            bottom.add(row, BorderLayout.SOUTH);
            card.add(bottom, BorderLayout.CENTER);
        }
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    // ------------------------------------------------------------------ step 3

    private void showContact() {
        Student s = session.getStudent();
        contactName.setText(s.getName() + "  /  " + s.getPrn());
        contactEmail.setText(s.getEmail());
        phoneField.setText(s.getPhone());
        addressField.setText(s.getAddress());
        confirmBox.setSelected(false);
        nextButton.setEnabled(true);
    }

    private void saveContact() {
        if (!confirmBox.isSelected()) {
            footerMessage.setText("Tick the box to confirm your contact details.");
            return;
        }
        int id = session.getStudent().getId();
        Async.run(this, () -> studentService.updateContact(session, id, phoneField.getText(), addressField.getText()),
                updated -> reloadReport(() -> goTo(3)),
                error -> footerMessage.setText(error.getMessage()));
    }

    // ------------------------------------------------------------------ step 4

    /** "Ready", or what is still missing for this section's component (e.g. a Lab-only video). */
    private String readiness(Section s) {
        EligibilityReport r = componentReports.get(s.getComponentType());
        if (r == null) {
            return "";
        }
        List<String> missing = new ArrayList<>();
        for (Check c : r.getFailures("")) {
            missing.add(c.getLabel());
        }
        return missing.isEmpty() ? "Ready" : "Needs " + String.join(", ", missing);
    }

    private void showSections() {
        resultLabel.setText(" ");
        nextButton.setEnabled(true);
        int studentId = session.getStudent().getId();
        Async.run(this, () -> {
            List<Section> list = courseService.getSectionsForCourse(course.getCourseId());
            Map<String, EligibilityReport> reports = new HashMap<>();
            for (Section s : list) {
                if (!reports.containsKey(s.getComponentType())) {
                    reports.put(s.getComponentType(), enrollmentService.checkEligibility(
                            studentId, course.getCourseId(), s.getComponentType()));
                }
            }
            componentReports.clear();
            componentReports.putAll(reports);
            return list;
        }, list -> {
            sections.setItems(list);
            if (list.isEmpty()) {
                footerMessage.setText("No sections of this course are offered this semester.");
                nextButton.setEnabled(false);
            }
        });
    }

    private void register() {
        Section s = sections.getSelected();
        if (s == null) {
            footerMessage.setText("Select a section in the table first.");
            return;
        }
        footerMessage.setText(" ");
        int id = session.getStudent().getId();
        Async.run(this, () -> enrollmentService.enroll(session, id, s.getSectionId(), false), result -> {
            resultLabel.setForeground(result.isEnrolled() ? UIStyles.SUCCESS : UIStyles.WARNING);
            resultLabel.setIcon(result.isEnrolled() ? StatusIcon.OK : StatusIcon.WARN);
            resultLabel.setText("<html>" + result.getMessage() + "</html>");
            Dialogs.info(this, result.isEnrolled() ? "Registered" : "Added to waitlist", result.getMessage());
            showSections();
        });
    }
}
