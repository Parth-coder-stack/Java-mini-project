package model;

/**
 * Abstract base class for everyone who can log in.
 * Student, Faculty and Admin extend it (Inheritance + Polymorphism).
 */
public abstract class Person {

    private final int id;
    private String name;
    private String email;
    private String phone;

    protected Person(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    /** Role label shown in the UI, e.g. "STUDENT". */
    public abstract Role getRole();

    /** Short line shown under the name in the dashboard sidebar. */
    public abstract String getSubtitle();

    public int getId()          { return id; }
    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }

    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }

    @Override
    public String toString() {
        return name;
    }
}
