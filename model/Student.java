package model;

import java.time.LocalDateTime;

/** A student, identified by PRN (Permanent Registration Number). */
public class Student extends Person {

    private final String prn;
    private String address;
    private final int yearOfStudy;
    private final int deptId;
    private final String deptName;
    private LocalDateTime contactVerifiedAt;

    public Student(int id, String prn, String name, String email, String phone, String address,
                   int yearOfStudy, int deptId, String deptName, LocalDateTime contactVerifiedAt) {
        super(id, name, email, phone);
        this.prn = prn;
        this.address = address;
        this.yearOfStudy = yearOfStudy;
        this.deptId = deptId;
        this.deptName = deptName;
        this.contactVerifiedAt = contactVerifiedAt;
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Override
    public String getSubtitle() {
        return "PRN " + prn;
    }

    public String getPrn()                        { return prn; }
    public String getAddress()                    { return address; }
    public int getYearOfStudy()                   { return yearOfStudy; }
    public int getDeptId()                        { return deptId; }
    public String getDeptName()                   { return deptName; }
    public LocalDateTime getContactVerifiedAt()   { return contactVerifiedAt; }
    public boolean isContactVerified()            { return contactVerifiedAt != null; }

    public void setAddress(String address)                      { this.address = address; }
    public void setContactVerifiedAt(LocalDateTime verifiedAt)  { this.contactVerifiedAt = verifiedAt; }

    @Override
    public String toString() {
        return getName() + " (" + prn + ")";
    }
}
