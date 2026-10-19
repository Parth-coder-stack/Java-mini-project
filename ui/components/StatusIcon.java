package ui.components;

import ui.UIStyles;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Painted tick / cross / warning circle, so it renders even if a font lacks the glyphs. */
public class StatusIcon implements Icon {

    public enum Kind { OK, FAIL, WARN }

    public static final StatusIcon OK = new StatusIcon(Kind.OK);
    public static final StatusIcon FAIL = new StatusIcon(Kind.FAIL);
    public static final StatusIcon WARN = new StatusIcon(Kind.WARN);

    private static final int SIZE = 18;
    private final Kind kind;

    private StatusIcon(Kind kind) {
        this.kind = kind;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = kind == Kind.OK ? UIStyles.SUCCESS : kind == Kind.FAIL ? UIStyles.DANGER : UIStyles.WARNING;
        g2.setColor(color);
        g2.fillOval(x, y, SIZE, SIZE);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (kind == Kind.OK) {
            g2.drawPolyline(new int[]{x + 5, x + 8, x + 13}, new int[]{y + 9, y + 12, y + 6}, 3);
        } else if (kind == Kind.FAIL) {
            g2.drawLine(x + 6, y + 6, x + 12, y + 12);
            g2.drawLine(x + 12, y + 6, x + 6, y + 12);
        } else {
            g2.drawLine(x + 9, y + 5, x + 9, y + 10);
            g2.fillOval(x + 8, y + 12, 3, 3);
        }
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
