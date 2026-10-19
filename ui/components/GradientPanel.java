package ui.components;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** Panel with a diagonal gradient and soft decorative circles (login/register branding side). */
public class GradientPanel extends JPanel {

    private final Color from;
    private final Color to;

    public GradientPanel(LayoutManager layout, Color from, Color to) {
        super(layout);
        this.from = from;
        this.to = to;
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, from, getWidth(), getHeight(), to));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillOval(getWidth() - 260, -120, 380, 380);
        g2.fillOval(-140, getHeight() - 220, 320, 320);
        g2.dispose();
    }
}
