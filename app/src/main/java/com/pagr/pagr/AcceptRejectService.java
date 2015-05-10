package com.pagr.pagr;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.pagr.backend.pagr.Pagr;
import com.pagr.backend.pagr.model.Alarm;

import java.io.IOException;

public class AcceptRejectService extends IntentService {

    private final Pagr service;

    public AcceptRejectService() {
        super(AcceptRejectService.class.getName());
        service = create();
    }

    public static Pagr create() {
        return new Pagr.Builder(
                AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null)
                .setRootUrl("https://pagrff.appspot.com/_ah/api/")
                .setGoogleClientRequestInitializer(
                        new GoogleClientRequestInitializer() {
                            @Override
                            public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest)
                                    throws IOException {
                                abstractGoogleClientRequest.setDisableGZipContent(true);
                            }
                        }
                ).build();
    }

    public static PendingIntent createIntent(Context context, Bundle extras, int req) {
        Intent intent = new Intent(context, AcceptRejectService.class);
        intent.putExtras(extras);
        return PendingIntent.getService(context, req, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String regId = getSharedPreferences(DemoActivity.class.getSimpleName(), Context.MODE_PRIVATE).getString(DemoActivity.PROPERTY_REG_ID, "");
        Log.i(AcceptRejectService.class.toString(), "accepted/rejected");
        try {
            Alarm alarm = DataUtils.alarm(intent.getExtras());
            service.alarms().postStatus(alarm.getId(), regId).execute();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(alarm.getId().intValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
