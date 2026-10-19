package model;

import java.time.LocalDate;

public class Semester {

    private final int semesterId;
    private final String name;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDate dropDeadline;

    public Semester(int semesterId, String name, LocalDate startDate, LocalDate endDate, LocalDate dropDeadline) {
        this.semesterId = semesterId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dropDeadline = dropDeadline;
    }

    public int getSemesterId()        { return semesterId; }
    public String getName()           { return name; }
    public LocalDate getStartDate()   { return startDate; }
    public LocalDate getEndDate()     { return endDate; }
    public LocalDate getDropDeadline() { return dropDeadline; }

    @Override
    public String toString() {
        return name;
    }
}
