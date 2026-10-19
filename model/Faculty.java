package model;

/** A faculty member who teaches sections. */
public class Faculty extends Person {

    private final String designation;
    private final int deptId;
    private final String deptName;

    public Faculty(int id, String name, String email, String phone,
                   String designation, int deptId, String deptName) {
        super(id, name, email, phone);
        this.designation = designation;
        this.deptId = deptId;
        this.deptName = deptName;
    }

    @Override
    public Role getRole() {
        return Role.FACULTY;
    }

    @Override
    public String getSubtitle() {
        return designation == null ? "Faculty" : designation;
    }

    public String getDesignation() { return designation; }
    public int getDeptId()         { return deptId; }
    public String getDeptName()    { return deptName; }
}
