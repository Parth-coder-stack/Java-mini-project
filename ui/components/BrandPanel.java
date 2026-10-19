package ui.components;

import ui.UIStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** Left-hand branding column on the login and register screens. */
public class BrandPanel extends GradientPanel {

    public BrandPanel(String heading, String... points) {
        super(null, new Color(67, 56, 202), new Color(30, 27, 75));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(60, 50, 40, 50));
        setPreferredSize(new Dimension(460, 700));

        add(white("UCMS", UIStyles.font(Font.BOLD, 46)));
        add(white("University Course Management System", UIStyles.H3));
        add(Box.createVerticalStrut(50));
        add(white(heading, UIStyles.H2));
        add(Box.createVerticalStrut(18));
        for (String p : points) {
            JPanel row = new JPanel();
            row.setOpaque(false);
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(new JLabel(StatusIcon.OK));
            row.add(Box.createHorizontalStrut(12));
            row.add(white(p, UIStyles.BODY));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            add(row);
            add(Box.createVerticalStrut(14));
        }
        add(Box.createVerticalGlue());
        JLabel footer = white("Java Swing  -  Service layer  -  JDBC  -  MySQL", UIStyles.SMALL);
        footer.setForeground(new Color(199, 210, 254));
        add(footer);
    }

    private static JLabel white(String text, Font font) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(Color.WHITE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
