package model;

/** Theory or Lab part of a course; each section teaches one component. */
public class CourseComponent {

    private final int componentId;
    private final int courseId;
    private final String componentType;
    private final double credits;

    public CourseComponent(int componentId, int courseId, String componentType, double credits) {
        this.componentId = componentId;
        this.courseId = courseId;
        this.componentType = componentType;
        this.credits = credits;
    }

    public int getComponentId()      { return componentId; }
    public int getCourseId()         { return courseId; }
    public String getComponentType() { return componentType; }
    public double getCredits()       { return credits; }

    @Override
    public String toString() {
        return componentType + " (" + credits + " cr)";
    }
}
