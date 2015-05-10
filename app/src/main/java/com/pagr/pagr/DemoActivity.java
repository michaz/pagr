/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pagr.pagr;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.pagr.backend.pagr.Pagr;
import com.pagr.backend.pagr.model.Alarm;
import com.pagr.backend.pagr.model.RegistrationRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main UI for the demo app.
 */
public class DemoActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener {

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String ACTION = "wurst";

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "439566031252";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Demo";

    GoogleCloudMessaging gcm;
    Context context;

    String regId;
    ListView alarmList;
    private SwipeRefreshLayout swipeLayout;
    private BroadcastReceiver updateList = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            swipeLayout.setRefreshing(true);
            onRefresh();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.list_alarms_swipecontainer);
        swipeLayout.setOnRefreshListener(this);
        context = getApplicationContext();
        register();
        alarmList = (ListView) findViewById(R.id.list_alarms);
        swipeLayout.setRefreshing(true);
        onRefresh();
    }

    private void register() {
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId(context);
            if (regId.isEmpty()) {
                registerInBackground();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        swipeLayout.setRefreshing(true);
        onRefresh();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(updateList, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateList);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences();
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regId;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regId);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

        }.execute(null, null, null);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private SharedPreferences getGcmPreferences() {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(DemoActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app.
     */
    private void sendRegistrationIdToBackend() {
        new ServerUtilities().register(getApplicationContext(), regId);
    }

    @Override
    public void onRefresh() {
        new ListRetriever().execute();
    }

    private class ListRetriever extends AsyncTask<Void, Void, List<Alarm>> {

        @Override
        protected List<Alarm> doInBackground(Void... params) {
            Pagr messaging = AcceptRejectService.create();
            try {
                return messaging.alarms().list().execute().getItems();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        @SuppressWarnings("null")
        protected void onPostExecute(List<Alarm> result) {
            alarmList.setAdapter(createListAdapter(result));
            swipeLayout.setRefreshing(false);
        }

        private ListAdapter createListAdapter(List<Alarm> result) {
            Collections.sort(result, new Comparator<Alarm>() {
                @Override
                public int compare(Alarm lhs, Alarm rhs) {
                    return - Long.compare(lhs.getAlarmDate().getValue(), rhs.getAlarmDate().getValue());
                }
            });
            List<Map<String,Object>> data = new ArrayList<>();
            for (Alarm alarm : result) {
                Map<String,Object> element = new HashMap<>();
                element.put("message", alarm.getMessage());
                element.put("alarmDate", alarm.getAlarmDate());
                element.put("pendingReplies", alarm);
                data.add(element);
            }
            SimpleAdapter adapter = new SimpleAdapter(DemoActivity.this, data, R.layout.alarmlistitem,
                    new String[]{"alarmDate", "message", "pendingReplies"},
                    new int[]{R.id.alarmlistitem_date, R.id.alarmlistitem_message, R.id.alarmlistitem_buttonbar});
            adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Object data, String textRepresentation) {
                    if (view.getId() == R.id.alarmlistitem_buttonbar) {
                        final Alarm alarm = (Alarm) data;
                        if (!isPendingForMe(alarm)) {
                            view.setVisibility(View.GONE);
                        } else {
                            view.setVisibility(View.VISIBLE);
                            Button accept = (Button) view.findViewById(R.id.alarmlistitem_acceptButton);
                            accept.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        AcceptRejectService.createIntent(DemoActivity.this, DataUtils.bundle(alarm), 0).send();
                                    } catch (PendingIntent.CanceledException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            Button reject = (Button) view.findViewById(R.id.alarmlistitem_rejectButton);
                            reject.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        AcceptRejectService.createIntent(DemoActivity.this, DataUtils.bundle(alarm), 0).send();
                                    } catch (PendingIntent.CanceledException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        return true;
                    }
                    return false;
                }

                private boolean isPendingForMe(Alarm alarm) {
                    if (alarm.getFullPendingReplies() == null) {
                        return false;
                    }
                    for (RegistrationRecord record : alarm.getFullPendingReplies()) {
                        if (regId.equals(record.getRegId())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            return adapter;
        }
    }
}
