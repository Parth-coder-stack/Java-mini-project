package service;

import db.dao.AttendanceDAO;
import db.dao.SectionDAO;
import exception.AuthException;
import exception.UCMSException;
import exception.ValidationException;
import model.AttendanceRecord;
import model.AttendanceSummary;
import model.Section;
import model.Semester;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Attendance viewing (students) and marking (faculty). */
public class AttendanceService {

    /** Below this percentage a student is flagged. */
    public static final double MIN_PERCENT = 75.0;

    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final SectionDAO sectionDAO = new SectionDAO();

    public List<AttendanceSummary> getSummaries(int studentId) throws UCMSException {
        return attendanceDAO.summariesForStudent(studentId);
    }

    public List<AttendanceRecord> getRecords(int enrollmentId) throws UCMSException {
        return attendanceDAO.findByEnrollment(enrollmentId);
    }

    /** Overall attendance across all current courses. */
    public double overallPercent(List<AttendanceSummary> summaries) {
        int attended = 0;
        int total = 0;
        for (AttendanceSummary s : summaries) {
            attended += s.getPresent() + s.getLate();
            total += s.getTotal();
        }
        return total == 0 ? 100.0 : attended * 100.0 / total;
    }

    public Map<Integer, AttendanceSummary> getSectionSummaries(int sectionId) throws UCMSException {
        return attendanceDAO.summariesForSection(sectionId);
    }

    public Map<Integer, String> getStatuses(int sectionId, LocalDate date) throws UCMSException {
        return attendanceDAO.findStatuses(sectionId, date);
    }

    /** Saves attendance for a whole class on one date. */
    public void saveAttendance(Session session, int sectionId, LocalDate date, Map<Integer, String> statuses)
            throws UCMSException {
        Section section = sectionDAO.findById(sectionId);
        if (session.isFaculty() && section.getFacultyId() != session.getFaculty().getId()) {
            throw new AuthException("You can only mark attendance for your own sections.");
        }
        if (!session.isFaculty() && !session.isAdmin()) {
            throw new AuthException("Only faculty can mark attendance.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Attendance cannot be marked for a future date.");
        }
        Semester sem = sectionDAO.findCurrentSemester();
        if (sem != null && date.isBefore(sem.getStartDate())) {
            throw new ValidationException("Date is before the semester started (" + sem.getStartDate() + ").");
        }
        if (statuses.isEmpty()) {
            throw new ValidationException("There are no students in this section.");
        }
        attendanceDAO.saveBatch(statuses, date);
    }
}
