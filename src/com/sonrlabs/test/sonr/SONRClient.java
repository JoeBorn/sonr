package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.IBinder;
import android.util.Log;

public class SONRClient
      extends Service {

   /*
    * This is a static because we can create multiple clients but we only want one listener.
    * Possibly the creation of multiple clients is incorrect?
    * 
    */
   private static MicSerialListener singletonListener;
   
   /*
    * No idea why this is public or static, or even what it's for. Some Android
    * reflective magic?
    */
   public static boolean CLIENT_ON = false;
   
   private BroadcastReceiver clientStopReceiver;
   
   private final AudioManager theAudioManager;
   private final AudioRecord theaudiorecord;
   private final int bufferSize;
   private final Context ctx;


   SONRClient(Context c, AudioRecord ar, int buffsize, AudioManager am) {
      theAudioManager = am;
      theaudiorecord = ar;
      bufferSize = buffsize;
      ctx = c;
      CLIENT_ON = true;
   }
   
   boolean foundDock() {
      return singletonListener != null && singletonListener.foundDock();
   }

   void searchSignal() {
      int i = 0;
      while (i < 5) {
         Log.d("searching...", Integer.toString(i));
         singletonListener.searchSignal();
         if (singletonListener.foundDock()) {
            break;
         }
         i++;
      }
   }

   void startListener() {
      if (!singletonListener.isAlive()) {
         // LogFile.MakeLog("Start Listener");
         try {
            Utils.runTask(singletonListener);
         } catch (Exception e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         }
      }
   }

   @Override
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
            if (singletonListener != null) {
               singletonListener.stopRunning();
            }
            singletonListener = new MicSerialListener(theaudiorecord, bufferSize, controller);
         }
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }
   

   @Override
   public IBinder onBind(Intent arg0) {
      return null;
   }

   @Override
   public void onDestroy() {
      // LogFile.MakeLog("SONRClient DESTROY\n\n");
      destroy();
      super.onDestroy();
   }

   void destroy() {
      try {
         synchronized (this) {
            unregisterReceiver();
            if (singletonListener != null) {
               singletonListener.stopRunning();
            }
         }
         CLIENT_ON = false;
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   private void registerReceiver() {
      clientStopReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
            // Handle reciever
            String mAction = intent.getAction();
            if (mAction.equals(SONR.DISCONNECT_ACTION)) {
               destroy();
            }
         }
      };
      ctx.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
      Log.i(getClass().getName(), "Registered broadcast receiver " + clientStopReceiver + " in context " + ctx);
   }

   private void unregisterReceiver() {
      if (clientStopReceiver != null) {
         try {
            ctx.unregisterReceiver(clientStopReceiver);
            Log.i(getClass().getName(), "Unregistered broadcast receiver " + clientStopReceiver + " in context " + ctx);
            clientStopReceiver = null;
         } catch (Exception e) {
            Log.i(getClass().getName(), "Failed to unregister broadcast receiver " + clientStopReceiver + " in context " + ctx, e);
         }
      } else {
         Log.i(getClass().getName(), "No broadcast receiver to unregister in context " + ctx);
      }
   }
}
