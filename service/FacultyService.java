package service;

import db.dao.FacultyDAO;
import db.dao.SectionDAO;
import db.dao.UserDAO;
import exception.UCMSException;
import exception.ValidationException;
import model.Faculty;
import model.Section;
import model.TeachingLoad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Faculty records (admin) and teaching-load reports (admin + faculty). */
public class FacultyService {

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private final SectionDAO sectionDAO = new SectionDAO();
    private final UserDAO userDAO = new UserDAO();

    public List<Faculty> getAll() throws UCMSException {
        return facultyDAO.findAll();
    }

    public int create(Session s, String name, String email, String phone, String designation, int deptId,
                      String password) throws UCMSException {
        CourseService.requireAdmin(s);
        String n = Validator.required(name, "Name");
        String e = Validator.email(email);
        String p = Validator.phone(phone);
        Validator.password(password, password);
        if (deptId <= 0) {
            throw new ValidationException("Choose a department.");
        }
        if (facultyDAO.emailExists(e) || userDAO.emailExists(e)) {
            throw new ValidationException("An account with email " + e + " already exists.");
        }
        return facultyDAO.createWithAccount(n, e, p, Validator.required(designation, "Designation"), deptId,
                AuthService.hash(password));
    }

    public void update(Session s, Faculty f, String name, String phone, String designation, int deptId)
            throws UCMSException {
        CourseService.requireAdmin(s);
        facultyDAO.update(f.getId(), Validator.required(name, "Name"), Validator.phone(phone),
                Validator.required(designation, "Designation"), deptId);
    }

    public void delete(Session s, Faculty f) throws UCMSException {
        CourseService.requireAdmin(s);
        if (!sectionDAO.findAllByFaculty(f.getId()).isEmpty()) {
            throw new ValidationException(f.getName() + " has sections. Re-assign them to another faculty member "
                    + "(Sections page -> Edit) before deleting.");
        }
        facultyDAO.delete(f.getId());
    }

    /** One row per semester the faculty member has taught, newest first. */
    public List<TeachingLoad> getTeachingLoad(Faculty f) throws UCMSException {
        Map<String, List<Section>> bySemester = new LinkedHashMap<>();
        for (Section sec : sectionDAO.findAllByFaculty(f.getId())) {
            bySemester.computeIfAbsent(sec.getSemesterName(), k -> new ArrayList<>()).add(sec);
        }
        List<TeachingLoad> rows = new ArrayList<>();
        for (Map.Entry<String, List<Section>> e : bySemester.entrySet()) {
            rows.add(summarise(f.getName(), e.getKey(), e.getValue()));
        }
        return rows;
    }

    /** Current-semester load of every faculty member (admin report). */
    public List<TeachingLoad> getCurrentLoadReport() throws UCMSException {
        List<TeachingLoad> rows = new ArrayList<>();
        String semester = sectionDAO.findCurrentSemester().getName();
        for (Faculty f : facultyDAO.findAll()) {
            rows.add(summarise(f.getName(), semester, sectionDAO.findCurrentByFaculty(f.getId())));
        }
        return rows;
    }

    private static TeachingLoad summarise(String name, String semester, List<Section> sections) {
        int students = 0;
        double hours = 0;
        double credits = 0;
        for (Section s : sections) {
            students += s.getEnrolledCount();
            hours += s.getWeeklyHours();
            credits += s.getCredits();
        }
        return new TeachingLoad(name, semester, sections.size(), students, hours, credits);
    }
}
