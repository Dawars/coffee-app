/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.dawars.coffeetracker.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GcmPubSub;

import java.io.IOException;

import me.dawars.coffeetracker.MainActivity;
import me.dawars.coffeetracker.R;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from   SenderID of the sender.
     * @param bundle Data bundle containing message bundle as key/value pairs.
     *               For Set of keys use bundle.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle bundle) {
        String message = bundle.getString("message");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int jugId = sharedPreferences.getInt(QuickstartPreferences.JUG_ID, -1);

        if (from.equals("/topics/" + jugId)) {
            if (message == null || message.equals("init")) return;

            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();

            float temp;
            float lvl;
            float pour;

            String[] data = message.split("<");
            temp = Float.parseFloat(data[0]);
            lvl = Float.parseFloat(data[1]);
            pour = Float.parseFloat(data[2]);

            float lastPour = sharedPreferences.getFloat(QuickstartPreferences.POUR, -1);
            sharedPreferences.edit().putFloat(QuickstartPreferences.POUR, pour + lastPour).apply();

            //Limit, threshold: 30, 10
            float lastTemp = sharedPreferences.getFloat(QuickstartPreferences.LAST_TEMPERATURE, -1);
            if (temp < 70 && lastTemp - temp > 10) {
                sendNotification("Kaffee is getting cold", "Your coffee is " + temp + " °C", 0);
                sharedPreferences.edit().putFloat(QuickstartPreferences.LAST_TEMPERATURE, temp).apply();

            }
            if (temp > lastTemp) {
                sharedPreferences.edit().putFloat(QuickstartPreferences.LAST_TEMPERATURE, temp).apply();
            }


            if ((int) sharedPreferences.getFloat(QuickstartPreferences.LEVEL, -1) > (int) lvl) {
                switch ((int) lvl) {
                    case 0:
                        sendNotification("You are running out of coffee", "Your Kaffee is empty", 1);
                        break;
                    case 1:
                        sendNotification("You are running out of coffee", "Your Kaffee is almost empty", 1);
                        break;
                    case 2:
                        sendNotification("You are running out of coffee", "Your Kaffee is halfway full", 1);
                        break;
                    case 3:
                        sendNotification("Your Kaffee has been refilled", "Your Kaffee is at 75%", 1);
                        break;
                }
            }
            sharedPreferences.edit().putFloat(QuickstartPreferences.TEMPERATURE, temp).apply();
            sharedPreferences.edit().putFloat(QuickstartPreferences.LEVEL, lvl).apply();

            // Notify UI that registration has completed, so the progress indicator can be hidden.
            Intent dataReceived = new Intent(QuickstartPreferences.DATA);
            LocalBroadcastManager.getInstance(this).sendBroadcast(dataReceived);
        } else {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            String token = sharedPreferences.getString(QuickstartPreferences.TOKEN, "");

            try {
                pubSub.unsubscribe(token, from);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String title, String message, int id) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0/* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
    }
}
