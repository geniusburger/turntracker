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

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import me.geniusburger.turntracker.Api;
import me.geniusburger.turntracker.Preferences;
import me.geniusburger.turntracker.R;
import me.geniusburger.turntracker.utilities.ToastUtils;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = RegistrationIntentService.class.getSimpleName();
    private static final String[] TOPICS = {};//{"global"};
    public static final String KEY_REFRESH_TOKEN = "RefreshToken";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Preferences prefs = new Preferences(getApplicationContext());

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            boolean refresh = intent.getBooleanExtra(KEY_REFRESH_TOKEN, false);
            if(refresh) {
                try {
                    ToastUtils.showToastOnUiThread(this, "Deleting instance ID", Toast.LENGTH_SHORT);
                    instanceID.deleteInstanceID();
                } catch(IOException e) {
                    String msg = "Failed to delete instance ID";
                    Log.e(TAG, msg, e);
                    ToastUtils.showToastOnUiThread(this, msg, Toast.LENGTH_SHORT);
                }
                instanceID = InstanceID.getInstance(this);
            }
            String token = instanceID.getToken(
                    getResources().getString(R.string.SENDER_ID),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);
            if(refresh) {
                String displayToken = token;
                if (displayToken.length() > 10) {
                    displayToken = displayToken.substring(0, 10) + "...";
                }
                ToastUtils.showToastOnUiThread(this, "GCM Registration Token: " + displayToken, Toast.LENGTH_LONG);
                prefs.setAndroidTokenSentToServer(false);
            }

            prefs.setAndroidToken(token);
            sendRegistrationToServer(token, prefs);

            // Subscribe to topic channels
            subscribeTopics(token);
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
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

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

}
