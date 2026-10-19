package model;

/** The system administrator. Has no row of its own besides user_account. */
public class Admin extends Person {

    public Admin(String email) {
        super(0, "Administrator", email, null);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    @Override
    public String getSubtitle() {
        return "System Admin";
    }
}
