package ui.components;

import ui.UIStyles;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Round badge with a person's initials. */
public class Avatar extends JComponent {

    private final String initials;
    private final Color color;
    private final int size;

    public Avatar(String name, int size) {
        this.size = size;
        StringBuilder sb = new StringBuilder();
        for (String part : name.replace("Dr.", "").replace("Prof.", "").trim().split("\\s+")) {
            if (!part.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        this.initials = sb.toString();
        this.color = UIStyles.PALETTE.get(Math.floorMod(name.hashCode(), UIStyles.PALETTE.size()));
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, size, size);
        g2.setColor(Color.WHITE);
        g2.setFont(UIStyles.font(java.awt.Font.BOLD, size * 0.4f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initials, (size - fm.stringWidth(initials)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
