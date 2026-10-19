package ui.student;

import model.AttendanceRecord;
import model.AttendanceSummary;
import service.AttendanceService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.Locale;

/** Attendance per course, with the day-by-day record of the selected course. */
public class AttendancePage extends Page {

    private final AttendanceService attendanceService = new AttendanceService();
    private final DataTable<AttendanceSummary> summary;
    private final DataTable<AttendanceRecord> details;
    private final JProgressBar overallBar = new JProgressBar(0, 100);
    private final JLabel overallLabel = UIStyles.heading(" ");
    private final JLabel detailHeading = UIStyles.heading("Select a course to see each class");

    public AttendancePage(BaseDashboard dashboard) {
        super(dashboard);
        summary = new DataTable<>(new String[]{"Course", "Title", "Type", "Faculty", "Present", "Late", "Absent",
                "Classes", "Attendance"},
                a -> new Object[]{a.getCourseCode(), a.getCourseTitle(), a.getComponentType(), a.getFacultyName(),
                        a.getPresent(), a.getLate(), a.getAbsent(), a.getTotal(),
                        a.getTotal() == 0 ? "No classes yet" : percent(a.getPercent())});
        summary.setColumnWidths(70, 200, 60, 150, 60, 50, 60, 60, 110);
        details = new DataTable<>(new String[]{"Date", "Day", "Status"},
                r -> new Object[]{r.getClassDate(), r.getClassDate().getDayOfWeek().getDisplayName(TextStyle.FULL,
                        Locale.ENGLISH), r.getStatus()});
        summary.onSelect(a -> {
            if (a == null) {
                details.setItems(Collections.emptyList());
                return;
            }
            detailHeading.setText(a.getCourseCode() + " " + a.getComponentType() + " - class by class");
            Async.run(this, () -> attendanceService.getRecords(a.getEnrollmentId()), details::setItems);
        });

        overallBar.setStringPainted(true);
        overallBar.setPreferredSize(new Dimension(300, 22));
        JPanel overall = UIStyles.clear(new BorderLayout(14, 0));
        overall.add(overallLabel, BorderLayout.WEST);
        overall.add(overallBar, BorderLayout.CENTER);
        overall.add(UIStyles.muted("Minimum required: " + (int) AttendanceService.MIN_PERCENT + "%  (Late counts as attended)"),
                BorderLayout.EAST);

        JPanel top = UIStyles.card();
        top.add(overall, BorderLayout.NORTH);
        top.add(summary, BorderLayout.CENTER);

        JPanel detailCard = UIStyles.card();
        detailCard.add(detailHeading, BorderLayout.NORTH);
        detailCard.add(details, BorderLayout.CENTER);
        detailCard.setPreferredSize(new Dimension(10, 260));

        add(top, BorderLayout.CENTER);
        add(detailCard, BorderLayout.SOUTH);
    }

    private static String percent(double p) {
        return String.format("%.1f%%", p) + (p < AttendanceService.MIN_PERCENT ? " (low)" : "");
    }

    @Override
    public void refresh() {
        Async.run(this, () -> attendanceService.getSummaries(session.getStudent().getId()), list -> {
            summary.setItems(list);
            details.setItems(Collections.emptyList());
            double overall = attendanceService.overallPercent(list);
            overallLabel.setText(String.format("Overall %.1f%%", overall));
            overallBar.setValue((int) Math.round(overall));
            overallBar.setString(String.format("%.1f%%", overall));
            overallBar.setForeground(overall < AttendanceService.MIN_PERCENT ? UIStyles.DANGER : UIStyles.SUCCESS);
            status("Attendance loaded for " + list.size() + " course(s)");
        });
    }
}
