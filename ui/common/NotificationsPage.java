package ui.common;

import model.Notification;
import service.NotificationService;
import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.DataTable;

import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;

/**
 * Messages for the logged-in user: registration result, waitlist promotion, grade posted,
 * assignment reviewed / submitted. Opening the page marks them as read.
 */
public class NotificationsPage extends Page {

    public static final String TITLE = "Notifications";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final NotificationService notificationService = new NotificationService();
    private final DataTable<Notification> table;

    public NotificationsPage(BaseDashboard dashboard) {
        super(dashboard);
        table = new DataTable<Notification>(new String[]{"", "When", "Message"},
                n -> new Object[]{n.isRead() ? "" : "New", n.getCreatedAt() == null ? "" : n.getCreatedAt().format(WHEN),
                        n.getMessage()}).withFilter("Filter:");
        table.setColumnWidths(50, 150, 900);
        add(UIStyles.card("Your notifications", table), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, () -> {
            java.util.List<Notification> list = notificationService.getMine(session);
            notificationService.markAllRead(session);
            return list;
        }, list -> {
            table.setItems(list);
            dashboard.setNavBadge(TITLE, 0);
            status(list.size() + " notification(s)");
        });
    }
}
