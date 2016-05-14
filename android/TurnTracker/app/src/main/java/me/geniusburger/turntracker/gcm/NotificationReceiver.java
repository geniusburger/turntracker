package me.geniusburger.turntracker.gcm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import me.geniusburger.turntracker.MainActivity;
import me.geniusburger.turntracker.Preferences;
import me.geniusburger.turntracker.R;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

    public static final String ACTION_DISMISS = "dismiss";
    public static final String ACTION_SNOOZE = "snooze-start";
    public static final String ACTION_NOTIFY = "snooze-end";
    public static final String EXTRA_MESSAGE = "message";

    @Override
    public void onReceive(Context context, Intent intent) {
        Preferences prefs = new Preferences(context);
        long userId = prefs.getUserId();
        long taskId = intent.getLongExtra(MainActivity.EXTRA_TASK_ID, 0);
        String action = intent.getAction();
        Log.d(TAG, "received action " + action + " for task " + taskId);
        if(taskId <= 0) {
            Log.e(TAG, "received intent for " + action + " missing task ID");
            return;
        }

        switch (action) {
            case ACTION_SNOOZE: // fall-through
                snooze(context, intent.getExtras(), taskId, prefs.getNotificationSnoozeMilliseconds());
            case ACTION_DISMISS:
                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) taskId);
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

    private void snooze(Context context, Bundle extras, long taskId, int milliseconds) {
        PendingIntent pendingIntent = createAlarmIntent(context, extras, taskId);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + milliseconds, pendingIntent);
    }

    private static void cancelSnoozedNotifications(Context context, long taskId) {
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(createAlarmIntent(context, null, taskId));
    }

    public static void sendNotification(Context context, String message, long taskId, long userId) {

        cancelSnoozedNotifications(context, taskId);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(MainActivity.EXTRA_TASK_ID, taskId);
        intent.putExtra(MainActivity.EXTRA_USER_ID, userId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.putExtras(intent.getExtras());
        snoozeIntent.putExtra(EXTRA_MESSAGE, message);
        snoozeIntent.setAction(ACTION_SNOOZE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent dismissIntent = new Intent(context, NotificationReceiver.class);
        dismissIntent.putExtras(intent.getExtras());
        dismissIntent.setAction(ACTION_DISMISS);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .addAction(R.drawable.ic_notifications_paused_24dp, "Snooze", snoozePendingIntent)
                .addAction(R.drawable.ic_clear_24dp, "Dismiss", dismissPendingIntent);

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify((int) taskId, notificationBuilder.build());
    }

    public static void updateNotification(Context context, int id) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] notes = nm.getActiveNotifications();
        for (StatusBarNotification wrapper : notes) {
            Notification note = wrapper.getNotification();
            for( int i = 0; i < note.actions.length; i++) {
                Notification.Action action = note.actions[i];
                Bundle extras = action.getExtras();
                for(String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d(TAG, wrapper.getId() + " - " + key + ": " + value);
                }
            }
        }
    }
}
