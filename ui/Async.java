package ui;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs database work on a background thread with {@link SwingWorker} so the
 * window never freezes, then hands the result back on the Swing (EDT) thread.
 */
public final class Async {

    /** Work that may throw (e.g. a service call). */
    @FunctionalInterface
    public interface Task<T> {
        T call() throws Exception;
    }

    /** Work with no result. */
    @FunctionalInterface
    public interface VoidTask {
        void run() throws Exception;
    }

    private Async() {
    }

    public static <T> void run(Component owner, Task<T> task, Consumer<T> onSuccess) {
        run(owner, task, onSuccess, null);
    }

    /** Runs work that returns nothing (save, delete, ...). */
    public static void exec(Component owner, VoidTask task, Runnable onSuccess) {
        run(owner, () -> {
            task.run();
            return null;
        }, ignored -> onSuccess.run(), null);
    }

    /**
     * @param onError optional; when null the error is shown in a dialog
     */
    public static <T> void run(Component owner, Task<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        Window window = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                if (window != null) {
                    window.setCursor(Cursor.getDefaultCursor());
                }
                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Exception cause = e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
                    if (onError != null) {
                        onError.accept(cause);
                    } else {
                        Dialogs.error(owner, cause);
                    }
                }
            }
        }.execute();
    }
}
