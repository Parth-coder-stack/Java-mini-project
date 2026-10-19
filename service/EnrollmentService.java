package service;

import db.dao.TaskDAO;
import db.dao.CourseDAO;
import db.dao.EnrollmentDAO;
import db.dao.NotificationDAO;
import db.dao.SectionDAO;
import db.dao.StudentDAO;
import db.dao.WaitlistDAO;
import exception.AuthException;
import exception.EnrollmentException;
import exception.UCMSException;
import exception.ValidationException;
import model.PrerequisiteTask;
import model.Course;
import model.EligibilityReport;
import model.EligibilityReport.Check;
import model.Enrollment;
import model.Prerequisite;
import model.Section;
import model.Semester;
import model.Student;
import model.Submission;
import model.TimeSlot;
import model.WaitlistEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enrollment rules. A student is registered in a section only when ALL of these pass:
 * <ol>
 *   <li>every mandatory prerequisite course is completed with at least its minimum grade</li>
 *   <li>every prerequisite task (assignment / video) for that Theory or Lab component is done</li>
 *   <li>contact details (phone + address) are complete and confirmed by the student</li>
 *   <li>no timetable clash, no duplicate enrollment, credit limit not exceeded</li>
 * </ol>
 * If the section is full the student is put on the waitlist instead.
 */
public class EnrollmentService {

    public static final double MAX_CREDITS_PER_SEMESTER = 24;

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final SectionDAO sectionDAO = new SectionDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final TaskDAO taskDAO = new TaskDAO();
    private final WaitlistDAO waitlistDAO = new WaitlistDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** Outcome of an enroll request. */
    public static class Result {
        private final boolean enrolled;
        private final String message;

        Result(boolean enrolled, String message) {
            this.enrolled = enrolled;
            this.message = message;
        }

        public boolean isEnrolled() { return enrolled; }
        public String getMessage()  { return message; }
    }

    // ------------------------------------------------------------ eligibility

    /** Course-level checklist (tasks for Theory & Lab both; component-only tasks shown as info). */
    public EligibilityReport checkEligibility(int studentId, int courseId) throws UCMSException {
        return checkEligibility(studentId, courseId, null);
    }

    /**
     * Checklist for one component of a course.
     *
     * @param componentType "Theory" or "Lab" - only the tasks required for that component block;
     *                      null for the course as a whole
     */
    public EligibilityReport checkEligibility(int studentId, int courseId, String componentType)
            throws UCMSException {
        Student student = studentDAO.findById(studentId);
        Course course = courseDAO.findById(courseId);
        if (student == null || course == null) {
            throw new ValidationException("Student or course not found.");
        }
        List<Enrollment> history = enrollmentDAO.findByStudent(studentId);
        Map<Integer, Submission> submissions = taskDAO.findSubmissionsByStudent(studentId);
        return buildReport(student, course, componentType, history, submissions, taskDAO.findByCourse(courseId));
    }

    /** Checklists for every course in the catalog (used by the catalog table). */
    public Map<Integer, EligibilityReport> checkCatalog(int studentId, List<Course> catalog) throws UCMSException {
        Student student = studentDAO.findById(studentId);
        List<Enrollment> history = enrollmentDAO.findByStudent(studentId);
        Map<Integer, Submission> submissions = taskDAO.findSubmissionsByStudent(studentId);
        // courseId -> its tasks, loaded with one query instead of one per course
        Map<Integer, List<PrerequisiteTask>> tasksByCourse = new HashMap<>();
        for (PrerequisiteTask t : taskDAO.findAll()) {
            tasksByCourse.computeIfAbsent(t.getCourseId(), k -> new ArrayList<>()).add(t);
        }
        Map<Integer, EligibilityReport> reports = new HashMap<>();
        for (Course c : catalog) {
            reports.put(c.getCourseId(), buildReport(student, c, null, history, submissions,
                    tasksByCourse.getOrDefault(c.getCourseId(), new ArrayList<>())));
        }
        return reports;
    }

