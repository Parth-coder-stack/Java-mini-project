package service;

import db.dao.ReportDAO;
import exception.UCMSException;
import model.AuditEntry;

import java.util.List;
import java.util.Map;

/** Admin statistics and grade audit trail. */
public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();

    public Map<String, Integer> getCounts() throws UCMSException {
        return reportDAO.getCounts();
    }

    public List<AuditEntry> getAuditLog(Session session) throws UCMSException {
        CourseService.requireAdmin(session);
        return reportDAO.findAuditLog();
    }
}
