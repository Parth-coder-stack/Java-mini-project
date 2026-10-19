package model;

/** "Course X requires course Y with at least grade G". */
public class Prerequisite {

    private final int prereqId;
    private final int courseId;
    private final int requiredCourseId;
    private final String requiredCode;
    private final String requiredTitle;
    private final String minGrade;
    private final boolean mandatory;

    public Prerequisite(int prereqId, int courseId, int requiredCourseId, String requiredCode,
                        String requiredTitle, String minGrade, boolean mandatory) {
        this.prereqId = prereqId;
        this.courseId = courseId;
        this.requiredCourseId = requiredCourseId;
        this.requiredCode = requiredCode;
        this.requiredTitle = requiredTitle;
        this.minGrade = minGrade;
        this.mandatory = mandatory;
    }

    public int getPrereqId()         { return prereqId; }
    public int getCourseId()         { return courseId; }
    public int getRequiredCourseId() { return requiredCourseId; }
    public String getRequiredCode()  { return requiredCode; }
    public String getRequiredTitle() { return requiredTitle; }
    public String getMinGrade()      { return minGrade; }
    public boolean isMandatory()     { return mandatory; }

    @Override
    public String toString() {
        return requiredCode + " (min " + minGrade + (mandatory ? ")" : ", recommended)");
    }
}
