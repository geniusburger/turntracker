package me.geniusburger.turntracker.gcm;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import me.geniusburger.turntracker.MainActivity;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(MainActivity.EXTRA_TASK_ID, 0);
        Log.d(TAG, "received action " + intent.getAction() + " for task " + taskId);
        if(taskId > 0) {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel((int) taskId);
        }
    }
}
