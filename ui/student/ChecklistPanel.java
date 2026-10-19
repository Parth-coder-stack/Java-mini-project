package ui.student;

import model.EligibilityReport.Check;
import ui.UIStyles;
import ui.components.StatusIcon;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

/** Vertical list of eligibility checks, each with a tick/cross icon and an explanation. */
public class ChecklistPanel extends JPanel {

    public ChecklistPanel() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void setChecks(List<Check> checks, String emptyText) {
        removeAll();
        if (checks.isEmpty()) {
            add(row(StatusIcon.OK, emptyText, null));
        }
        for (Check c : checks) {
            StatusIcon icon = c.isPassed() ? StatusIcon.OK : c.isBlocking() ? StatusIcon.FAIL : StatusIcon.WARN;
            add(row(icon, c.getCategory() + ":  " + c.getLabel(), c.getDetail()));
        }
        revalidate();
        repaint();
    }

    private static JPanel row(StatusIcon icon, String title, String detail) {
        JPanel row = UIStyles.clear(new BorderLayout(10, 0));
        row.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setVerticalAlignment(JLabel.TOP);
        row.add(iconLabel, BorderLayout.WEST);
        JPanel text = UIStyles.clear(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UIStyles.label(title, UIStyles.BOLD, UIStyles.TEXT));
        if (detail != null) {
            text.add(UIStyles.muted(detail));
        }
        row.add(text, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
