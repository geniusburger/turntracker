package me.geniusburger.turntracker.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Turn {

    public String date;
    public String name;
    public long userId;
    public long turnId;

    public Turn(JSONObject json) throws JSONException {
        date = json.getString("date");
        name = json.getString("name");
        userId = json.getLong("userid");
        turnId = json.getLong("turnid");
    }

    @Override
    public String toString() {
        return name + " " + date;
    }
}
