package me.geniusburger.turntracker.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

public class Turn extends DateModel {

    private static final long MS_PRE_DATED_THRESHOLD = 3600000;

    public Date date;
    public Date added;
    public String name;
    public long userId;
    public long turnId;
    public boolean preDated;

    public Turn(JSONObject json) throws JSONException, ParseException {
        date = inputFormat.parse(json.getString("date"));
        added = inputFormat.parse(json.getString("inserted"));
        preDated = added.getTime() - date.getTime() > MS_PRE_DATED_THRESHOLD;

        name = json.getString("name");
        userId = json.getLong("userid");
        turnId = json.getLong("turnid");
    }

    public String getDateString() {
        return outputFormat.format(date);
    }
    public String getAddedString() {
        return outputFormat.format(added);
    }

    @Override
    public String toString() {
        return name + " - " + outputFormat.format(date);
    }
}
