package com.pagr.pagr;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.pagr.backend.pagr.Pagr;
import com.pagr.backend.pagr.model.CellUpdate;

import java.io.IOException;
import java.util.List;

public class CellIdService extends Service implements GoogleApiClient.ConnectionCallbacks {

    private PhoneStateListener listener;

    private TelephonyManager tm;
    private Pagr service;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        service = new Pagr.Builder(
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
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        tm = (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);
        listener = new PhoneStateListener() {
            @Override
            public void onCellInfoChanged(List<CellInfo> cellInfo) {
                dump();
            }
        };
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        tm.listen(listener, 0);
    }

    private void dump() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i("wurst", "wurst");
                Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                        mGoogleApiClient);
                List<CellInfo> allCellInfo = tm.getAllCellInfo();
                if (allCellInfo != null) { // happens!
                    for (CellInfo cellInfo : allCellInfo) {
                        Log.i("allCellInfo", cellInfo.toString());
                        if (cellInfo.isRegistered()) {
                            if (cellInfo instanceof CellInfoLte) {
                                CellInfoLte cellInfoLte = (CellInfoLte) cellInfo;
                                CellUpdate cellUpdate = new CellUpdate();
                                cellUpdate.setTimestamp(cellInfoLte.getTimeStamp());
                                cellUpdate.setCi(cellInfoLte.getCellIdentity().getCi());
                                cellUpdate.setMcc(cellInfoLte.getCellIdentity().getMcc());
                                cellUpdate.setMnc(cellInfoLte.getCellIdentity().getMnc());
                                cellUpdate.setPci(cellInfoLte.getCellIdentity().getPci());
                                cellUpdate.setTac(cellInfoLte.getCellIdentity().getTac());
                                if (lastLocation != null) {
                                    cellUpdate.setLatitude(lastLocation.getLatitude());
                                    cellUpdate.setLongitude(lastLocation.getLongitude());
                                }
                                try {
                                    service.cellupdate().post(cellUpdate).execute();
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
        }).start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        tm.listen(listener, PhoneStateListener.LISTEN_CELL_INFO);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
