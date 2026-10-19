package ui.admin;

import ui.Async;
import ui.UIStyles;
import ui.components.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/** Small modal "form + Save/Cancel" dialog. Save runs in the background; errors keep the dialog open. */
public class FormDialog extends JDialog {

    private boolean saved;

    public FormDialog(JFrame owner, String title, JPanel form, Async.VoidTask save) {
        super(owner, title, true);
        FlatButton ok = UIStyles.primary("Save");
        FlatButton cancel = UIStyles.secondary("Cancel");
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> Async.exec(this, save, () -> {
            saved = true;
            dispose();
        }));
        JPanel buttons = UIStyles.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(cancel);
        buttons.add(ok);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(UIStyles.CARD);
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setSize(Math.max(getWidth(), 560), getHeight());
        setLocationRelativeTo(owner);
    }

    /** @return true if the user saved successfully */
    public boolean showDialog() {
        setVisible(true);
        return saved;
    }
}
