package com.pagr.pagr;

import android.os.Bundle;

import com.pagr.backend.pagr.model.Alarm;

public class DataUtils {

    static Alarm alarm(Bundle msg) {
        Alarm alarm = new Alarm();
        Long alarmId = Long.parseLong(msg.getString("alarmId"));
        alarm.setId(alarmId);
        String message = msg.getString("message");
        alarm.setMessage(message);
        return alarm;
    }

    static Bundle bundle(Alarm alarm) {
        Bundle bundle = new Bundle();
        bundle.putString("alarmId", Long.toString(alarm.getId()));
        bundle.putString("message", alarm.getMessage());
        return bundle;
    }

}
