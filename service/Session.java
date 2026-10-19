package service;

import model.Faculty;
import model.Person;
import model.Role;
import model.Student;
import model.UserAccount;

/** The logged-in user: their account plus their Student/Faculty/Admin profile. */
public class Session {

    private final UserAccount account;
    private Person person;

    public Session(UserAccount account, Person person) {
        this.account = account;
        this.person = person;
    }

    public UserAccount getAccount() { return account; }
    public Person getPerson()       { return person; }
    public Role getRole()           { return account.getRole(); }

    public boolean isStudent() { return getRole() == Role.STUDENT; }
    public boolean isFaculty() { return getRole() == Role.FACULTY; }
    public boolean isAdmin()   { return getRole() == Role.ADMIN; }

    public Student getStudent() { return (Student) person; }
    public Faculty getFaculty() { return (Faculty) person; }

    /** Replaces the cached profile after it is edited (e.g. contact details). */
    public void refresh(Person updated) {
        this.person = updated;
    }
}
