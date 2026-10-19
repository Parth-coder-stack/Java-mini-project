package ui.common;

import ui.Async;
import ui.BaseDashboard;
import ui.Page;
import ui.UIStyles;
import ui.components.TimetableGrid;

import java.awt.BorderLayout;
import java.util.List;

/** Weekly timetable. The caller supplies how to load the blocks (student vs faculty). */
public class TimetablePage extends Page {

    private final TimetableGrid grid = new TimetableGrid();
    private final Async.Task<List<TimetableGrid.Entry>> loader;

    public TimetablePage(BaseDashboard dashboard, Async.Task<List<TimetableGrid.Entry>> loader) {
        super(dashboard);
        this.loader = loader;
        add(UIStyles.card("This week", UIStyles.scroll(grid)), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        Async.run(this, loader, entries -> {
            grid.setEntries(entries);
            status(entries.isEmpty() ? "No classes scheduled this semester" : entries.size() + " class slot(s) this week");
        });
    }
}
