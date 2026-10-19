package service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 10-point grade scale used for prerequisite checks (GPA itself is computed by MySQL fn_cgpa / fn_sgpa). */
public final class GradeScale {

    private static final Map<String, Integer> POINTS = new LinkedHashMap<>();

    static {
        POINTS.put("A+", 10);
        POINTS.put("A", 9);
        POINTS.put("B+", 8);
        POINTS.put("B", 7);
        POINTS.put("C+", 6);
        POINTS.put("C", 5);
        POINTS.put("D", 4);
        POINTS.put("F", 0);
    }

    private GradeScale() {
    }

    public static List<String> grades() {
        return new ArrayList<>(POINTS.keySet());
    }

    public static boolean isValid(String grade) {
        return grade != null && POINTS.containsKey(grade);
    }

    public static int points(String grade) {
        return grade == null ? -1 : POINTS.getOrDefault(grade, -1);
    }

    public static boolean isPass(String grade) {
        return points(grade) > 0;
    }

    /** True if {@code grade} is at least {@code minimum} (e.g. B+ meets C). */
    public static boolean meets(String grade, String minimum) {
        return isPass(grade) && points(grade) >= points(minimum);
    }

    /** Returns the better of two grades (null-safe). */
    public static String best(String a, String b) {
        return points(a) >= points(b) ? a : b;
    }
}
