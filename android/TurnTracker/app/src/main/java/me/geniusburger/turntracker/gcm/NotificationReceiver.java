package me.geniusburger.turntracker.gcm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import me.geniusburger.turntracker.MainActivity;
import me.geniusburger.turntracker.Preferences;
import me.geniusburger.turntracker.R;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();
    private static final String TAG_REMINDER = "reminder";

    public static final String ACTION_DISMISS = "dismiss";
    public static final String ACTION_SNOOZE = "snooze-start";
    public static final String ACTION_NOTIFY = "snooze-end";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    public void onReceive(Context context, Intent intent) {
        Preferences prefs = new Preferences(context);
        boolean test = intent.getBooleanExtra(MainActivity.EXTRA_TEST, false);
        long userId = prefs.getUserId();
        long taskId = intent.getLongExtra(MainActivity.EXTRA_TASK_ID, 0);
        String action = intent.getAction();
        Log.d(TAG, "received action " + action + " for task " + taskId);
        if(taskId <= 0 && !test) {
            Log.e(TAG, "received intent for " + action + " missing task ID");
            return;
        }

        switch (action) {
            case ACTION_SNOOZE: // fall-through
                snooze(context, intent.getExtras(), taskId, prefs.getNotificationSnoozeMilliseconds(), prefs.getNotificationSnoozeLabel());
            case ACTION_DISMISS:
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(TAG_REMINDER, (int) taskId);
                break;
            case ACTION_NOTIFY:
                sendNotification(context, intent.getStringExtra(EXTRA_MESSAGE), taskId, userId);
                break;
            default:
                Log.e(TAG, "unhandled action " + action);
                break;
        }

    }

    private static PendingIntent createAlarmIntent(Context context, Bundle extras, long taskId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        if(extras != null) {
            intent.putExtras(extras);
        }
        intent.addCategory(String.valueOf(taskId));
        intent.setAction(ACTION_NOTIFY);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void snooze(Context context, Bundle extras, long taskId, int milliseconds, String description) {
        PendingIntent pendingIntent = createAlarmIntent(context, extras, taskId);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + milliseconds, pendingIntent);
        Toast.makeText(context, "Snoozing " + description, Toast.LENGTH_SHORT).show();
    }

    private static void cancelSnoozedNotifications(Context context, long taskId) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createAlarmIntent(context, null, taskId));
    }

    public static void sendNotification(Context context, String message, long taskId, long userId) {
        sendNotification(context, message, taskId, userId, null, false);
    }

    public static void sendNotification(Context context, String message, long taskId, long userId, boolean test) {
        sendNotification(context, message, taskId, userId, null, test);
    }

    public static void sendNotification(Context context, String message, long taskId, long userId, String snoozeLabel) {
        sendNotification(context, message, taskId, userId, snoozeLabel, false);
    }

    public static void sendNotification(Context context, String message, long taskId, long userId, String snoozeLabel, boolean test) {

        Preferences prefs = new Preferences(context);
        if(snoozeLabel == null) {
            snoozeLabel = prefs.getNotificationSnoozeLabel();
        } else {
            snoozeLabel = prefs.getNotificationSnoozeLabel(snoozeLabel);
        }
        Log.d(TAG, "Building notification: '" + message + "', task: " + taskId + ", user: " + userId + ", snooze: " + snoozeLabel + ", test: " + test);

        cancelSnoozedNotifications(context, taskId);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(MainActivity.EXTRA_TASK_ID, taskId);
        intent.putExtra(MainActivity.EXTRA_USER_ID, userId);
        intent.putExtra(MainActivity.EXTRA_TEST, test);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.putExtras(intent.getExtras());
        snoozeIntent.setAction(ACTION_SNOOZE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.putExtras(intent.getExtras());
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

//        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher, );
//        Log.d(TAG, "Bitmap size: " + bitmap.getAllocationByteCount());

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                //.setLargeIcon(bitmap)
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setExtras(intent.getExtras())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .addAction(R.drawable.ic_notifications_paused_24dp, snoozeLabel, snoozePendingIntent)
                .addAction(R.drawable.ic_clear_24dp, "Dismiss", dismissPendingIntent);

        if(!test) {
            notificationBuilder.setOngoing(true);
        }

        Notification note = notificationBuilder.build();
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(TAG_REMINDER, (int) taskId, note);
    }

    public static void updateNotifications(Context context, String updatedSnoozeLabel) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notes = nm.getActiveNotifications();
        for (StatusBarNotification wrapper : notes) {
            if(TAG_REMINDER.equals(wrapper.getTag())) {
                Bundle extras = wrapper.getNotification().extras;
                String message = extras.getString(EXTRA_MESSAGE);
                long userId = extras.getLong(MainActivity.EXTRA_USER_ID);
                long taskId = extras.getLong(MainActivity.EXTRA_TASK_ID);
                boolean test = extras.getBoolean(MainActivity.EXTRA_TEST);
                if (message != null) {
                    Log.d(TAG, "Updating notification " + wrapper.getId());
                    sendNotification(context, message, taskId, userId, updatedSnoozeLabel, test);
                }
            }
        }
    }
}
