package ui.components;

import ui.UIStyles;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/** Builds vertical forms: each row holds one or more equally wide cells. */
public class FormBuilder {

    private final JPanel panel = UIStyles.clear(new GridBagLayout());
    private int row;

    /** A caption above a field. */
    public static JPanel labeled(String caption, JComponent field) {
        JPanel p = UIStyles.clear(new BorderLayout(0, 5));
        p.add(UIStyles.label(caption, UIStyles.BOLD, UIStyles.TEXT), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    public FormBuilder row(JComponent... cells) {
        JComponent content;
        if (cells.length == 1) {
            content = cells[0];
        } else {
            JPanel grid = UIStyles.clear(new GridLayout(1, cells.length, 14, 0));
            for (JComponent c : cells) {
                grid.add(c);
            }
            content = grid;
        }
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = row++;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 12, 0);
        panel.add(content, gc);
        return this;
    }

    public FormBuilder field(String caption, JComponent field) {
        return row(labeled(caption, field));
    }

    public FormBuilder gap(int height) {
        return row((JComponent) Box.createVerticalStrut(height));
    }

    public JPanel build() {
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        panel.add(Box.createVerticalGlue(), filler);
        return panel;
    }
}
