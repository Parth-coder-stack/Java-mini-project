package model;

import java.time.LocalDate;

/** A student's place in the queue for a full section. */
public class WaitlistEntry {

    private final int waitlistId;
    private final int studentId;
    private final String studentName;
    private final int sectionId;
    private final String sectionKey;
    private final String slot;
    private final int position;
    private final String status;
    private final LocalDate waitlistDate;

    public WaitlistEntry(int waitlistId, int studentId, String studentName, int sectionId,
                         String sectionKey, String slot, int position, String status,
                         LocalDate waitlistDate) {
        this.waitlistId = waitlistId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.sectionId = sectionId;
        this.sectionKey = sectionKey;
        this.slot = slot;
        this.position = position;
        this.status = status;
        this.waitlistDate = waitlistDate;
    }

    public int getWaitlistId()         { return waitlistId; }
    public int getStudentId()          { return studentId; }
    public String getStudentName()     { return studentName; }
    public int getSectionId()          { return sectionId; }
    public String getSectionKey()      { return sectionKey; }
    public String getSlot()            { return slot; }
    public int getPosition()           { return position; }
    public String getStatus()          { return status; }
    public LocalDate getWaitlistDate() { return waitlistDate; }
}
