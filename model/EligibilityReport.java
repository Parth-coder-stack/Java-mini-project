package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of checking whether a student may enroll in a course:
 * one line per prerequisite course, prerequisite task (assignment / video) and contact-detail check.
 */
public class EligibilityReport {

    /** One line of the report. Non-blocking checks are shown but never stop enrollment. */
    public static class Check {
        private final String category;
        private final String label;
        private final boolean passed;
        private final boolean blocking;
        private final String detail;

        public Check(String category, String label, boolean passed, boolean blocking, String detail) {
            this.category = category;
            this.label = label;
            this.passed = passed;
            this.blocking = blocking;
            this.detail = detail;
        }

        public String getCategory() { return category; }
        public String getLabel()    { return label; }
        public boolean isPassed()   { return passed; }
        public boolean isBlocking() { return blocking; }
        public String getDetail()   { return detail; }
    }

    public static final String PREREQUISITE = "Prerequisite";
    public static final String TASK = "Prerequisite task";
    public static final String CONTACT = "Contact details";
    public static final String STATUS = "Course status";

    private final Course course;
    private final List<Check> checks = new ArrayList<>();
    private boolean enrolledNow;

    public EligibilityReport(Course course) {
        this.course = course;
    }

    /** True when the student already has an active section of this course. */
    public boolean isEnrolledNow()                 { return enrolledNow; }
    public void setEnrolledNow(boolean enrolledNow) { this.enrolledNow = enrolledNow; }

    public void add(Check check) {
        checks.add(check);
    }

    public Course getCourse()      { return course; }
    public List<Check> getChecks() { return Collections.unmodifiableList(checks); }

    /** Failed blocking checks, optionally ignoring one category (e.g. contact, which the wizard fixes). */
    public List<Check> getFailures(String ignoredCategory) {
        List<Check> failed = new ArrayList<>();
        for (Check c : checks) {
            if (!c.isPassed() && c.isBlocking() && !c.getCategory().equals(ignoredCategory)) {
                failed.add(c);
            }
        }
        return failed;
    }

    public boolean isEligible() {
        return getFailures("").isEmpty();
    }

    public boolean isCategoryPassed(String category) {
        for (Check c : checks) {
            if (c.getCategory().equals(category) && c.isBlocking() && !c.isPassed()) {
                return false;
            }
        }
        return true;
    }

    /** Short text for the catalog table, e.g. "Eligible" or "Missing: CS201". */
    public String getSummary() {
        if (enrolledNow) {
            return "Enrolled this semester";
        }
        List<Check> failed = getFailures(CONTACT);
        if (failed.isEmpty()) {
            return isCategoryPassed(CONTACT) ? "Eligible" : "Eligible (verify contact)";
        }
        List<String> parts = new ArrayList<>();
        for (Check c : failed) {
            parts.add(c.getLabel());
        }
        return "Blocked: " + String.join(", ", parts);
    }
}
