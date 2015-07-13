package com.pagr.pagr;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationResult;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.pagr.backend.pagr.Pagr;
import com.pagr.backend.pagr.model.CellUpdate;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class LocationUpdateIntentService extends IntentService {


    private TelephonyManager tm;
    private GoogleApiClient mGoogleApiClient;
    private Pagr pagr;

    public LocationUpdateIntentService() {
        super("LocationUpdateIntentService");

    }

    @Override
    public void onCreate() {
        super.onCreate();
        pagr = new Pagr.Builder(
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
        tm = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
        if (tm == null) {
            Log.e("LocationUpdate", "onStart: tm == null");
        } else {
            Log.i("LocationUpdate", "onStart: tm initialized");
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        LocationResult locationResult = LocationResult.extractResult(intent);
        List<CellInfo> allCellInfo = tm.getAllCellInfo();
        if (allCellInfo != null) { // happens!
            for (CellInfo cellInfo : allCellInfo) {
                Log.i("allCellInfo", cellInfo.toString());
                if (cellInfo.isRegistered()) {
                    if (cellInfo instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                        CellUpdate cellUpdate = new CellUpdate();
                        cellUpdate.setTimestamp(Calendar.getInstance().getTimeInMillis());
                        cellUpdate.setDeviceId(tm.getDeviceId());
                        cellUpdate.setCi(cellInfoLte.getCellIdentity().getCi());
                        cellUpdate.setMcc(cellInfoLte.getCellIdentity().getMcc());
                        cellUpdate.setMnc(cellInfoLte.getCellIdentity().getMnc());
                        cellUpdate.setPci(cellInfoLte.getCellIdentity().getPci());
                        cellUpdate.setTac(cellInfoLte.getCellIdentity().getTac());
                        if (locationResult != null) {
                            Location lastLocation = locationResult.getLastLocation();
                            if (lastLocation != null) {
                                cellUpdate.setLatitude(lastLocation.getLatitude());
                                cellUpdate.setLongitude(lastLocation.getLongitude());
                            }
                        }
                        try {
                            pagr.cellupdate().post(cellUpdate).execute();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        List<NeighboringCellInfo> neighboringCellInfo = tm.getNeighboringCellInfo();
        if (neighboringCellInfo != null) {
            for (NeighboringCellInfo cellInfo : neighboringCellInfo) {
                Log.i("neighboringCellInfo", cellInfo.toString());
            }
        }
    }

}
