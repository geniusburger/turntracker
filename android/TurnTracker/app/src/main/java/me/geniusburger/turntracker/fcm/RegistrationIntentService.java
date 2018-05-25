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

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

import me.geniusburger.turntracker.Api;
import me.geniusburger.turntracker.Preferences;
import me.geniusburger.turntracker.utilities.ToastUtils;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = RegistrationIntentService.class.getSimpleName();
    public static final String KEY_REFRESH_TOKEN = "RefreshToken";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Preferences prefs = new Preferences(getApplicationContext());

        try {
            boolean refresh = intent.getBooleanExtra(KEY_REFRESH_TOKEN, false);
            FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance();
            if(refresh) {
                try {
                    //ToastUtils.showToastOnUiThread(this, "Deleting instance ID", Toast.LENGTH_SHORT);
                    instanceId.deleteInstanceId();
                } catch(IOException e) {
                    ToastUtils.showToastOnUiThread(this, "Failed to delete instance ID", Toast.LENGTH_SHORT, TAG, Log::e);
                }
                instanceId = FirebaseInstanceId.getInstance();
            }
            String token = instanceId.getToken();

            if(token == null || token.length() == 0) {
                throw new Exception("Token is blank");
            }

            Log.i(TAG, "token: " + token);
            if(refresh) {
                String displayToken = token;
                if (displayToken.length() > 10) {
                    displayToken = displayToken.substring(0, 10) + "...";
                }
                ToastUtils.showToastOnUiThread(this, "FCM Registration Token: " + displayToken, Toast.LENGTH_LONG);
            }

            prefs.setAndroidToken(token);
            prefs.setAndroidTokenSentToServer(false);
            sendRegistrationToServer(token, prefs);

            // Subscribe to topic channels
            subscribeTopics(token);
        } catch (Exception e) {
            Log.e(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            prefs.setAndroidTokenSentToServer(false);
            notifyUI(e);
        }
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token, Preferences prefs) {
        if(!prefs.getAndroidTokenSentToServer()) {
            boolean success = new Api(getApplicationContext()).updateToken(token);
            prefs.setAndroidTokenSentToServer(success);
            notifyUI(null);
        }
    }

    private void notifyUI(Exception e) {
        Intent registrationComplete = new Intent(Preferences.ANDROID_REGISTRATION_COMPLETE);
        if(e != null) {
            registrationComplete.putExtra(Preferences.ANDROID_REGISTRATION_COMPLETE_ERROR, e.getMessage());
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void subscribeTopics(String topic) throws IOException {
        //FirebaseMessaging.getInstance().subscribeToTopic(topic);
    }
}
