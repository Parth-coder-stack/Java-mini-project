package model;

public class Department {

    private final int deptId;
    private final String deptName;
    private final String deptCode;

    public Department(int deptId, String deptName, String deptCode) {
        this.deptId = deptId;
        this.deptName = deptName;
        this.deptCode = deptCode;
    }

    public int getDeptId()      { return deptId; }
    public String getDeptName() { return deptName; }
    public String getDeptCode() { return deptCode; }

    @Override
    public String toString() {
        return deptCode + " - " + deptName;
    }
}
