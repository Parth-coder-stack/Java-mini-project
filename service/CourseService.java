package service;

import db.dao.ClassroomDAO;
import db.dao.CourseDAO;
import db.dao.SectionDAO;
import exception.AuthException;
import exception.UCMSException;
import exception.ValidationException;
import model.Classroom;
import model.Course;
import model.CourseComponent;
import model.Prerequisite;
import model.Section;
import model.Semester;
import model.TimeSlot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Course catalog, prerequisites and sections. Changes are admin-only. */
public class CourseService {

    private final CourseDAO courseDAO = new CourseDAO();
    private final SectionDAO sectionDAO = new SectionDAO();

    public List<Course> getCatalog() throws UCMSException {
        return courseDAO.findAll();
    }

    public Course getCourse(int courseId) throws UCMSException {
        return courseDAO.findById(courseId);
    }

    public List<Section> getSectionsForCourse(int courseId) throws UCMSException {
        return sectionDAO.findCurrentByCourse(courseId);
    }

    public List<Section> getCurrentSections() throws UCMSException {
        return sectionDAO.findCurrent();
    }

    public List<Section> getSectionsForFaculty(int facultyId) throws UCMSException {
        return sectionDAO.findCurrentByFaculty(facultyId);
    }

    /** Every section the faculty member has taught, all semesters. */
    public List<Section> getAllSectionsForFaculty(int facultyId) throws UCMSException {
        return sectionDAO.findAllByFaculty(facultyId);
    }

    public Section getSection(int sectionId) throws UCMSException {
        return sectionDAO.findById(sectionId);
    }

    public List<CourseComponent> getComponents(int courseId) throws UCMSException {
        return courseDAO.findComponents(courseId);
    }

    public List<Classroom> getRooms() throws UCMSException {
        return new ClassroomDAO().findAll();
    }

    public Semester getCurrentSemester() throws UCMSException {
        return sectionDAO.findCurrentSemester();
    }

    // ---------------------------------------------------------------- admin

    public int createCourse(Session s, String code, String title, String description, int deptId,
                            String theoryCredits, String labCredits) throws UCMSException {
        requireAdmin(s);
        double theory = Validator.nonNegativeDouble(theoryCredits, "Theory credits");
        double lab = labCredits == null || labCredits.trim().isEmpty()
                ? 0 : Validator.nonNegativeDouble(labCredits, "Lab credits");
        if (theory <= 0) {
            throw new ValidationException("Theory credits must be greater than 0.");
        }
        return courseDAO.create(courseCode(code), Validator.required(title, "Title"), description, deptId, theory, lab);
    }

    public void updateCourse(Session s, int courseId, String code, String title, String description, int deptId)
            throws UCMSException {
        requireAdmin(s);
        courseDAO.updateCourse(courseId, courseCode(code), Validator.required(title, "Title"), description, deptId);
    }

    public void deleteCourse(Session s, int courseId) throws UCMSException {
        requireAdmin(s);
        courseDAO.delete(courseId);
    }

    /** Adds "courseId requires requiredId", refusing self-references and cycles. */
    public void addPrerequisite(Session s, int courseId, int requiredId, String minGrade, boolean mandatory)
            throws UCMSException {
        requireAdmin(s);
        if (courseId == requiredId) {
            throw new ValidationException("A course cannot be its own prerequisite.");
        }
        if (!GradeScale.isValid(minGrade) || !GradeScale.isPass(minGrade)) {
            throw new ValidationException("Minimum grade must be a passing grade (A+ ... D).");
        }
        if (requires(requiredId, courseId)) {
            throw new ValidationException("That would create a circular prerequisite chain.");
        }
        courseDAO.addPrerequisite(courseId, requiredId, minGrade, mandatory);
    }

    public void removePrerequisite(Session s, Prerequisite p) throws UCMSException {
        requireAdmin(s);
        courseDAO.deletePrerequisite(p.getPrereqId());
    }

