package model;

import java.time.LocalDateTime;

/** A message for one user (waitlist promotion, grade posted, registration result, ...). */
public class Notification {

    private final int notificationId;
    private final String message;
    private final LocalDateTime createdAt;
    private final boolean read;

    public Notification(int notificationId, String message, LocalDateTime createdAt, boolean read) {
        this.notificationId = notificationId;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getNotificationId()     { return notificationId; }
    public String getMessage()         { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead()            { return read; }
}
