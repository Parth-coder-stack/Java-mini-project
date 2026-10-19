package service;

import exception.ValidationException;

import java.util.regex.Pattern;

/** Input validation rules shared by the services. */
public final class Validator {

    /** PRN format, e.g. 25070122259. Change here if your university uses another format. */
    public static final Pattern PRN = Pattern.compile("\\d{11}");
    public static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    public static final Pattern PHONE = Pattern.compile("\\d{10}");
    public static final Pattern COURSE_CODE = Pattern.compile("[A-Z]{2,4}\\d{3}");

    private Validator() {
    }

    public static String required(String value, String field) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String prn(String value) throws ValidationException {
        String v = required(value, "PRN");
        if (!PRN.matcher(v).matches()) {
            throw new ValidationException("PRN must be exactly 11 digits (e.g. 25070122259).");
        }
        return v;
    }

    public static String email(String value) throws ValidationException {
        String v = required(value, "Email").toLowerCase();
        if (!EMAIL.matcher(v).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        return v;
    }

    public static String phone(String value) throws ValidationException {
        String v = required(value, "Phone number").replaceAll("[\\s-]", "");
        if (!PHONE.matcher(v).matches()) {
            throw new ValidationException("Phone number must be 10 digits.");
        }
        return v;
    }

    public static boolean isValidPhone(String value) {
        return value != null && PHONE.matcher(value.trim()).matches();
    }

    public static String address(String value) throws ValidationException {
        String v = required(value, "Address");
        if (v.length() < 5) {
            throw new ValidationException("Please enter a complete address.");
        }
        return v;
    }

    public static String password(String password, String confirm) throws ValidationException {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters.");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ValidationException("Password must contain at least one letter and one digit.");
        }
        if (!password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
        return password;
    }

    public static int positiveInt(String value, String field) throws ValidationException {
        try {
            int n = Integer.parseInt(required(value, field));
            if (n <= 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a positive whole number.");
        }
    }

    public static double nonNegativeDouble(String value, String field) throws ValidationException {
        try {
            double d = Double.parseDouble(required(value, field));
            if (d < 0) {
                throw new NumberFormatException();
            }
            return d;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number >= 0.");
        }
    }
}
