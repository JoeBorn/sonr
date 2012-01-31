package com.sonrlabs.test.sonr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This receiver intercepts non specific button events from other apps.
 * 
 * Fix for the Last person dialed and Play/Pause behavior on the Moto. Atrix.
 * 
 */
public class MediaButtonReceiver extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      abortBroadcast();
   }
}
