package ui.admin;

import model.AuditEntry;
import service.ReportService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;

import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;

/** Every grade change: who changed what, and when. */
public class AuditLogPage extends Page {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss");
    private final ReportService reportService = new ReportService();
    private final DataTable<AuditEntry> table;

    public AuditLogPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<AuditEntry>(new String[]{"When", "Changed by", "Student PRN", "Section", "Old grade", "New grade"},
                a -> new Object[]{a.getChangedAt() == null ? "" : a.getChangedAt().format(WHEN), a.getChangedBy(),
                        a.getStudentPrn(), a.getSectionKey(), a.getOldGrade() == null ? "-" : a.getOldGrade(),
                        a.getNewGrade()}).withFilter("Filter:");
        add(UIStyles.card("Grade audit trail", table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> reportService.getAuditLog(session), list -> {
            table.setItems(list);
            status(list.size() + " grade change(s) recorded");
        });
    }
}
