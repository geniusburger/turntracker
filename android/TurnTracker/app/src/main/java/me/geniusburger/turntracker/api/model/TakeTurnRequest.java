package me.geniusburger.turntracker.api.model;

import com.google.gson.annotations.SerializedName;

import java.util.Calendar;

import retrofit2.http.Query;

public class TakeTurnRequest {
    @SerializedName("user_id") private long userId;
    @SerializedName("task_id") private long taskId;
    private long date;

    public TakeTurnRequest(long taskId, @Query("user_id") long userId) {
        this.userId = userId;
        this.taskId = taskId;
    }

    public TakeTurnRequest(long taskId, @Query("user_id") long userId, Calendar date) {
        this.userId = userId;
        this.taskId = taskId;
        this.date = date.getTimeInMillis();
    }
}
