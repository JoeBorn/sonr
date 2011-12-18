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

/*
 * This class does not need to extend service.
 * [Then why does it?]
 */
public class SONRClient
      extends Service {
   // private static final String TAG = SONRClient.class.getSimpleName();

   /*
    * This is a static because we can create multiple clients but we only want
    * one listener. Possibly the creation of multiple clients is incorrect?
    */
   private static MicSerialListener singletonListener;

   /*
    * No idea why this is public or static, or even what it's for. Some Android
    * reflective magic?
    */
   public static boolean CLIENT_ON = false;

   private BroadcastReceiver clientStopReceiver;
   private IUserActionHandler controller;

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
            singletonListener.start();
         } catch (Exception e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
         }
      }
   }

   @Override
   /*
    * SONRClient is not created like a Android Service should be, therefore
    * synch block applies here.
    */
   public void onCreate() {
      try {
         synchronized (this) {
            // LogFile.MakeLog("\n\nSONRClient CREATED");
            unregisterReceiver();
            registerReceiver();
         }
         controller = new UserActionHandler(theAudioManager, ctx);
         singletonListener = new MicSerialListener(theaudiorecord, bufferSize, controller);
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
      try {
         synchronized (this) {
            super.onDestroy();
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
               onDestroy();
            }
         }
      };
      ctx.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
   }

   private void unregisterReceiver() {
      if (clientStopReceiver != null) {
         try {
            ctx.unregisterReceiver(clientStopReceiver);
         } catch (Exception e) {
            // ignore errors here
         } finally {
            clientStopReceiver = null;
         }
      }
   }
}
