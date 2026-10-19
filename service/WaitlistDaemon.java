package service;

import exception.UCMSException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Background thread (Multithreading rubric) that checks every 30 seconds
 * whether a seat has opened in a full section and promotes the next student.
 */
public final class WaitlistDaemon {

    private static final long PERIOD_SECONDS = 30;
    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private static ScheduledExecutorService executor;

    private WaitlistDaemon() {
    }

    public static synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "waitlist-daemon");
            t.setDaemon(true);
            return t;
        });
        EnrollmentService service = new EnrollmentService();
        executor.scheduleWithFixedDelay(() -> {
            try {
                for (String msg : service.promoteAll()) {
                    for (Consumer<String> l : listeners) {
                        l.accept(msg);
                    }
                }
            } catch (UCMSException | RuntimeException e) {
                System.err.println("[waitlist-daemon] " + e.getMessage());
            }
        }, PERIOD_SECONDS, PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Listener is called on the daemon thread; UI code must hop to the EDT itself. */
    public static void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}
