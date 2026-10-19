package exception;

/** Thrown when login fails or a user is not allowed to perform an action. */
public class AuthException extends UCMSException {

    public AuthException(String message) {
        super(message);
    }
}
