package me.geniusburger.turntracker.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Turn {

    private static SimpleDateFormat inputFormat;
    private static SimpleDateFormat outputFormat;

    static {
        inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        outputFormat = new SimpleDateFormat("EEE MMM d, yyyy - h:mm a", Locale.US);
        outputFormat.setTimeZone(TimeZone.getDefault());
    }

    public Date date;
    public String name;
    public long userId;
    public long turnId;

    public Turn(JSONObject json) throws JSONException, ParseException {
        date = inputFormat.parse(json.getString("date"));

        name = json.getString("name");
        userId = json.getLong("userid");
        turnId = json.getLong("turnid");
    }

    public String getDateString() {
        return outputFormat.format(date);
    }

    @Override
    public String toString() {
        return name + " - " + outputFormat.format(date);
    }
}
