package me.geniusburger.turntracker.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {

    public long creatorUserID;
    public int methodID;
    public boolean notification;
    public int periodicHours;
    public int reasonID;
    public boolean reminder;
    public long id;
    public String name;

    public Task(JSONObject json) throws JSONException {
        creatorUserID = json.getLong("creator_user_id");
        methodID = json.optInt("method_id", 0);
        notification = 0 != json.getInt("notification");
        periodicHours = json.getInt("periodic_hours");
        reasonID = json.optInt("reason_id", 0);
        reminder = 0 != json.optInt("reminder", 0);
        id = json.getLong("taskId");
        name = json.getString("taskName");
    }

    public Task(long id, String name, int periodicHours, long creatorUserID) {
        this.id = id;
        this.name = name;
        this.periodicHours = periodicHours;
        this.creatorUserID = creatorUserID;
    }

    public Task(Task task) {
        update(task);
    }

    public void update(Task task) {
        this.creatorUserID = task.creatorUserID;
        this.methodID = task.methodID;
        this.notification = task.notification;
        this.periodicHours = task.periodicHours;
        this.reasonID = task.reasonID;
        this.reminder = task.reminder;
        this.id = task.id;
        this.name = task.name;
    }

    @Override
    public String toString() {
        return name;
    }
}
