package model;

import java.time.LocalDate;

/** Attendance for one enrollment on one class date. */
public class AttendanceRecord {

    private final int attendanceId;
    private final int enrollmentId;
    private final LocalDate classDate;
    private final String status;

    public AttendanceRecord(int attendanceId, int enrollmentId, LocalDate classDate, String status) {
        this.attendanceId = attendanceId;
        this.enrollmentId = enrollmentId;
        this.classDate = classDate;
        this.status = status;
    }

    public int getAttendanceId()   { return attendanceId; }
    public int getEnrollmentId()   { return enrollmentId; }
    public LocalDate getClassDate() { return classDate; }
    public String getStatus()      { return status; }
}
