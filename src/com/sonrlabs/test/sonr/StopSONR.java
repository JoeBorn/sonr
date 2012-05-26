package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

public class StopSONR extends Activity {
   
   private static final String TAG = StopSONR.class.getSimpleName();
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
         SonrLog.d(TAG, TAG);
         // LogFile.MakeLog("StopSONR sending broadcast");

         NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
         mNotificationManager.cancel(SonrService.SONR_ID);

         sendBroadcast(new Intent(SonrActivity.DISCONNECT_ACTION));
         finish();
         
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
