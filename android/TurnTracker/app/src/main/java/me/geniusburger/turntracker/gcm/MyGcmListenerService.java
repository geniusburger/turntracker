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

package me.geniusburger.turntracker.gcm;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;

import me.geniusburger.turntracker.Preferences;
import me.geniusburger.turntracker.utilities.ToastUtils;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = MyGcmListenerService.class.getSimpleName();

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        final String message = data.getString("message");
        final long taskId = Long.parseLong(data.getString("taskId", "0"));
        final long userId = Long.parseLong(data.getString("userId", "0"));
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "TaskId: " + taskId);
        Log.d(TAG, "UserId: " + userId);

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

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        NotificationReceiver.sendNotification(this, message, taskId, userId);
        // [END_EXCLUDE]
    }
    // [END receive_message]
}
