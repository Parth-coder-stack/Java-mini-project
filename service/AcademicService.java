package service;

import db.dao.AcademicDAO;
import db.dao.StudentDAO;
import exception.UCMSException;
import exception.ValidationException;
import model.AcademicStanding;
import model.Student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Grade finalization & academic standing (SRS 3.4): SGPA / CGPA come from the
 * MySQL stored functions; students below the configurable threshold are on probation.
 */
public class AcademicService {

    public static final String PROBATION_KEY = "probation_cgpa";

    private final AcademicDAO academicDAO = new AcademicDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    public double getProbationThreshold() throws UCMSException {
        try {
            return Double.parseDouble(academicDAO.getSetting(PROBATION_KEY, "5.00"));
        } catch (NumberFormatException e) {
            return 5.0;
        }
    }

    public void setProbationThreshold(Session s, String value) throws UCMSException {
        CourseService.requireAdmin(s);
        double v;
        try {
            v = Double.parseDouble(Validator.required(value, "Threshold"));
        } catch (NumberFormatException e) {
            throw new ValidationException("Threshold must be a number like 5.0");
        }
        if (v <= 0 || v > 10) {
            throw new ValidationException("Threshold must be between 0 and 10.");
        }
        academicDAO.setSetting(PROBATION_KEY, String.format("%.2f", v));
    }

    public AcademicStanding getStanding(int studentId) throws UCMSException {
        Student st = studentDAO.findById(studentId);
        return new AcademicStanding(st, academicDAO.cgpa(studentId), getProbationThreshold(),
                academicDAO.sgpaBySemester(studentId));
    }

    /** Students whose CGPA is below the threshold, lowest first. */
    public List<AcademicStanding> getProbationList() throws UCMSException {
        double threshold = getProbationThreshold();
        List<AcademicStanding> list = new ArrayList<>();
        for (Map.Entry<Student, Double> e : academicDAO.findBelow(threshold).entrySet()) {
            list.add(new AcademicStanding(e.getKey(), e.getValue(), threshold, Collections.emptyMap()));
        }
        return list;
    }
}
