package model;

/** How much one faculty member teaches in one semester. */
public class TeachingLoad {

    private final String facultyName;
    private final String semesterName;
    private final int sections;
    private final int students;
    private final double weeklyHours;
    private final double credits;

    public TeachingLoad(String facultyName, String semesterName, int sections, int students,
                        double weeklyHours, double credits) {
        this.facultyName = facultyName;
        this.semesterName = semesterName;
        this.sections = sections;
        this.students = students;
        this.weeklyHours = weeklyHours;
        this.credits = credits;
    }

    public String getFacultyName()  { return facultyName; }
    public String getSemesterName() { return semesterName; }
    public int getSections()        { return sections; }
    public int getStudents()        { return students; }
    public double getWeeklyHours()  { return weeklyHours; }
    public double getCredits()      { return credits; }
}
