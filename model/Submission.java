package model;

import java.time.LocalDateTime;

/** A student's work on a prerequisite task: an assignment answer, or a "video watched" record. */
public class Submission {

    public static final String SUBMITTED = "Submitted";
    public static final String APPROVED = "Approved";
    public static final String REJECTED = "Rejected";
    public static final String WATCHED = "Watched";

    private final int submissionId;
    private final int taskId;
    private final String taskTitle;
    private final String courseCode;
    private final int studentId;
    private final String studentName;
    private final String prn;
    private final String content;
    private final LocalDateTime submittedAt;
    private final String status;
    private final String remarks;

    public Submission(int submissionId, int taskId, String taskTitle, String courseCode,
                      int studentId, String studentName, String prn, String content,
                      LocalDateTime submittedAt, String status, String remarks) {
        this.submissionId = submissionId;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.courseCode = courseCode;
        this.studentId = studentId;
        this.studentName = studentName;
        this.prn = prn;
        this.content = content;
        this.submittedAt = submittedAt;
        this.status = status;
        this.remarks = remarks;
    }

    public int getSubmissionId()          { return submissionId; }
    public int getTaskId()                { return taskId; }
    public String getTaskTitle()          { return taskTitle; }
    public String getCourseCode()         { return courseCode; }
    public int getStudentId()             { return studentId; }
    public String getStudentName()        { return studentName; }
    public String getPrn()                { return prn; }
    public String getContent()            { return content; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getStatus()             { return status; }
    public String getRemarks()            { return remarks; }

    /** Submitted/approved answers and watched videos count as done; rejected work does not. */
    public boolean satisfiesRequirement() {
        return !REJECTED.equals(status);
    }
}
