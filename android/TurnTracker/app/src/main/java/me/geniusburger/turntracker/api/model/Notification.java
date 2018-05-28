package me.geniusburger.turntracker.api.model;

import com.google.gson.annotations.SerializedName;

import me.geniusburger.turntracker.model.Task;

public class Notification {
    @SerializedName("reason_id") private int reasonId;
    @SerializedName("method_id") private int methodId;
    private int reminder;

    public Notification(Task task) {
        reasonId = task.reasonID;
        methodId = task.methodID;
        reminder = task.reminder ? 1 : 0;
    }
}
