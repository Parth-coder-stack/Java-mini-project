package ui.components;

import ui.UIStyles;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Rounded, flat button with a hover colour. Paints itself so it looks the same on every OS. */
public class FlatButton extends JButton {

    private final Color base;
    private final Color hover;
    private final Color outline;
    private boolean hovering;

    public FlatButton(String text, Color base, Color hover, Color foreground) {
        this(text, base, hover, foreground, null);
    }

    public FlatButton(String text, Color base, Color hover, Color foreground, Color outline) {
        super(text);
        this.base = base;
        this.hover = hover;
        this.outline = outline;
        setUI(new BasicButtonUI());
        setForeground(foreground);
        setFont(UIStyles.BOLD);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = hovering && isEnabled() ? hover : base;
        if (fill != null) {
            g2.setColor(isEnabled() ? fill : new Color(209, 213, 219));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        }
        if (outline != null) {
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
