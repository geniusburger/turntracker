package me.geniusburger.turntracker.model;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

public class User {

    public long id;
    public String username;
    @SerializedName("name")  public String displayName;
    public String token;
    public int turns;
    private int mobile;
    public int consecutiveTurns;
    public boolean selected;

    public User(long id) {
        this.id = id;
    }

    public User(long id, String username, String displayName, String token) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.token = "null".equals(token) ? null : token;
    }

    public User(long id, String displayName, boolean selected) {
        this.id = id;
        this.displayName = displayName;
        this.selected = selected;
    }

    public User(JSONObject json) throws JSONException {
        id = json.getLong("id");
        displayName = json.getString("name");
        turns = json.getInt("turns");
        mobile = json.getInt("mobile");
    }

    public boolean isMobile() {
        return mobile != 0;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
