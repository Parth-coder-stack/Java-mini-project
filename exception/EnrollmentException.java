package exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when a student cannot be enrolled in a section, e.g. because a
 * prerequisite is missing, a prerequisite task (assignment / video) is not done, or the
 * timetable clashes. Carries every reason so the UI can list them.
 */
public class EnrollmentException extends UCMSException {

    private final List<String> reasons;

    public EnrollmentException(List<String> reasons) {
        super("Enrollment blocked:\n - " + String.join("\n - ", reasons));
        this.reasons = new ArrayList<>(reasons);
    }

    public EnrollmentException(String reason) {
        this(Collections.singletonList(reason));
    }

    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }
}