    public int createSection(Session s, int courseId, int componentId, int facultyId, Classroom room,
                             String sectionNumber, String capacity, String slot) throws UCMSException {
        requireAdmin(s);
        int cap = Validator.positiveInt(capacity, "Capacity");
        String slotText = checkSchedule(0, facultyId, room, cap, slot);
        return sectionDAO.create(courseId, componentId, facultyId, room.getRoomId(),
                Validator.required(sectionNumber, "Section").toUpperCase(), cap, slotText);
    }

    /** Re-assigns a section's faculty, room, time slot or capacity (with the same clash checks). */
    public void updateSection(Session s, Section section, int facultyId, Classroom room, String capacity, String slot)
            throws UCMSException {
        requireAdmin(s);
        int cap = Validator.positiveInt(capacity, "Capacity");
        if (cap < section.getEnrolledCount()) {
            throw new ValidationException(section.getEnrolledCount() + " students are already enrolled - "
                    + "capacity cannot be lower than that.");
        }
        String slotText = checkSchedule(section.getSectionId(), facultyId, room, cap, slot);
        sectionDAO.update(section.getSectionId(), facultyId, room.getRoomId(), cap, slotText);
    }

    /**
     * Timetable clash detection for scheduling (SRS 3.4): the room must be big enough, and in the
     * current semester neither the faculty member nor the room may already be booked at that time.
     *
     * @param ignoreSectionId the section being edited (0 when creating)
     * @return the validated slot text
     */
    private String checkSchedule(int ignoreSectionId, int facultyId, Classroom room, int capacity, String slot)
            throws UCMSException {
        if (capacity > room.getCapacity()) {
            throw new ValidationException("Room " + room.getRoomNumber() + " only seats " + room.getCapacity() + ".");
        }
        String slotText = Validator.required(slot, "Slot");
        TimeSlot newSlot;
        try {
            newSlot = TimeSlot.parse(slotText);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        }
        List<String> clashes = new ArrayList<>();
        for (Section other : sectionDAO.findCurrent()) {
            if (other.getSectionId() == ignoreSectionId || !TimeSlot.parse(other.getSlot()).overlaps(newSlot)) {
                continue;
            }
            if (other.getFacultyId() == facultyId) {
                clashes.add("Faculty double booking: " + other.getFacultyName() + " already teaches "
                        + other.getKey() + " at " + other.getSlot());
            }
            if (other.getRoomId() == room.getRoomId()) {
                clashes.add("Classroom double booking: " + room.getRoomNumber() + " is used by "
                        + other.getKey() + " at " + other.getSlot());
            }
        }
        if (!clashes.isEmpty()) {
            throw new ValidationException(String.join("\n", clashes));
        }
        return slotText;
    }

    public void deleteSection(Session s, int sectionId) throws UCMSException {
        requireAdmin(s);
        sectionDAO.delete(sectionId);
    }

    /** Depth-first search: does {@code from} (directly or indirectly) require {@code target}? */
    private boolean requires(int from, int target) throws UCMSException {
        Map<Integer, List<Prerequisite>> graph = courseDAO.findAllPrerequisites();
        Deque<Integer> stack = new ArrayDeque<>();
        Set<Integer> seen = new HashSet<>();
        stack.push(from);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (current == target) {
                return true;
            }
            if (seen.add(current)) {
                for (Prerequisite p : graph.getOrDefault(current, Collections.emptyList())) {
                    stack.push(p.getRequiredCourseId());
                }
            }
        }
        return false;
    }

    private static String courseCode(String code) throws ValidationException {
        String c = Validator.required(code, "Course code").toUpperCase();
        if (!Validator.COURSE_CODE.matcher(c).matches()) {
            throw new ValidationException("Course code must look like CS101 (2-4 letters + 3 digits).");
        }
        return c;
    }

    static void requireAdmin(Session s) throws AuthException {
        if (!s.isAdmin()) {
            throw new AuthException("Only the administrator can do this.");
        }
    }
}
