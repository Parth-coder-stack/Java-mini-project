package exception;

/**
 * Base checked exception for every UCMS error. All other custom
 * exceptions extend this class, so the UI can catch one type.
 */
public class UCMSException extends Exception {

    public UCMSException(String message) {
        super(message);
    }

    public UCMSException(String message, Throwable cause) {
        super(message, cause);
    }
}
