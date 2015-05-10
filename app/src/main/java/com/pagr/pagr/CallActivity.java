package com.pagr.pagr;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.pagr.backend.pagr.model.Alarm;

public class CallActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        final Alarm alarm = DataUtils.alarm(getIntent().getExtras());
        ((TextView) findViewById(R.id.alarmlistitem_message)).setText(alarm.getMessage());
        Button accept = (Button) findViewById(R.id.alarmlistitem_acceptButton);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AcceptRejectService.createIntent(CallActivity.this, DataUtils.bundle(alarm), 0).send();
                    finish();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        });
        Button reject = (Button) findViewById(R.id.alarmlistitem_rejectButton);
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AcceptRejectService.createIntent(CallActivity.this, DataUtils.bundle(alarm), 0).send();
                    finish();
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
