package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.util.Log;

class SONRClient {

   private final static String TAG = SONRClient.class.getSimpleName();
   
   private MicSerialListener singletonListener;

   private final AudioManager theAudioManager;
   private final Context applicationContext;

   private boolean clientStopRegistered;
   
   private final BroadcastReceiver clientStopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         // Handle receiver
         String mAction = intent.getAction();
         if (SONR.DISCONNECT_ACTION.equals(mAction)) {
            onDestroy();
         }
      }
   };


   SONRClient(Context applicationContext, AudioManager am) {
      this.theAudioManager = am;
      this.applicationContext = applicationContext;
      this.clientStopRegistered = false;
   }

   boolean foundDock() {
      return singletonListener != null && singletonListener.foundDock();
   }

   void searchSignal() {
      singletonListener.searchSignal();
   }

   void startListener() {
      if (!singletonListener.isAlive()) {
         // LogFile.MakeLog("Start Listener");
         try {
            MicSerialListener.startNewListener(singletonListener);
         } catch (RuntimeException e) {
            e.printStackTrace();
            //ErrorReporter.getInstance().handleException(e);
         }
      }
   }

   public void createListener() {
      try {
         synchronized (this) {
            // LogFile.MakeLog("\n\nSONRClient CREATED");
            unregisterReceiver();
            registerReceiver();
            IUserActionHandler controller = new UserActionHandler(theAudioManager, applicationContext);
            AudioProcessorQueue.setUserActionHandler(controller);
            if (singletonListener != null) {
               singletonListener.stopRunning();
            }
            singletonListener = new MicSerialListener();
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }


   public void onDestroy() {
      try {
         synchronized (this) {
            unregisterReceiver();
            if (singletonListener != null) {
               singletonListener.stopRunning();
            }
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }

   private void registerReceiver() {
      applicationContext.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
      clientStopRegistered = true;
      Log.i(TAG, "Registered broadcast receiver " + clientStopReceiver + " in context " + applicationContext);
   }

   private void unregisterReceiver() {
      try {
         if (clientStopRegistered) {
            applicationContext.unregisterReceiver(clientStopReceiver);
            clientStopRegistered = false;
            Log.i(TAG, "Unregistered broadcast receiver " + clientStopReceiver + " in context " + applicationContext);
         }
      } catch (RuntimeException e) {
         Log.i(TAG, "Failed to unregister broadcast receiver " + clientStopReceiver + " in context " + applicationContext, e);
      }
   }
}
