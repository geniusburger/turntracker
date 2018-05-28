package me.geniusburger.turntracker.api.model;

import me.geniusburger.turntracker.model.Task;

public class NotificationRequest {
    private long taskId;
    private long userId;
    private Notification note;

    public NotificationRequest(Task task, long userId) {
        taskId = task.id;
        this.userId = userId;
        note = new Notification(task);
    }
}
