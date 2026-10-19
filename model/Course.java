package model;

import java.util.ArrayList;
import java.util.List;

/** A course in the catalog, together with its prerequisites. */
public class Course {

    private final int courseId;
    private final String courseCode;
    private final String title;
    private final String description;
    private final int deptId;
    private final String deptName;
    private final double credits;
    private final List<Prerequisite> prerequisites = new ArrayList<>();

    public Course(int courseId, String courseCode, String title, String description,
                  int deptId, String deptName, double credits) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
        this.deptId = deptId;
        this.deptName = deptName;
        this.credits = credits;
    }

    public int getCourseId()                    { return courseId; }
    public String getCourseCode()               { return courseCode; }
    public String getTitle()                    { return title; }
    public String getDescription()              { return description; }
    public int getDeptId()                      { return deptId; }
    public String getDeptName()                 { return deptName; }
    public double getCredits()                  { return credits; }
    public List<Prerequisite> getPrerequisites() { return prerequisites; }

    /** e.g. "CS101 (min D), IT101 (min C)" or "None". */
    public String getPrerequisiteText() {
        if (prerequisites.isEmpty()) {
            return "None";
        }
        List<String> parts = new ArrayList<>();
        for (Prerequisite p : prerequisites) {
            parts.add(p.toString());
        }
        return String.join(", ", parts);
    }

    @Override
    public String toString() {
        return courseCode + " - " + title;
    }
}
