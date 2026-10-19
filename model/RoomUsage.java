package model;

/** How much a classroom is used this semester. */
public class RoomUsage {

    /** Mon-Sat, 08:00-18:00. */
    public static final double AVAILABLE_HOURS_PER_WEEK = 60;

    private final Classroom room;
    private final int sections;
    private final double bookedHours;
    private final int studentsSeated;
    private final int seatsOffered;

    public RoomUsage(Classroom room, int sections, double bookedHours, int studentsSeated, int seatsOffered) {
        this.room = room;
        this.sections = sections;
        this.bookedHours = bookedHours;
        this.studentsSeated = studentsSeated;
        this.seatsOffered = seatsOffered;
    }

    public Classroom getRoom()     { return room; }
    public int getSections()       { return sections; }
    public double getBookedHours() { return bookedHours; }
    public int getStudentsSeated() { return studentsSeated; }
    public int getSeatsOffered()   { return seatsOffered; }

    /** Share of the teaching week the room is booked. */
    public int getTimeUtilization() {
        return (int) Math.round(bookedHours * 100 / AVAILABLE_HOURS_PER_WEEK);
    }

    /** Share of offered seats that are taken. */
    public int getSeatUtilization() {
        return seatsOffered == 0 ? 0 : studentsSeated * 100 / seatsOffered;
    }
}
