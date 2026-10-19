package model;

/** Totals of present/absent/late classes for one enrollment. "Late" counts as attended. */
public class AttendanceSummary {

    private final int enrollmentId;
    private final String courseCode;
    private final String courseTitle;
    private final String componentType;
    private final String facultyName;
    private final int present;
    private final int absent;
    private final int late;

    public AttendanceSummary(int enrollmentId, String courseCode, String courseTitle,
                             String componentType, String facultyName,
                             int present, int absent, int late) {
        this.enrollmentId = enrollmentId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.componentType = componentType;
        this.facultyName = facultyName;
        this.present = present;
        this.absent = absent;
        this.late = late;
    }

    public int getEnrollmentId()     { return enrollmentId; }
    public String getCourseCode()    { return courseCode; }
    public String getCourseTitle()   { return courseTitle; }
    public String getComponentType() { return componentType; }
    public String getFacultyName()   { return facultyName; }
    public int getPresent()          { return present; }
    public int getAbsent()           { return absent; }
    public int getLate()             { return late; }

    public int getTotal() {
        return present + absent + late;
    }

    /** Percentage attended, or 100 when no classes have been held yet. */
    public double getPercent() {
        int total = getTotal();
        return total == 0 ? 100.0 : (present + late) * 100.0 / total;
    }
}
