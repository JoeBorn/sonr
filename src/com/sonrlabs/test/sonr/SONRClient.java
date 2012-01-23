package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.util.Log;

public class SONRClient {

   /*
    * This is a static because we can create multiple clients but we only want one listener.
    * Possibly the creation of multiple clients is incorrect?
    * 
    */
   private static MicSerialListener singletonListener;
   

   private final AudioManager theAudioManager;
   private final AudioRecord theaudiorecord;
   private final int bufferSize;
   private final Context ctx;

   private BroadcastReceiver clientStopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         // Handle receiver
         String mAction = intent.getAction();
         if (SONR.DISCONNECT_ACTION.equals(mAction)) {
            destroy();
         }
      }
   };


   SONRClient(Context c, AudioRecord ar, AudioManager am) {
      theAudioManager = am;
      theaudiorecord = ar;
      bufferSize = com.sonrlabs.test.sonr.signal.AudioProcessor.getAudioBufferSize();
      ctx = c;
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

   public void onCreate() {
      createListener();
   }

   void createListener() {
      try {
         synchronized (this) {
            // LogFile.MakeLog("\n\nSONRClient CREATED");
            unregisterReceiver();
            registerReceiver();
            IUserActionHandler controller = new UserActionHandler(theAudioManager,ctx);
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
      // LogFile.MakeLog("SONRClient DESTROY\n\n");
      destroy();
   }

   void destroy() {
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
      ctx.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
      Log.i(getClass().getName(), "Registered broadcast receiver " + clientStopReceiver + " in context " + ctx);
   }

   private void unregisterReceiver() {
      try {
         ctx.unregisterReceiver(clientStopReceiver);
         Log.i(getClass().getName(), "Unregistered broadcast receiver " + clientStopReceiver + " in context " + ctx);
         clientStopReceiver = null;
      } catch (RuntimeException e) {
         Log.i(getClass().getName(), "Failed to unregister broadcast receiver " + clientStopReceiver + " in context " + ctx, e);
      }
   }
}
