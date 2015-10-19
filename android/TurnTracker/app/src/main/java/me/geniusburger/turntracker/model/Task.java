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
}
