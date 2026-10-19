package ui;

import service.Session;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Base class for every dashboard page. {@link #refresh()} is called each time
 * the page is opened so it always shows fresh data from the database.
 */
public abstract class Page extends JPanel {

    protected final BaseDashboard dashboard;
    protected final Session session;

    protected Page(BaseDashboard dashboard) {
        super(new BorderLayout(16, 16));
        this.dashboard = dashboard;
        this.session = dashboard.getSession();
        setBackground(UIStyles.BG);
        setBorder(BorderFactory.createEmptyBorder(4, 24, 20, 24));
    }

    /** Reloads the page's data (in the background). */
    public abstract void refresh();

    protected void status(String message) {
        dashboard.setStatus(message);
    }
}
