package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootIntentReceiver extends BroadcastReceiver {
   
   private static final String TAG = BootIntentReceiver.class.getSimpleName();

   @Override
   public void onReceive(Context context, Intent intent) {
      try {
         if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SonrLog.d(TAG, "Boot Intent Received");
            Intent i = new Intent(context, ToggleSONR.class);
            context.startService(i);
         }
      } catch (RuntimeException e) {
         SonrLog.e(TAG, "Boot Intent Received");
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
