package service;

import db.dao.NotificationDAO;
import exception.UCMSException;
import model.Notification;

import java.util.List;

/** The logged-in user's notifications. */
public class NotificationService {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    public List<Notification> getMine(Session s) throws UCMSException {
        return notificationDAO.findForUser(s.getAccount().getUserId());
    }

    public int unreadCount(Session s) throws UCMSException {
        return notificationDAO.countUnread(s.getAccount().getUserId());
    }

    public void markAllRead(Session s) throws UCMSException {
        notificationDAO.markAllRead(s.getAccount().getUserId());
    }
}
