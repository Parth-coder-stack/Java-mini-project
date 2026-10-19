package model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A student's CGPA, SGPA per semester and whether they are on academic probation. */
public class AcademicStanding {

    private final Student student;
    private final double cgpa;
    private final double probationThreshold;
    private final Map<String, Double> sgpaBySemester;

    public AcademicStanding(Student student, double cgpa, double probationThreshold,
                            Map<String, Double> sgpaBySemester) {
        this.student = student;
        this.cgpa = cgpa;
        this.probationThreshold = probationThreshold;
        this.sgpaBySemester = new LinkedHashMap<>(sgpaBySemester);
    }

    public Student getStudent()                  { return student; }
    public double getCgpa()                      { return cgpa; }
    public double getProbationThreshold()        { return probationThreshold; }
    public Map<String, Double> getSgpaBySemester() { return Collections.unmodifiableMap(sgpaBySemester); }

    /** Has graded results and the CGPA is below the threshold. */
    public boolean isOnProbation() {
        return cgpa > 0 && cgpa < probationThreshold;
    }

    public String getStandingText() {
        if (cgpa == 0) {
            return "No grades yet";
        }
        return isOnProbation() ? "Academic probation" : "Good standing";
    }
}
