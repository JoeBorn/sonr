package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class StopSONR
        extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Log.e("SONR", "StopSONR");
            // LogFile.MakeLog("StopSONR sending broadcast");

            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
            mNotificationManager.cancel(SONR.SONR_ID);

            Intent i = new Intent();
            i.setAction(SONR.DISCONNECT_ACTION);
            this.sendBroadcast(i);
            super.onDestroy();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
        }
    }
}