    private EligibilityReport buildReport(Student student, Course course, String componentType,
                                          List<Enrollment> history, Map<Integer, Submission> submissions,
                                          List<PrerequisiteTask> tasks) {
        EligibilityReport report = new EligibilityReport(course);
        Map<Integer, String> bestGrades = bestGrades(history);
        Set<Integer> inProgress = new HashSet<>();
        for (Enrollment e : history) {
            if (e.isActive()) {
                inProgress.add(e.getCourseId());
            }
        }

        if (GradeScale.isPass(bestGrades.get(course.getCourseId()))) {
            report.add(new Check(EligibilityReport.STATUS, "Already completed", false, true,
                    "You passed " + course.getCourseCode() + " with grade " + bestGrades.get(course.getCourseId())));
        } else if (inProgress.contains(course.getCourseId())) {
            report.setEnrolledNow(true);
            report.add(new Check(EligibilityReport.STATUS, "Enrolled this semester", true, false,
                    "You are taking this course now - see My Courses"));
        }

        for (Prerequisite p : course.getPrerequisites()) {
            String grade = bestGrades.get(p.getRequiredCourseId());
            boolean ok = GradeScale.meets(grade, p.getMinGrade());
            String detail;
            if (ok) {
                detail = "Completed with grade " + grade;
            } else if (grade != null) {
                detail = "Your grade " + grade + " is below the required " + p.getMinGrade();
            } else if (inProgress.contains(p.getRequiredCourseId())) {
                detail = "In progress this semester - not completed yet";
            } else {
                detail = "Not taken yet (need grade " + p.getMinGrade() + " or better)";
            }
            if (!p.isMandatory() && !ok) {
                detail += "  [recommended only]";
            }
            report.add(new Check(EligibilityReport.PREREQUISITE, p.getRequiredCode(), ok, p.isMandatory(), detail));
        }

        // Prerequisite tasks: assignments to submit and videos to watch.
        for (PrerequisiteTask t : tasks) {
            boolean required;
            if (componentType == null) {
                required = PrerequisiteTask.BOTH.equals(t.getAppliesTo());
            } else if (t.appliesTo(componentType)) {
                required = true;
            } else {
                continue;   // e.g. a Lab-only video does not matter for a Theory section
            }
            Submission sub = submissions.get(t.getTaskId());
            boolean ok = sub != null && sub.satisfiesRequirement();
            String detail;
            if (sub == null) {
                detail = t.isVideo() ? "Not watched yet" : "Not submitted";
            } else if (Submission.REJECTED.equals(sub.getStatus())) {
                detail = "Rejected" + (sub.getRemarks() == null ? "" : ": " + sub.getRemarks()) + " - please resubmit";
            } else if (Submission.APPROVED.equals(sub.getStatus())) {
                detail = "Approved by faculty";
            } else if (Submission.WATCHED.equals(sub.getStatus())) {
                detail = "Watched";
            } else {
                detail = "Submitted - awaiting faculty review";
            }
            detail += "   (" + t.getAppliesToText() + ")";
            report.add(new Check(EligibilityReport.TASK, t.getTaskType() + ": " + t.getTitle(), ok, required, detail));
        }

        boolean contactOk = student.isContactVerified() && Validator.isValidPhone(student.getPhone())
                && student.getAddress() != null && !student.getAddress().trim().isEmpty();
        report.add(new Check(EligibilityReport.CONTACT, "Contact details", contactOk, true,
                contactOk ? "Verified: " + student.getPhone() + ", " + student.getEmail()
                        : "Please confirm your phone number and address"));
        return report;
    }

    /** courseId -> best grade achieved in completed enrollments. */
    private static Map<Integer, String> bestGrades(List<Enrollment> history) {
        Map<Integer, String> best = new HashMap<>();
        for (Enrollment e : history) {
            if ("Completed".equals(e.getStatus()) && e.getGrade() != null) {
                best.merge(e.getCourseId(), e.getGrade(), GradeScale::best);
            }
        }
        return best;
    }

    // ------------------------------------------------------------ enroll / drop

    /**
     * Enrolls a student in a section, or waitlists them if it is full.
     * Only the tasks for the section's component (Theory or Lab) are required.
     * Admins may set {@code override} to skip prerequisite/task/contact checks.
     *
     * @throws EnrollmentException listing every reason the request was refused
     */
    public Result enroll(Session session, int studentId, int sectionId, boolean override) throws UCMSException {
        return enroll(session, studentId, sectionId, override, false);
    }

