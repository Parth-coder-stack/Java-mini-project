package ui.components;

import ui.UIStyles;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/** Dashboard tile: big number + caption, with a coloured accent stripe. */
public class StatCard extends JPanel {

    private final JLabel value;
    private final JLabel caption;

    public StatCard(String captionText, Color accent) {
        super(new BorderLayout());
        setBackground(UIStyles.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIStyles.BORDER, 1, true),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                        BorderFactory.createEmptyBorder(14, 16, 14, 16))));
        JPanel text = UIStyles.clear(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        value = UIStyles.label("-", UIStyles.H1, UIStyles.TEXT);
        caption = UIStyles.muted(captionText);
        text.add(value);
        text.add(caption);
        add(text, BorderLayout.CENTER);
        setPreferredSize(new Dimension(180, 92));
    }

    public void setValue(String text) {
        value.setText(text);
    }

    public void setValue(String text, Color color) {
        value.setText(text);
        value.setForeground(color);
    }

    public void setCaption(String text) {
        caption.setText(text);
    }
}
