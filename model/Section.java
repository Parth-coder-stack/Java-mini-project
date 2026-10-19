package model;

/** A scheduled offering of one course component, taught by one faculty in one room. */
public class Section {

    private final int sectionId;
    private final int courseId;
    private final String courseCode;
    private final String courseTitle;
    private final int componentId;
    private final String componentType;
    private final double credits;
    private final int facultyId;
    private final String facultyName;
    private final int roomId;
    private final String roomNumber;
    private final String semesterName;
    private final String sectionNumber;
    private final int maxCapacity;
    private final int enrolledCount;
    private final String slot;

    public Section(int sectionId, int courseId, String courseCode, String courseTitle, int componentId,
                   String componentType, double credits, int facultyId, String facultyName,
                   int roomId, String roomNumber, String semesterName, String sectionNumber,
                   int maxCapacity, int enrolledCount, String slot) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.componentId = componentId;
        this.componentType = componentType;
        this.credits = credits;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.semesterName = semesterName;
        this.sectionNumber = sectionNumber;
        this.maxCapacity = maxCapacity;
        this.enrolledCount = enrolledCount;
        this.slot = slot;
    }

    public int getSectionId()        { return sectionId; }
    public int getCourseId()         { return courseId; }
    public String getCourseCode()    { return courseCode; }
    public String getCourseTitle()   { return courseTitle; }
    public int getComponentId()      { return componentId; }
    public String getComponentType() { return componentType; }
    public double getCredits()       { return credits; }
    public int getFacultyId()        { return facultyId; }
    public String getFacultyName()   { return facultyName; }
    public int getRoomId()           { return roomId; }
    public String getRoomNumber()    { return roomNumber; }
    public String getSemesterName()  { return semesterName; }
    public String getSectionNumber() { return sectionNumber; }
    public int getMaxCapacity()      { return maxCapacity; }
    public int getEnrolledCount()    { return enrolledCount; }
    public String getSlot()          { return slot; }

    public int getSeatsLeft() {
        return Math.max(0, maxCapacity - enrolledCount);
    }

    public boolean isFull() {
        return enrolledCount >= maxCapacity;
    }

    /** Percentage of seats taken. */
    public int getFillPercent() {
        return maxCapacity == 0 ? 0 : enrolledCount * 100 / maxCapacity;
    }

    /** Teaching hours per week, worked out from the slot (e.g. Mon/Wed 09:00-10:30 = 3.0). */
    public double getWeeklyHours() {
        try {
            return TimeSlot.parse(slot).getWeeklyHours();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /** e.g. "CS201-Theory-A" - used as a readable key in the audit log. */
    public String getKey() {
        return courseCode + "-" + componentType + "-" + sectionNumber;
    }

    @Override
    public String toString() {
        return courseCode + " " + componentType + " Sec " + sectionNumber + "  |  " + slot
                + "  |  " + facultyName + "  |  " + getSeatsLeft() + "/" + maxCapacity + " seats left";
    }
}
