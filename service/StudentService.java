package service;

import db.dao.DepartmentDAO;
import db.dao.FacultyDAO;
import db.dao.StudentDAO;
import exception.AuthException;
import exception.UCMSException;
import model.Department;
import model.Faculty;
import model.Student;

import java.util.List;
import java.util.Map;

/** Student profiles, contact details and the student directory search. */
public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();
    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public Student getStudent(int studentId) throws UCMSException {
        return studentDAO.findById(studentId);
    }

    /** Searches by PRN, name, email or phone; an empty keyword returns everyone. */
    public List<Student> search(String keyword) throws UCMSException {
        String k = keyword == null ? "" : keyword.trim();
        return k.isEmpty() ? studentDAO.findAll() : studentDAO.search(k);
    }

    /** Validates and saves phone/address, marking the contact details as verified. */
    public Student updateContact(Session session, int studentId, String phone, String address)
            throws UCMSException {
        if (session.isStudent() && session.getStudent().getId() != studentId) {
            throw new AuthException("You can only edit your own contact details.");
        }
        studentDAO.updateContact(studentId, Validator.phone(phone), Validator.address(address));
        Student updated = studentDAO.findById(studentId);
        if (session.isStudent()) {
            session.refresh(updated);
        }
        return updated;
    }

    /** Faculty teaching this student -> courses they teach them. */
    public Map<Faculty, String> getMyFaculty(int studentId) throws UCMSException {
        return facultyDAO.findForStudent(studentId);
    }

    public List<Faculty> getAllFaculty() throws UCMSException {
        return facultyDAO.findAll();
    }

    public List<Department> getDepartments() throws UCMSException {
        return departmentDAO.findAll();
    }

    public void deleteStudent(Session session, int studentId) throws UCMSException {
        if (!session.isAdmin()) {
            throw new AuthException("Only the administrator can delete students.");
        }
        studentDAO.delete(studentId);
    }
}
