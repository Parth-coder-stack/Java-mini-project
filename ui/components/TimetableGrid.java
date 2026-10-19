package ui.components;

import model.TimeSlot;
import ui.UIStyles;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Weekly timetable (Mon-Sat, 08:00-18:00) drawn as coloured blocks. */
public class TimetableGrid extends JPanel {

    /** One block on the grid. */
    public static class Entry {
        final String slot;
        final String title;
        final String subtitle;

        public Entry(String slot, String title, String subtitle) {
            this.slot = slot;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static final int FIRST_HOUR = 8;
    private static final int LAST_HOUR = 18;
    private static final int GUTTER = 60;
    private static final int HEADER = 34;
    private static final int HOUR_HEIGHT = 58;
    private static final DayOfWeek[] DAYS = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, Color> colors = new LinkedHashMap<>();

    public TimetableGrid() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, HEADER + (LAST_HOUR - FIRST_HOUR) * HOUR_HEIGHT + 2));
    }

    public void setEntries(List<Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        colors.clear();
        for (Entry e : entries) {
            String key = e.title.split(" ")[0];
            colors.putIfAbsent(key, UIStyles.PALETTE.get(colors.size() % UIStyles.PALETTE.size()));
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int colWidth = (getWidth() - GUTTER) / DAYS.length;

        // header + vertical lines
        g2.setFont(UIStyles.BOLD);
        for (int d = 0; d < DAYS.length; d++) {
            int x = GUTTER + d * colWidth;
            g2.setColor(new Color(249, 250, 251));
            g2.fillRect(x, 0, colWidth, HEADER);
            g2.setColor(UIStyles.TEXT);
            String name = DAYS[d].getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            g2.drawString(name, x + (colWidth - g2.getFontMetrics().stringWidth(name)) / 2, 22);
            g2.setColor(UIStyles.BORDER);
            g2.drawLine(x, 0, x, getHeight());
        }
        // hour lines
        g2.setFont(UIStyles.SMALL);
        for (int h = FIRST_HOUR; h <= LAST_HOUR; h++) {
            int y = HEADER + (h - FIRST_HOUR) * HOUR_HEIGHT;
            g2.setColor(UIStyles.BORDER);
            g2.drawLine(GUTTER, y, getWidth(), y);
            g2.setColor(UIStyles.MUTED);
            g2.drawString(String.format("%02d:00", h), 10, y + 4);
        }

        for (Entry e : entries) {
            TimeSlot slot;
            try {
                slot = TimeSlot.parse(e.slot);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Color color = colors.get(e.title.split(" ")[0]);
            int y1 = yFor(slot.getStart());
            int y2 = yFor(slot.getEnd());
            for (int d = 0; d < DAYS.length; d++) {
                if (!slot.getDays().contains(DAYS[d])) {
                    continue;
                }
                int x = GUTTER + d * colWidth + 3;
                int w = colWidth - 6;
                int h = Math.max(20, y2 - y1 - 2);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                g2.fillRoundRect(x, y1 + 1, w, h, 8, 8);
                g2.setColor(color);
                g2.fillRoundRect(x, y1 + 1, 4, h, 4, 4);
                g2.setFont(UIStyles.BOLD);
                g2.setColor(UIStyles.TEXT);
                drawClipped(g2, e.title, x + 10, y1 + 18, w - 14);
                g2.setFont(UIStyles.SMALL);
                g2.setColor(UIStyles.MUTED);
                drawClipped(g2, e.subtitle, x + 10, y1 + 34, w - 14);
                drawClipped(g2, e.slot.substring(e.slot.indexOf(' ') + 1), x + 10, y1 + 49, w - 14);
            }
        }
        g2.dispose();
    }

    private int yFor(LocalTime t) {
        int minutes = (t.getHour() - FIRST_HOUR) * 60 + t.getMinute();
        return HEADER + minutes * HOUR_HEIGHT / 60;
    }

    private static void drawClipped(Graphics2D g2, String text, int x, int y, int maxWidth) {
        if (text == null) {
            return;
        }
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) {
            g2.drawString(text, x, y);
            return;
        }
        String s = text;
        while (!s.isEmpty() && fm.stringWidth(s + "...") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        g2.drawString(s + "...", x, y);
    }
}
