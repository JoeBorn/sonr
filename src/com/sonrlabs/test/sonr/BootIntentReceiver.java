package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

class BootIntentReceiver
      extends BroadcastReceiver {
   
   private static final String TAG = BootIntentReceiver.class.getSimpleName();

   @Override
   public void onReceive(Context context, Intent intent) {
      try {
         if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot Intent Received");
            // Intent i = new Intent(context, BootIntentHandler.class);
            // context.startActivity(i);
            if (!ToggleSONR.SERVICE_ON) {
               Intent i = new Intent(context, ToggleSONR.class);
               context.startService(i);
            }
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);

      }
   }
}
