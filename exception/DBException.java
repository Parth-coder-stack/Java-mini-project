package exception;

/**
 * Wraps a JDBC {@link java.sql.SQLException} so that the service and UI
 * layers never depend on java.sql directly.
 */
public class DBException extends UCMSException {

    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }
}
