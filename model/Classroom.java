package model;

public class Classroom {

    private final int roomId;
    private final String roomNumber;
    private final String roomType;
    private final int capacity;

    public Classroom(int roomId, String roomNumber, String roomType, int capacity) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
    }

    public int getRoomId()        { return roomId; }
    public String getRoomNumber() { return roomNumber; }
    public String getRoomType()   { return roomType; }
    public int getCapacity()      { return capacity; }

    @Override
    public String toString() {
        return roomNumber + " (" + roomType + ", " + capacity + " seats)";
    }
}
