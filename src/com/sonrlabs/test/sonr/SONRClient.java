package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import com.sonrlabs.test.sonr.common.Common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.util.Log;

public class SONRClient {

   private final static String TAG = SONRClient.class.getSimpleName();
   
   /*
    * This is a static because we can create multiple clients but we only want one listener.
    * Possibly the creation of multiple clients is incorrect?
    * 
    */
   private static MicSerialListener singletonListener;


   private final AudioManager theAudioManager;
   private final AudioRecord theaudiorecord;
   private final int bufferSize;
   private final Context applicationContext;

   private BroadcastReceiver clientStopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         // Handle receiver
         String mAction = intent.getAction();
         if (SONR.DISCONNECT_ACTION.equals(mAction)) {
            onDestroy();
         }
      }
   };


   SONRClient(Context applicationContext, AudioRecord ar, AudioManager am) {
      theAudioManager = am;
      theaudiorecord = ar;
      bufferSize = com.sonrlabs.test.sonr.signal.AudioProcessor.getAudioBufferSize();
      this.applicationContext = applicationContext;
      Common.save(applicationContext, SONR.CLIENT_STOP_RECEIVER_REGISTERED, false);
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

   public  void createListener() {
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
            singletonListener = new MicSerialListener(theaudiorecord, bufferSize);
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
      Common.save(applicationContext, SONR.CLIENT_STOP_RECEIVER_REGISTERED, true);
      applicationContext.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
      Log.i(TAG, "Registered broadcast receiver " + clientStopReceiver + " in context " + applicationContext);
   }

   private void unregisterReceiver() {
      try {
         if (Common.get(applicationContext, SONR.CLIENT_STOP_RECEIVER_REGISTERED, false)) {
            Common.save(applicationContext, SONR.CLIENT_STOP_RECEIVER_REGISTERED, false);
            applicationContext.unregisterReceiver(clientStopReceiver);
            Log.i(TAG, "Unregistered broadcast receiver " + clientStopReceiver + " in context " + applicationContext);
         }
      } catch (RuntimeException e) {
         Log.i(TAG, "Failed to unregister broadcast receiver " + clientStopReceiver + " in context " + applicationContext, e);
      }
   }
}
