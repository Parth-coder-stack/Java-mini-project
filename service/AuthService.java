package service;

import db.dao.FacultyDAO;
import db.dao.StudentDAO;
import db.dao.UserDAO;
import exception.AuthException;
import exception.UCMSException;
import exception.ValidationException;
import model.Admin;
import model.Person;
import model.UserAccount;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Login and student self-registration. */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();

    /**
     * Logs in with an e-mail address or (for students) a PRN.
     *
     * @throws AuthException if the credentials are wrong
     */
    public Session login(String emailOrPrn, String password) throws UCMSException {
        String id = Validator.required(emailOrPrn, "Email or PRN");
        Validator.required(password, "Password");

        UserAccount account = id.contains("@")
                ? userDAO.findByEmail(id.toLowerCase())
                : userDAO.findByStudentPrn(id);
        if (account == null || !account.getPasswordHash().equalsIgnoreCase(hash(password))) {
            throw new AuthException("Incorrect email/PRN or password.");
        }
        return new Session(account, loadProfile(account));
    }

    /** Creates a student + login account, then logs them in. */
    public Session registerStudent(String name, String prn, String email, String phone, String address,
                                   int deptId, int yearOfStudy, String password, String confirm)
            throws UCMSException {
        name = Validator.required(name, "Full name");
        prn = Validator.prn(prn);
        email = Validator.email(email);
        phone = Validator.phone(phone);
        address = Validator.address(address);
        Validator.password(password, confirm);
        if (deptId <= 0) {
            throw new ValidationException("Please choose your department.");
        }
        if (studentDAO.prnExists(prn)) {
            throw new ValidationException("A student with PRN " + prn + " is already registered.");
        }
        if (studentDAO.emailExists(email) || userDAO.emailExists(email)) {
            throw new ValidationException("An account with email " + email + " already exists.");
        }
        studentDAO.registerWithAccount(prn, name, email, phone, address, yearOfStudy, deptId, hash(password));
        return login(email, password);
    }

    private Person loadProfile(UserAccount account) throws UCMSException {
        Person person;
        switch (account.getRole()) {
            case STUDENT:
                person = studentDAO.findById(account.getRoleRefId());
                break;
            case FACULTY:
                person = facultyDAO.findById(account.getRoleRefId());
                break;
            default:
                person = new Admin(account.getEmail());
        }
        if (person == null) {
            throw new AuthException("This account is not linked to a profile. Contact the administrator.");
        }
        return person;
    }

    /** SHA-256 hex - matches MySQL's SHA2(text, 256) used in the seed data. */
    static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
