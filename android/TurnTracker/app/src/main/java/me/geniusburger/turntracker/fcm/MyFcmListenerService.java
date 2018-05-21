/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.geniusburger.turntracker.fcm;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import me.geniusburger.turntracker.Preferences;
import me.geniusburger.turntracker.utilities.ToastUtils;

public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = MyFcmListenerService.class.getSimpleName();

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        String from = remoteMessage.getFrom();
        Log.d(TAG, "From: " + from);

        // Check if message contains a data payload.
        Map<String,String> data = remoteMessage.getData();
        if(data.isEmpty())
        {
            String msg = "received empty notifcation";
            Log.w(TAG, msg);
            ToastUtils.showToastOnUiThread(this, msg, Toast.LENGTH_LONG);
            return;
        }

        final String message = data.getOrDefault("message", "??????");
        final long taskId = Long.parseLong(data.getOrDefault("taskId", "0"));
        final long userId = Long.parseLong(data.getOrDefault("userId", "0"));
        final boolean test = Boolean.parseBoolean(data.getOrDefault("test", "false"));

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "TaskId: " + taskId);
        Log.d(TAG, "UserId: " + userId);
        Log.d(TAG, "test: " + test);

        long myUserId = new Preferences(getApplicationContext()).getUserId();
        if(userId != myUserId) {
            // TODO ignore other IDs for now
            String msg = "ignoring message for wrong user id " + userId + " instead of " + myUserId;
            Log.w(TAG, msg);
            ToastUtils.showToastOnUiThread(this, msg, Toast.LENGTH_LONG);
            return;
        }

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        NotificationReceiver.sendNotification(this, message, taskId, userId, test);
    }
    // [END receive_message]
}
