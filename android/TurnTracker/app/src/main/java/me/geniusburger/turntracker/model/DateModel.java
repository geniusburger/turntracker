package me.geniusburger.turntracker.model;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class DateModel {

    protected static SimpleDateFormat inputFormat;
    protected static SimpleDateFormat outputFormat;

    static {
        inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        outputFormat = new SimpleDateFormat("EEE MMM d, yyyy   h:mm a", Locale.US);
        outputFormat.setTimeZone(TimeZone.getDefault());
    }
}
