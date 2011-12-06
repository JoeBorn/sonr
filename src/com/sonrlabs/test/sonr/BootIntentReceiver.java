package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootIntentReceiver
        extends BroadcastReceiver {
    private static final String TAG = "BootIntentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                /* LogFile.MakeLog("Boot Intent Received"); */
                Log.d(TAG, "Boot Intent Received");
                // Intent i = new Intent(context, BootIntentHandler.class);
                // context.startActivity(i);
                if (!ToggleSONR.SERVICE_ON) {
                    Intent i = new Intent(context, ToggleSONR.class);
                    context.startService(i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);

        }
    }
}
