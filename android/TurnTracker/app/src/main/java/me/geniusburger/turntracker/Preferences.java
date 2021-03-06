package me.geniusburger.turntracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Arrays;

import me.geniusburger.turntracker.model.User;

public class Preferences {

    private static final String TAG = Preferences.class.getSimpleName();

    public static final String KEY_VERSION_CODE = "version.code";
    public static final String KEY_NOTIFICATION_SNOOZE = "notification.snooze";
    public static final String KEY_ANDROID_TOKEN = "android.token";
    public static final String KEY_ANDROID_TOKEN_SENT_TO_SERVER = "android.token.senttoserver";
    public static final String KEY_ANDROID_TOKEN_RETRY = "android.token.retry";
    public static final String KEY_ANDROID_TOKEN_REFRESH = "android.token.refresh";
    public static final String ANDROID_REGISTRATION_COMPLETE = "android.registration.complete";
    public static final String ANDROID_REGISTRATION_COMPLETE_ERROR = "android.registration.complete.error";

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
    private Context context;

    public Preferences(Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        DEFAULT_SERVER_IP = context.getString(R.string.pref_default_server_ip);
        DEFAULT_SERVER_PORT = context.getString(R.string.pref_default_server_port);
    }

    public String getAndroidToken() {
        return prefs.getString(KEY_ANDROID_TOKEN, "");
    }

    public void setAndroidToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ANDROID_TOKEN, token);
        editor.apply();
    }

    public boolean getAndroidTokenSentToServer() {
        return prefs.getBoolean(KEY_ANDROID_TOKEN_SENT_TO_SERVER, false);
    }

    public void setAndroidTokenSentToServer(boolean sent) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_ANDROID_TOKEN_SENT_TO_SERVER, sent);
        editor.apply();
    }

    public int getNotificationSnoozeMilliseconds() {
        return Integer.parseInt(getKeyNotificationSnoozeMillisecondsString());
    }

    private String getKeyNotificationSnoozeMillisecondsString() {
        return prefs.getString(KEY_NOTIFICATION_SNOOZE, context.getString(R.string.snoozeDurationDefault));
    }

    public String getNotificationSnoozeLabel(String millis) {
        String[] allMillis = context.getResources().getStringArray(R.array.snoozeDurationMilliseconds);
        int index = Arrays.asList(allMillis).indexOf(millis);
        String[] allLabels = context.getResources().getStringArray(R.array.snoozeDurationLabels);
        if(index >= 0 && index < allLabels.length) {
            return allLabels[index];
        }
        return "Snooze";
    }

    public String getNotificationSnoozeLabel() {
        return getNotificationSnoozeLabel(getKeyNotificationSnoozeMillisecondsString());
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
        if(user.token == null) {
            editor.putBoolean(KEY_ANDROID_TOKEN_SENT_TO_SERVER, false);
        }
        editor.apply();
    }

    public void clearUser() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_USER_DISPLAY_NAME);
        editor.apply();
    }

    private void saveAppVersionCode(int code) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_VERSION_CODE, code);
        editor.apply();
    }

    public boolean IsNewAppVersion() {
        try {
            int newCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            int lastCode = prefs.getInt(KEY_VERSION_CODE, 0);
            if(lastCode < newCode) {
                Log.i(TAG, "Version code updating from " + lastCode + " to " + newCode);
                saveAppVersionCode(newCode);
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to check app version", e);
        }
        return false;
    }
}
