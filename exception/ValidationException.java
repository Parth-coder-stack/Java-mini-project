package exception;

/** Thrown when user input breaks a validation rule (empty field, bad PRN, ...). */
public class ValidationException extends UCMSException {

    public ValidationException(String message) {
        super(message);
    }
}
