package me.geniusburger.turntracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import me.geniusburger.turntracker.model.User;

public class Preferences {

    public static final String KEY_SERVER_IP = "server.ip";
    private final String DEFAULT_SERVER_IP;

    public static final String KEY_SERVER_PORT = "server.port";
    private final String DEFAULT_SERVER_PORT;

    private static final String KEY_USER_ID = "user.id";
    private static final long DEFAULT_USER_ID = 0;

    private static final String KEY_USER_NAME = "user.name";
    private static final String KEY_USER_DISPLAY_NAME = "user.displayname";
    private static final String DEFAULT_USER = "?";

    private SharedPreferences prefs;

    public Preferences(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        DEFAULT_SERVER_IP = context.getString(R.string.pref_default_server_ip);
        DEFAULT_SERVER_PORT = context.getString(R.string.pref_default_server_port);
    }

    public String getServerIP() {
        return prefs.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP);
    }

    public String getServerPort() {
        return prefs.getString(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, DEFAULT_USER_ID);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, DEFAULT_USER);
    }

    public String getUserDisplayName() {
        return prefs.getString(KEY_USER_DISPLAY_NAME, DEFAULT_USER);
    }

    public void saveUser(User user) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_USER_ID, user.id);
        editor.putString(KEY_USER_NAME, user.username);
        editor.putString(KEY_USER_DISPLAY_NAME, user.displayName);
        editor.apply();
    }

    public void clearUser() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_DISPLAY_NAME);
        editor.apply();
    }
}