    /**
     * FR-08: enroll into a free seat, or place on the waitlist with the next queue position.
     *
     * @param ignoreCapacity admin-only "exceptional case" override of the seat limit (and waitlist order)
     */
    public Result enroll(Session session, int studentId, int sectionId, boolean override, boolean ignoreCapacity)
            throws UCMSException {
        if (session.isStudent() && session.getStudent().getId() != studentId) {
            throw new AuthException("Students can only register themselves.");
        }
        if ((override || ignoreCapacity) && !session.isAdmin()) {
            throw new AuthException("Only the administrator can override prerequisites or seat capacity.");
        }
        Section section = sectionDAO.findById(sectionId);
        Semester current = sectionDAO.findCurrentSemester();
        if (section == null || current == null || !section.getSemesterName().equals(current.getName())) {
            throw new EnrollmentException("This section is not open for registration this semester.");
        }

        List<String> problems = new ArrayList<>();
        if (!override) {
            EligibilityReport report = checkEligibility(studentId, section.getCourseId(), section.getComponentType());
            for (Check c : report.getFailures("")) {
                problems.add(c.getCategory() + " - " + c.getLabel() + ": " + c.getDetail());
            }
        }
        problems.addAll(scheduleProblems(studentId, section));
        if (waitlistDAO.isWaiting(studentId, sectionId) && !ignoreCapacity) {
            problems.add("You are already on the waitlist for this section.");
        }
        if (!problems.isEmpty()) {
            notificationDAO.notifyStudent(studentId, "Registration failed for " + section.getKey() + ": "
                    + String.join("; ", problems));
            throw new EnrollmentException(problems);
        }

        if (enrollmentDAO.enrollIfSeatFree(studentId, sectionId, override, ignoreCapacity)) {
            String msg = "Registered for " + section.getCourseCode() + " " + section.getComponentType()
                    + " section " + section.getSectionNumber() + " (" + section.getSlot() + ")"
                    + (ignoreCapacity && section.isFull() ? " - seat limit overridden by admin." : ".");
            notificationDAO.notifyStudent(studentId, "Registration confirmed: " + msg);
            return new Result(true, msg);
        }
        int position = waitlistDAO.add(studentId, sectionId);
        String msg = "The section is full. You have been added to the waitlist at position "
                + position + ". You will be enrolled automatically when a seat frees up.";
        notificationDAO.notifyStudent(studentId, section.getKey() + " is full - you are #" + position + " on the waitlist.");
        return new Result(false, msg);
    }

    /**
     * Duplicate enrollment (FR-09), student timetable clash and credit-limit checks.
     * FR-09: a student may not hold two active enrollments in different sections of the
     * same course in the same semester. Theory and Lab are separate parts of a course, so
     * holding CS201 Theory A + CS201 Lab A is fine, but CS201 Theory A + CS201 Theory B is not.
     * (Active enrollments are always in the current semester.)
     */
    private List<String> scheduleProblems(int studentId, Section section) throws UCMSException {
        List<String> problems = new ArrayList<>();
        TimeSlot newSlot = TimeSlot.parse(section.getSlot());
        double credits = section.getCredits();
        for (Enrollment e : enrollmentDAO.findByStudent(studentId)) {
            if (!e.isActive()) {
                continue;
            }
            if (e.getSectionId() == section.getSectionId()) {
                problems.add("You are already enrolled in this section.");
            } else if (e.getCourseId() == section.getCourseId()
                    && e.getComponentType().equals(section.getComponentType())) {
                problems.add("Duplicate course (FR-09): you already hold an active enrollment in "
                        + e.getCourseCode() + " " + e.getComponentType() + " section " + e.getSectionNumber()
                        + " this semester. Drop it first to switch sections.");
            } else if (TimeSlot.parse(e.getSlot()).overlaps(newSlot)) {
                problems.add("Timetable clash with " + e.getCourseCode() + " " + e.getComponentType()
                        + " (" + e.getSlot() + ").");
            }
            credits += e.getCredits();
        }
        if (credits > MAX_CREDITS_PER_SEMESTER) {
            problems.add("This would take you to " + credits + " credits (limit " + MAX_CREDITS_PER_SEMESTER + ").");
        }
        return problems;
    }

