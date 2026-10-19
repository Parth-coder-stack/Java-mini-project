package model;

import java.time.LocalDate;

/** A student's registration in a section, flattened with the section details for display. */
public class Enrollment {

    private final int enrollmentId;
    private final int studentId;
    private final String studentName;
    private final String prn;
    private final String studentEmail;
    private final int sectionId;
    private final int courseId;
    private final String courseCode;
    private final String courseTitle;
    private final String componentType;
    private final double credits;
    private final String sectionNumber;
    private final String slot;
    private final String facultyName;
    private final String roomNumber;
    private final String semesterName;
    private final String status;
    private final String grade;
    private final LocalDate enrollmentDate;
    private final boolean overridden;

    public Enrollment(int enrollmentId, int studentId, String studentName, String prn, String studentEmail,
                      int sectionId, int courseId, String courseCode, String courseTitle,
                      String componentType, double credits, String sectionNumber, String slot,
                      String facultyName, String roomNumber, String semesterName,
                      String status, String grade, LocalDate enrollmentDate, boolean overridden) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.prn = prn;
        this.studentEmail = studentEmail;
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.componentType = componentType;
        this.credits = credits;
        this.sectionNumber = sectionNumber;
        this.slot = slot;
        this.facultyName = facultyName;
        this.roomNumber = roomNumber;
        this.semesterName = semesterName;
        this.status = status;
        this.grade = grade;
        this.enrollmentDate = enrollmentDate;
        this.overridden = overridden;
    }

    public int getEnrollmentId()        { return enrollmentId; }
    public int getStudentId()           { return studentId; }
    public String getStudentName()      { return studentName; }
    public String getPrn()              { return prn; }
    public String getStudentEmail()     { return studentEmail; }
    public int getSectionId()           { return sectionId; }
    public int getCourseId()            { return courseId; }
    public String getCourseCode()       { return courseCode; }
    public String getCourseTitle()      { return courseTitle; }
    public String getComponentType()    { return componentType; }
    public double getCredits()          { return credits; }
    public String getSectionNumber()    { return sectionNumber; }
    public String getSlot()             { return slot; }
    public String getFacultyName()      { return facultyName; }
    public String getRoomNumber()       { return roomNumber; }
    public String getSemesterName()     { return semesterName; }
    public String getStatus()           { return status; }
    public String getGrade()            { return grade; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public boolean isOverridden()       { return overridden; }

    public boolean isActive() {
        return "Enrolled".equals(status);
    }

    public String getSectionKey() {
        return courseCode + "-" + componentType + "-" + sectionNumber;
    }
}
