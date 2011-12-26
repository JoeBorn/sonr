package com.sonrlabs.test.sonr;

import java.lang.reflect.Method;

import org.acra.ErrorReporter;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.sonrlabs.test.sonr.common.Common;

public class ToggleSONR
      extends Service {

   private static final String TAG = ToggleSONR.class.getSimpleName();

   // public static final String INTENT_UPDATE_ICON = "INTENT_UPDATE_ICON";

   enum HeadsetAction {
      INTENT_USER_TOGGLE_REQUEST("INTENT_TOGGLE_HEADSET"),
      HEADSET_PLUG_INTENT("android.intent.action.HEADSET_PLUG"),
      ACTION_POWER_CONNECTED("android.intent.action.ACTION_POWER_CONNECTED"),
      ACTION_POWER_DISCONNECTED("android.intent.action.ACTION_POWER_DISCONNECTED");

      private final String action;

      HeadsetAction(String action) {
         this.action = action;
      }

      String getActionString() {
         return action;
      }

      static HeadsetAction getAction(String string) {
         for (HeadsetAction action : values()) {
            if (action.getActionString().equals(string)) {
               return action;
            }
         }
         return null;
      }
   }

   /*
    * Constants determined from AudioSystem source
    */
   private static final int DEVICE_IN_WIRED_HEADSET = 0x400000;
   private static final int DEVICE_OUT_EARPIECE = 0x1;
   private static final int DEVICE_OUT_WIRED_HEADSET = 0x4;
   private static final int DEVICE_STATE_UNAVAILABLE = 0;
   private static final int DEVICE_STATE_AVAILABLE = 1;

   private static boolean SERVICE_ON = false;

   static boolean isServiceOn() {
      return SERVICE_ON;
   }

   private static HeadphoneReciever headsetReceiver = null;

   @Override
   public IBinder onBind(Intent arg0) {
      return null;
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      Log.d(TAG, "onStart");
      
      SERVICE_ON = true;
      
      if (headsetReceiver == null) {
         createHeadsetReceiver();
      }
      
      HeadsetAction action = HeadsetAction.getAction(intent.getAction());
      if (action != null) {
         handleIntentAction(intent, action);
      }
      return START_STICKY;
   }

   private void handleIntentAction(Intent intent, HeadsetAction action) {
      Log.d(TAG, "Received " + action.getActionString());
      switch (action) {
         case INTENT_USER_TOGGLE_REQUEST:
            Intent newIntent = new Intent(this, SONR.class);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(newIntent);
            break;
            
         case HEADSET_PLUG_INTENT:
            try {
               handlePlugIntent(intent);
            } catch (RuntimeException e) {
               e.printStackTrace();
            }
            break;
            
         case ACTION_POWER_CONNECTED:
            /*
             * Do nothing - but this intent should wake the service up and
             * allow us to catch HEADSET_PLUG
             */
            Log.d(TAG, "Caught POWER_CONNECTED_INTENT");
            break;
            
         case ACTION_POWER_DISCONNECTED:
            /*
             * Do nothing - but this intent should wake the service up and
             * allow us to refresh the icon if we were previously asleep
             */
            Log.d(TAG, "Caught POWER_DISCONNECTED_INTENT");
            break;
      }
   }

   /**
    * Called when the service is destroyed (low memory conditions). We may miss
    * notification of headset plug
    */
   @Override
   public void onDestroy() {
      try {
         SERVICE_ON = false;
         Log.i(TAG, "onDestroy");
         if (headsetReceiver != null) {
            unregisterReceiver(headsetReceiver);
         }
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   /**
    * Found by log and source code examine - state 2 is the state on the
    * multi-function adapter where the 3.5mm audio jack is plugged in
    */
   private void handlePlugIntent(Intent intent) {
      int state = intent.getExtras().getInt("state");
      Log.d(TAG, "Headset plug intent recieved, state " + Integer.toString(state));
      if (state != 0) {
         routeHeadset(this);
      } else { // end if state != 0
         unrouteHeadset(this);
      }
      updateIcon();
   }

   private void createHeadsetReceiver() {
      /**
       * Since HEADSET_PLUG uses FLAG_RECIEVER_REGISTERED_ONLY we need to
       * register and unregister the broadcast receiver in the service
       */
      headsetReceiver = new HeadphoneReciever();
      IntentFilter plugIntentFilter = new IntentFilter(HeadsetAction.HEADSET_PLUG_INTENT.getActionString());
      registerReceiver(headsetReceiver, plugIntentFilter);

      IntentFilter powerConnectedFilter = new IntentFilter(HeadsetAction.ACTION_POWER_CONNECTED.getActionString());
      registerReceiver(headsetReceiver, powerConnectedFilter);

      IntentFilter powerDisconnectedFilter = new IntentFilter(HeadsetAction.ACTION_POWER_DISCONNECTED.getActionString());
      registerReceiver(headsetReceiver, powerDisconnectedFilter);
   }

   private void routeHeadset(Context ctx) {
      Log.d(TAG, "route to headset");
      AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
         /*
          * see AudioService.setRouting Use MODE_INVALID to force headset
          * routing change
          */
         manager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET);
      } else {
         setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
         setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
      }

      if (!SONR.MAIN_SCREEN && !SONR.SONR_ON && isRoutingHeadset(this)) {
         /*
          * if running the app already, don't do autostart, that would be a mess
          */
         remakeClient();
      }
   }

   private void remakeClient() {
      AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
      SONRClient client = new SONRClient(this, SONR.findAudioRecord(), SONR.bufferSize, audioManager);
      client.createListener();
      client.searchSignal();
      boolean foundDock = client.foundDock();
      Log.d(TAG, "made it past search signal");
      if (foundDock) {
         // LogFile.MakeLog(SONR.DOCK_FOUND);
         Log.d(TAG, SONR.DOCK_FOUND);
         SONR.SONR_ON = true;
         
         if (Common.get(this, SONR.DEFAULT_PLAYER_SELECTED, false)) {
            Log.d(TAG, "DEFAULT MEDIA PLAYER FOUND");
            client.startListener();
            SONR.Start(this, true);
         } else {
            Log.d(TAG, "NO DEFAULT MEDIA PLAYER");
            Intent intent = new Intent(this, SONR.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
         }
         
         updateIconON();
      } else { 
         /* dock not found, probably headphones */
         Log.d(TAG, SONR.DOCK_NOT_FOUND);
         client.destroy();
      }
      /* Used to be client.destroy() here, which seems odd. */
   }

   private void unrouteHeadset(Context ctx) {
      Log.d(TAG, "unroute headset");
      AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

      // Restore notification volume
      SharedPreferences sharedPrefs = ctx.getSharedPreferences(SONR.SHARED_PREFERENCES, 0);
      int savedNotificationVolume = sharedPrefs.getInt("sharedNotificationVolume", 10);
      manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotificationVolume, AudioManager.FLAG_VIBRATE);

      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
         /*
          * see AudioService.setRouting Use MODE_INVALID to force headset
          * routing change
          */
         manager.setRouting(AudioManager.MODE_INVALID, 0, AudioManager.ROUTE_HEADSET);
      } else {
         setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
         setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
         setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "");
      }

      // LogFile.MakeLog("ToggleSONR unrouting headset");
      if (SONR.SONR_ON) {
         SONR.SONR_ON = false;
         Intent stopintent = new Intent(this, StopSONR.class);
         stopintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         startActivity(stopintent);
      }
   }

   /**
    * Toggles the current headset setting. If currently routed headset, routes
    * to speaker. If currently routed to speaker routes to headset
    */

   /**
    * Checks whether we are currently routing to headset
    * 
    * @return true if routing to headset, false if routing somewhere else
    */
   private boolean isRoutingHeadset(Context ctx) {
      boolean isRoutingHeadset = false;

      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
         /*
          * The code that works and is tested for Donut...
          */
         AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

         int routing = manager.getRouting(AudioManager.MODE_NORMAL);
         Log.d(TAG, "getRouting returns " + routing);
         isRoutingHeadset = (routing & AudioManager.ROUTE_HEADSET) != 0;
      } else {
         /*
          * Code for Android 2.1, 2.2, 2.3, maybe others... Thanks Adam King!
          */
         try {
            /**
             * Use reflection to get headset routing
             */
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            Method getDeviceConnectionState = audioSystem.getMethod("getDeviceConnectionState", int.class, String.class);

            int retVal = (Integer) getDeviceConnectionState.invoke(audioSystem, DEVICE_IN_WIRED_HEADSET, "");

            isRoutingHeadset = retVal == 1;
            Log.d(TAG, "getDeviceConnectionState " + retVal);

         } catch (Exception e) {
            Log.e(TAG, "Could not determine status in isRoutingHeadset(): " + e);
         }
      }
      return isRoutingHeadset;
   }

   /**
    * Updates the icon of the appwidget based on the current status of headphone
    * routing
    */
   private void updateIconON() {
      RemoteViews view = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);
      view.setImageViewResource(R.id.Icon, R.drawable.sonr_on);

      // Create an Intent to launch toggle headset
      Intent toggleIntent = new Intent(this, ToggleSONR.class);
      toggleIntent.setAction(HeadsetAction.INTENT_USER_TOGGLE_REQUEST.getActionString());
      PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

      // Get the layout for the App Widget and attach an on-click listener to
      // the icon
      view.setOnClickPendingIntent(R.id.Icon, pendingIntent);

      ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
      AppWidgetManager manager = AppWidgetManager.getInstance(this);
      manager.updateAppWidget(thisWidget, view);
   }

   private void updateIcon() {
      Log.d(TAG, "updateIcon");

      RemoteViews view = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);

      if (isRoutingHeadset(this)) {
         Log.d(TAG, "Headset is routed");
         view.setImageViewResource(R.id.Icon, R.drawable.sonr_on);
      } else {
         Log.d(TAG, "Headset not routed");
         view.setImageViewResource(R.id.Icon, R.drawable.sonr_off);
      }

      // Create an Intent to launch toggle headset
      Intent toggleIntent = new Intent(this, ToggleSONR.class);
      toggleIntent.setAction(HeadsetAction.INTENT_USER_TOGGLE_REQUEST.getActionString());
      PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

      // Get the layout for the App Widget and attach an on-click listener to
      // the icon
      view.setOnClickPendingIntent(R.id.Icon, pendingIntent);

      // Push update for this widget to the home screen
      ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
      AppWidgetManager manager = AppWidgetManager.getInstance(this);
      manager.updateAppWidget(thisWidget, view);
   }

   /**
    * set device connection state through reflection for Android 2.1, 2.2, 2.3,
    * maybe others. Thanks Adam King!
    * 
    */
   private void setDeviceConnectionState(final int device, final int state, final String address) {
      try {
         Class<?> audioSystem = Class.forName("android.media.AudioSystem");
         Method setDeviceConnectionState = audioSystem.getMethod("setDeviceConnectionState", int.class, int.class, String.class);

         setDeviceConnectionState.invoke(audioSystem, device, state, address);
      } catch (Exception e) {
         Log.e(TAG, "setDeviceConnectionState failed: " + e);
      }
   }
}
