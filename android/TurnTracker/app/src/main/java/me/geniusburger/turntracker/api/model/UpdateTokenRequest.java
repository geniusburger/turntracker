package me.geniusburger.turntracker.api.model;

import com.google.gson.annotations.SerializedName;

public class UpdateTokenRequest {
    private String token;
    @SerializedName("userId") private long userId;

    public UpdateTokenRequest(){}

    public UpdateTokenRequest(String token, long userId) {
        this.token = token;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public long getUserId() {
        return userId;
    }
}
