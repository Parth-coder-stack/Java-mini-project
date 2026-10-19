package ui;

import exception.AuthException;
import exception.DBException;
import exception.EnrollmentException;
import exception.UCMSException;
import exception.ValidationException;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Consistent message boxes. Picks a title based on the exception type. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(Component owner, Throwable e) {
        String title;
        if (e instanceof EnrollmentException) {
            title = "Cannot register";
        } else if (e instanceof ValidationException) {
            title = "Please check your input";
        } else if (e instanceof AuthException) {
            title = "Not allowed";
        } else if (e instanceof DBException) {
            title = "Database problem";
        } else if (e instanceof UCMSException) {
            title = "Error";
        } else {
            title = "Unexpected error";
            e.printStackTrace();
        }
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        JOptionPane.showMessageDialog(owner, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component owner, String title, String message) {
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void warn(Component owner, String title, String message) {
        JOptionPane.showMessageDialog(owner, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static boolean confirm(Component owner, String title, String message) {
        return JOptionPane.showConfirmDialog(owner, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
}
