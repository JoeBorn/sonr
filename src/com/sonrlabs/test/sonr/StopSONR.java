package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class StopSONR extends Activity {
   
   private static final String TAG = StopSONR.class.getSimpleName();
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
         Log.e(TAG, TAG);
         // LogFile.MakeLog("StopSONR sending broadcast");

         String ns = Context.NOTIFICATION_SERVICE;
         NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
         mNotificationManager.cancel(SONR.SONR_ID);

         Intent i = new Intent(SONR.DISCONNECT_ACTION);
         sendBroadcast(i);
         finish();
         
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
