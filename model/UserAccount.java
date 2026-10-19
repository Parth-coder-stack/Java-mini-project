package model;

/** One row of user_account: login credentials linked to a student/faculty id. */
public class UserAccount {

    private final int userId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final int roleRefId;

    public UserAccount(int userId, String email, String passwordHash, Role role, int roleRefId) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.roleRefId = roleRefId;
    }

    public int getUserId()          { return userId; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole()           { return role; }
    public int getRoleRefId()       { return roleRefId; }
}
