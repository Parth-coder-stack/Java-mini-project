package model;

/**
 * Something a student must finish before enrolling in a course:
 * an Assignment to submit, or a basic Video to watch.
 * It can be required for the course's Theory sections, its Lab sections, or Both.
 */
public class PrerequisiteTask {

    public static final String ASSIGNMENT = "Assignment";
    public static final String VIDEO = "Video";
    public static final String BOTH = "Both";

    private final int taskId;
    private final int courseId;
    private final String courseCode;
    private final String taskType;
    private final String appliesTo;
    private final String title;
    private final String description;
    private final String videoUrl;

    public PrerequisiteTask(int taskId, int courseId, String courseCode, String taskType, String appliesTo,
                            String title, String description, String videoUrl) {
        this.taskId = taskId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.taskType = taskType;
        this.appliesTo = appliesTo;
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
    }

    public int getTaskId()         { return taskId; }
    public int getCourseId()       { return courseId; }
    public String getCourseCode()  { return courseCode; }
    public String getTaskType()    { return taskType; }
    public String getAppliesTo()   { return appliesTo; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getVideoUrl()    { return videoUrl; }

    public boolean isVideo() {
        return VIDEO.equals(taskType);
    }

    /** Is this task required for a section of the given component ("Theory" / "Lab")? */
    public boolean appliesTo(String componentType) {
        return BOTH.equals(appliesTo) || appliesTo.equals(componentType);
    }

    /** "Theory & Lab", "Theory only" or "Lab only". */
    public String getAppliesToText() {
        return BOTH.equals(appliesTo) ? "Theory & Lab" : appliesTo + " only";
    }

    @Override
    public String toString() {
        return courseCode + " - " + taskType + ": " + title;
    }
}
