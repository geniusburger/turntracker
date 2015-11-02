package me.geniusburger.turntracker.model;

import org.json.JSONException;
import org.json.JSONObject;

public class User {

    public long id;
    public String username;
    public String displayName;
    public int turns;
    public boolean mobile;
    public int consecutiveTurns;

    public User(long id) {
        this.id = id;
    }

    public User(long id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    public User(JSONObject json) throws JSONException {
        id = json.getLong("id");
        displayName = json.getString("name");
        turns = json.getInt("turns");
        mobile = 0 != json.getInt("mobile");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(displayName);
        if(consecutiveTurns > 0) {
            sb.append(" (x");
            sb.append(consecutiveTurns);
            sb.append(")");
        }
        if(mobile) {
            sb.append(" M");
        }
        return sb.toString();
    }
}
