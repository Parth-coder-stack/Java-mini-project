package model;

import java.time.LocalDateTime;

/** One grade change recorded in audit_log. */
public class AuditEntry {

    private final int auditId;
    private final String changedBy;
    private final String studentPrn;
    private final String sectionKey;
    private final String oldGrade;
    private final String newGrade;
    private final LocalDateTime changedAt;

    public AuditEntry(int auditId, String changedBy, String studentPrn, String sectionKey,
                      String oldGrade, String newGrade, LocalDateTime changedAt) {
        this.auditId = auditId;
        this.changedBy = changedBy;
        this.studentPrn = studentPrn;
        this.sectionKey = sectionKey;
        this.oldGrade = oldGrade;
        this.newGrade = newGrade;
        this.changedAt = changedAt;
    }

    public int getAuditId()             { return auditId; }
    public String getChangedBy()        { return changedBy; }
    public String getStudentPrn()       { return studentPrn; }
    public String getSectionKey()       { return sectionKey; }
    public String getOldGrade()         { return oldGrade; }
    public String getNewGrade()         { return newGrade; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
