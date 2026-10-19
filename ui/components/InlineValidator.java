package ui.components;

import exception.ValidationException;
import ui.UIStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Inline (as-you-type) validation: shows a red message under a field the moment its
 * value becomes invalid, and a green tick once it is valid. The rules themselves are the
 * same {@link service.Validator} rules the service layer enforces on submit.
 */
public class InlineValidator {

    /** A validation rule; throw ValidationException with the message to show. */
    @FunctionalInterface
    public interface Rule {
        void check(String value) throws ValidationException;
    }

    private static class Entry {
        final JTextComponent field;
        final JLabel message;
        final Rule rule;
        boolean touched;

        Entry(JTextComponent field, JLabel message, Rule rule) {
            this.field = field;
            this.message = message;
            this.rule = rule;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    /** Returns a panel: caption, field, and a message line that updates while typing. */
    public JPanel field(String caption, JTextComponent field, Rule rule) {
        JLabel message = UIStyles.label(" ", UIStyles.SMALL, UIStyles.DANGER);
        Entry entry = new Entry(field, message, rule);
        entries.add(entry);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { touch(entry); }
            @Override public void removeUpdate(DocumentEvent e)  { touch(entry); }
            @Override public void changedUpdate(DocumentEvent e) { touch(entry); }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                touch(entry);
            }
        });
        JPanel p = FormBuilder.labeled(caption, field);
        p.add(message, BorderLayout.SOUTH);
        return p;
    }

    private void touch(Entry e) {
        e.touched = true;
        validate(e);
    }

    private boolean validate(Entry e) {
        String error = null;
        try {
            e.rule.check(e.field.getText());
        } catch (ValidationException ex) {
            error = ex.getMessage();
        }
        if (!e.touched) {
            return error == null;
        }
        if (error == null) {
            e.message.setIcon(StatusIcon.OK);
            e.message.setForeground(UIStyles.SUCCESS);
            e.message.setText("Looks good");
            e.field.setBorder(border(UIStyles.BORDER));
        } else {
            e.message.setIcon(null);
            e.message.setForeground(UIStyles.DANGER);
            e.message.setText(error);
            e.field.setBorder(border(UIStyles.DANGER));
        }
        return error == null;
    }

    /** Checks every field (showing all messages); true if all are valid. */
    public boolean validateAll() {
        boolean ok = true;
        for (Entry e : entries) {
            e.touched = true;
            ok &= validate(e);
        }
        return ok;
    }

    /** Clears messages, e.g. when the form is reset. */
    public void reset() {
        for (Entry e : entries) {
            e.touched = false;
            e.message.setIcon(null);
            e.message.setText(" ");
            e.field.setBorder(border(UIStyles.BORDER));
        }
    }

    private static javax.swing.border.Border border(java.awt.Color color) {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }
}
