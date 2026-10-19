package service;

import db.dao.NotificationDAO;
import db.dao.TaskDAO;
import exception.AuthException;
import exception.UCMSException;
import exception.ValidationException;
import model.PrerequisiteTask;
import model.Submission;

import java.util.List;
import java.util.Map;

/**
 * Prerequisite tasks (assignments and videos): students complete them,
 * faculty review assignment answers, the admin creates and deletes them.
 */
public class TaskService {

    public static final int MIN_ANSWER_LENGTH = 20;

    private final TaskDAO taskDAO = new TaskDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public List<PrerequisiteTask> getAll() throws UCMSException {
        return taskDAO.findAll();
    }

    public List<PrerequisiteTask> getForCourse(int courseId) throws UCMSException {
        return taskDAO.findByCourse(courseId);
    }

    public Map<Integer, Submission> getMySubmissions(int studentId) throws UCMSException {
        return taskDAO.findSubmissionsByStudent(studentId);
    }

    public void submitAnswer(Session session, PrerequisiteTask task, String content) throws UCMSException {
        requireStudent(session);
        if (task.isVideo()) {
            throw new ValidationException("This task is a video - open it and mark it as watched.");
        }
        String text = Validator.required(content, "Your answer");
        if (text.length() < MIN_ANSWER_LENGTH) {
            throw new ValidationException("Your answer is too short (at least " + MIN_ANSWER_LENGTH + " characters).");
        }
        taskDAO.submitAnswer(task.getTaskId(), session.getStudent().getId(), text);
        notificationDAO.notifyFacultyOfCourse(task.getCourseId(), session.getPerson().getName()
                + " submitted \"" + task.getTitle() + "\" (" + task.getCourseCode() + ") - please review.");
    }

    public void markVideoWatched(Session session, PrerequisiteTask task) throws UCMSException {
        requireStudent(session);
        if (!task.isVideo()) {
            throw new ValidationException("Only video tasks can be marked as watched.");
        }
        taskDAO.markWatched(task.getTaskId(), session.getStudent().getId());
    }

    public List<Submission> getSubmissionsForFaculty(int facultyId) throws UCMSException {
        return taskDAO.findSubmissionsForFaculty(facultyId);
    }

    public void review(Session session, Submission sub, boolean approve, String remarks) throws UCMSException {
        if (!session.isFaculty() && !session.isAdmin()) {
            throw new AuthException("Only faculty can review submissions.");
        }
        if (!approve && (remarks == null || remarks.trim().isEmpty())) {
            throw new ValidationException("Please give a reason when rejecting a submission.");
        }
        taskDAO.review(sub.getSubmissionId(), approve ? Submission.APPROVED : Submission.REJECTED,
                remarks == null || remarks.trim().isEmpty() ? null : remarks.trim());
        notificationDAO.notifyStudent(sub.getStudentId(), "Your answer to \"" + sub.getTaskTitle() + "\" ("
                + sub.getCourseCode() + ") was " + (approve ? "approved." : "rejected: " + remarks.trim()));
    }

    /**
     * @param type      "Assignment" or "Video"
     * @param appliesTo "Both", "Theory" or "Lab"
     */
    public void create(Session session, int courseId, String type, String appliesTo, String title,
                       String description, String videoUrl) throws UCMSException {
        CourseService.requireAdmin(session);
        String url = null;
        if (PrerequisiteTask.VIDEO.equals(type)) {
            url = Validator.required(videoUrl, "Video link");
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new ValidationException("The video link must start with http:// or https://");
            }
        }
        taskDAO.create(courseId, type, appliesTo, Validator.required(title, "Title"), description, url);
    }

    public void delete(Session session, int taskId) throws UCMSException {
        CourseService.requireAdmin(session);
        taskDAO.delete(taskId);
    }

    private static void requireStudent(Session session) throws AuthException {
        if (!session.isStudent()) {
            throw new AuthException("Only students complete prerequisite tasks.");
        }
    }
}
