package me.geniusburger.turntracker;

import android.content.Context;
import android.content.SharedPreferences;

import me.geniusburger.turntracker.model.User;

public class Preferences {

    private static final String SHARED_PREFERENCES_FILENAME = "tt";

    private static final String KEY_SERVER_IP = "server.ip";
    private static final String DEFAULT_SERVER_IP = "192.168.2.11";

    private static final String KEY_SERVER_PORT = "server.port";
    private static final int DEFAULT_SERVER_PORT = 3000;

    private static final String KEY_USER_ID = "user.id";
    private static final long DEFAULT_USER_ID = 0;

    private static final String KEY_USER_NAME = "user.name";
    private static final String KEY_USER_DISPLAY_NAME = "user.displayname";
    private static final String DEFAULT_USER = "?";

    private SharedPreferences prefs;

    public Preferences(Context context) {
        prefs = context.getSharedPreferences(SHARED_PREFERENCES_FILENAME, Context.MODE_PRIVATE);
    }

    public String getServerIP() {
        return prefs.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP);
    }

    public int getServerPort() {
        return prefs.getInt(KEY_SERVER_PORT, DEFAULT_SERVER_PORT);
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
