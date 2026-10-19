package service;

import db.dao.ClassroomDAO;
import db.dao.SectionDAO;
import exception.UCMSException;
import exception.ValidationException;
import model.Classroom;
import model.RoomUsage;
import model.Section;

import java.util.ArrayList;
import java.util.List;

/** Classroom records and utilization report (admin). */
public class ClassroomService {

    private final ClassroomDAO classroomDAO = new ClassroomDAO();
    private final SectionDAO sectionDAO = new SectionDAO();

    public List<Classroom> getAll() throws UCMSException {
        return classroomDAO.findAll();
    }

    public void create(Session s, String roomNumber, String type, String capacity) throws UCMSException {
        CourseService.requireAdmin(s);
        classroomDAO.create(roomNumber(roomNumber), type(type), Validator.positiveInt(capacity, "Capacity"));
    }

    public void update(Session s, Classroom room, String roomNumber, String type, String capacity) throws UCMSException {
        CourseService.requireAdmin(s);
        int cap = Validator.positiveInt(capacity, "Capacity");
        for (Section sec : sectionDAO.findCurrent()) {
            if (sec.getRoomId() == room.getRoomId() && sec.getMaxCapacity() > cap) {
                throw new ValidationException(sec.getKey() + " in this room has capacity " + sec.getMaxCapacity()
                        + " - lower that section first.");
            }
        }
        classroomDAO.update(room.getRoomId(), roomNumber(roomNumber), type(type), cap);
    }

    public void delete(Session s, Classroom room) throws UCMSException {
        CourseService.requireAdmin(s);
        classroomDAO.delete(room.getRoomId());
    }

    /** Hours booked and seats filled per room, current semester. */
    public List<RoomUsage> getUtilization() throws UCMSException {
        List<Section> sections = sectionDAO.findCurrent();
        List<RoomUsage> rows = new ArrayList<>();
        for (Classroom room : classroomDAO.findAll()) {
            int count = 0;
            int seated = 0;
            int offered = 0;
            double hours = 0;
            for (Section sec : sections) {
                if (sec.getRoomId() == room.getRoomId()) {
                    count++;
                    seated += sec.getEnrolledCount();
                    offered += sec.getMaxCapacity();
                    hours += sec.getWeeklyHours();
                }
            }
            rows.add(new RoomUsage(room, count, hours, seated, offered));
        }
        return rows;
    }

    private static String roomNumber(String v) throws ValidationException {
        return Validator.required(v, "Room number").toUpperCase();
    }

    private static String type(String v) throws ValidationException {
        if (!"Theory".equals(v) && !"Lab".equals(v)) {
            throw new ValidationException("Room type must be Theory or Lab.");
        }
        return v;
    }
}
