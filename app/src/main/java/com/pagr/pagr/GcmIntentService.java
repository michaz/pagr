/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pagr.pagr;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.pagr.backend.pagr.model.Alarm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {

    public GcmIntentService() {
        super("GcmIntentService");
    }

    public static final String TAG = "GCM Demo";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Intent localBroadcast = new Intent(DemoActivity.ACTION);
                LocalBroadcastManager.getInstance(this).sendBroadcast(localBroadcast);
                if (extras.getString("alarmId") != null) {
                    sendAlarmNotification(extras);
                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendAlarmNotification(Bundle msg) {

        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, DemoActivity.class);
        intent.putExtras(msg);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent1 = new Intent(this, CallActivity.class);
        intent1.putExtras(msg);
        PendingIntent callIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent acceptIntent = AcceptRejectService.createIntent(this, msg, 0);
        PendingIntent rejectIntent = AcceptRejectService.createIntent(this, msg, 1);

        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        Alarm alarm = DataUtils.alarm(msg);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setColor(getResources().getColor(R.color.leuchtrot))
                        .setSound(ringtone)
                        .setVibrate(new long[]{500, 500})
                        .setSmallIcon(R.drawable.ic_stat_gcm)
                        .setContentTitle(getString(R.string.alarm))
                        .setContentText(alarm.getMessage())
                        .addAction(R.drawable.ic_action_accept, getString(R.string.ichkomme), acceptIntent)
                        .addAction(R.drawable.ic_action_cancel, getString(R.string.ichkommenicht), rejectIntent)
                        .setContentIntent(contentIntent)
                        .setFullScreenIntent(callIntent, true)
                        .setAutoCancel(true);
        mNotificationManager.notify(alarm.getId().intValue(), mBuilder.build());
    }

}
