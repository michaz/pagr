package com.pagr.pagr;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class DetectedActivitiesIntentService extends IntentService {

    public DetectedActivitiesIntentService() {
        super("DetectedActivitiesIntentService");
    }

    private CellIdService cellIdService;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CellIdService.LocalBinder binder = (CellIdService.LocalBinder) service;
            cellIdService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            cellIdService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Bind to LocalService
        Intent i = new Intent(this, CellIdService.class);
        bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (cellIdService == null) {
            Log.e("DetectedActivities", "Service disconnected!");
            // Strange. Apparently, we have already been destroyed, but still get to handle
            // intents. How can I prevent this?
            return;
        }
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Log.i("DetectedActivities", result.toString());
        if (result.getActivityConfidence(DetectedActivity.STILL) < 20) {
            cellIdService.turnOnLocationUpdates();
        } else if (result.getActivityConfidence(DetectedActivity.STILL) > 80) {
            cellIdService.turnOffLocationUpdates();
        }
    }

}
