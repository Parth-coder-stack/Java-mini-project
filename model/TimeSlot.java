package model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parsed form of a section slot string such as {@code "Mon/Wed 09:00-10:30"}.
 * Used for timetable clash detection and for drawing the weekly timetable.
 */
public class TimeSlot {

    private static final Map<String, DayOfWeek> DAY_NAMES = new LinkedHashMap<>();

    static {
        DAY_NAMES.put("MON", DayOfWeek.MONDAY);
        DAY_NAMES.put("TUE", DayOfWeek.TUESDAY);
        DAY_NAMES.put("WED", DayOfWeek.WEDNESDAY);
        DAY_NAMES.put("THU", DayOfWeek.THURSDAY);
        DAY_NAMES.put("FRI", DayOfWeek.FRIDAY);
        DAY_NAMES.put("SAT", DayOfWeek.SATURDAY);
        DAY_NAMES.put("SUN", DayOfWeek.SUNDAY);
    }

    private final Set<DayOfWeek> days;
    private final LocalTime start;
    private final LocalTime end;

    private TimeSlot(Set<DayOfWeek> days, LocalTime start, LocalTime end) {
        this.days = days;
        this.start = start;
        this.end = end;
    }

    /**
     * Parses "Mon/Wed 09:00-10:30".
     *
     * @throws IllegalArgumentException if the text is not in that format
     */
    public static TimeSlot parse(String text) {
        try {
            String[] parts = text.trim().split("\\s+");
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            for (String d : parts[0].split("/")) {
                DayOfWeek day = DAY_NAMES.get(d.trim().toUpperCase().substring(0, 3));
                if (day == null) {
                    throw new IllegalArgumentException("Unknown day " + d);
                }
                days.add(day);
            }
            String[] times = parts[1].split("-");
            LocalTime start = LocalTime.parse(times[0].trim());
            LocalTime end = LocalTime.parse(times[1].trim());
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("End time must be after start time");
            }
            return new TimeSlot(days, start, end);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Slot must look like \"Mon/Wed 09:00-10:30\", got \"" + text + "\"");
        }
    }

    /** True if both slots share a day and their time ranges overlap. */
    public boolean overlaps(TimeSlot other) {
        if (Collections.disjoint(days, other.days)) {
            return false;
        }
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** Class hours per week: number of days x length of one class. */
    public double getWeeklyHours() {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        return days.size() * minutes / 60.0;
    }

    public Set<DayOfWeek> getDays() { return Collections.unmodifiableSet(days); }
    public LocalTime getStart()     { return start; }
    public LocalTime getEnd()       { return end; }
}