    /** Drops an enrollment and immediately offers the seat to the waitlist. */
    public String drop(Session session, int enrollmentId) throws UCMSException {
        Enrollment e = enrollmentDAO.findById(enrollmentId);
        if (e == null || !e.isActive()) {
            throw new ValidationException("Only active enrollments can be dropped.");
        }
        if (session.isStudent()) {
            if (e.getStudentId() != session.getStudent().getId()) {
                throw new AuthException("You can only drop your own courses.");
            }
            Semester sem = sectionDAO.findCurrentSemester();
            if (sem != null && LocalDate.now().isAfter(sem.getDropDeadline())) {
                throw new ValidationException("The drop deadline (" + sem.getDropDeadline()
                        + ") has passed. Contact the administrator.");
            }
        } else if (!session.isAdmin()) {
            throw new AuthException("Only students or the administrator can drop courses.");
        }
        enrollmentDAO.updateStatus(enrollmentId, "Dropped");
        notificationDAO.notifyStudent(e.getStudentId(), "You were dropped from " + e.getSectionKey()
                + (session.isAdmin() ? " by the administrator." : "."));
        List<String> promoted = promoteWaitlist(e.getSectionId());
        String msg = "Dropped " + e.getCourseCode() + " " + e.getComponentType() + ".";
        if (!promoted.isEmpty()) {
            msg += " Seat given to " + String.join(", ", promoted) + " from the waitlist.";
        }
        return msg;
    }

    // ------------------------------------------------------------ waitlist

    public List<WaitlistEntry> getWaitlist(int studentId) throws UCMSException {
        return waitlistDAO.findWaitingByStudent(studentId);
    }

    public List<WaitlistEntry> getSectionWaitlist(int sectionId) throws UCMSException {
        return waitlistDAO.findWaitingBySection(sectionId);
    }

    public void leaveWaitlist(Session session, WaitlistEntry entry) throws UCMSException {
        if (session.isStudent() && entry.getStudentId() != session.getStudent().getId()) {
            throw new AuthException("You can only leave your own waitlist entries.");
        }
        waitlistDAO.updateStatus(entry.getWaitlistId(), "Removed");
    }

    /**
     * Moves waiting students into free seats, in queue order. A student whose
     * timetable now clashes is skipped (removed from the queue).
     * Synchronized because both the UI and the background daemon call it.
     *
     * @return names of promoted students
     */
    public synchronized List<String> promoteWaitlist(int sectionId) throws UCMSException {
        List<String> promoted = new ArrayList<>();
        Section section = sectionDAO.findById(sectionId);
        if (section == null) {
            return promoted;
        }
        for (WaitlistEntry w : waitlistDAO.findWaitingBySection(sectionId)) {
            if (!scheduleProblems(w.getStudentId(), section).isEmpty()) {
                waitlistDAO.updateStatus(w.getWaitlistId(), "Removed");
                continue;
            }
            // promotion notification is written by the trg_waitlist_promoted trigger
            if (!enrollmentDAO.enrollIfSeatFree(w.getStudentId(), sectionId, false, false)) {
                break;
            }
            promoted.add(w.getStudentName());
        }
        return promoted;
    }

    /** Called periodically by {@link WaitlistDaemon}. */
    public List<String> promoteAll() throws UCMSException {
        List<String> messages = new ArrayList<>();
        for (int sectionId : waitlistDAO.findSectionsWithQueue()) {
            for (String name : promoteWaitlist(sectionId)) {
                messages.add(name + " was moved from the waitlist into section #" + sectionId);
            }
        }
        return messages;
    }

    // ------------------------------------------------------------ queries & grading

    public List<Enrollment> getEnrollments(int studentId) throws UCMSException {
        return enrollmentDAO.findByStudent(studentId);
    }

    public List<Enrollment> getRoster(int sectionId) throws UCMSException {
        return enrollmentDAO.findRoster(sectionId);
    }

    public List<Enrollment> searchCurrentEnrollments(String keyword) throws UCMSException {
        return enrollmentDAO.findCurrent(keyword == null ? "" : keyword.trim());
    }

    /**
     * Faculty (for their own sections) or admin enters or updates a final grade.
     * A database trigger logs it in audit_log and notifies the student.
     */
    public void saveGrade(Session session, Enrollment e, String grade) throws UCMSException {
        if (!GradeScale.isValid(grade)) {
            throw new ValidationException("Choose a grade from " + GradeScale.grades());
        }
        if (session.isFaculty()) {
            Section sec = sectionDAO.findById(e.getSectionId());
            if (sec.getFacultyId() != session.getFaculty().getId()) {
                throw new AuthException("You can only grade your own sections.");
            }
        } else if (!session.isAdmin()) {
            throw new AuthException("Students cannot enter grades.");
        }
        enrollmentDAO.saveGrade(e, grade, session.getAccount().getEmail());
    }
}
