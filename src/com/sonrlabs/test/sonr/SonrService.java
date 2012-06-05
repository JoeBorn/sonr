package com.sonrlabs.test.sonr;

import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import com.sonrlabs.prod.sonr.R;

public class SonrService
extends Service {

   // @formatter:off
   /**
    * state - 0 for unplugged, 1 for plugged. name - Headset type, human
    * readable string microphone - 1 if headset has a microphone, 0 otherwise
    */
   // @formatter:on

   static final int UN_PLUGGED = 20;
   static final int PLUGGED_IN = 30;

   static final int START_SELECTED_PLAYER = 2;

   static final int SONR_ID = 1;

   private boolean mSonrServiceCreated = false;
   private boolean mSonrServiceStarted = false;
   private boolean mSonrDiscoveryInProgress = false;

   private static final String TAG = SonrService.class.getSimpleName();
   private static final String INTENT_USER_TOGGLE_REQUEST = "INTENT_TOGGLE_HEADSET";

   private AudioManager audioManager = null;

   private static HeadphoneReceiver headsetReceiver = null;

   private PowerManager.WakeLock mWakeLock;

   class ServiceHandler
   extends Handler {
      public ServiceHandler(Looper looper) {
         super(looper);
      }

      @Override
      public void handleMessage(Message msg) {
         switch (msg.what) {
            case PLUGGED_IN:
               SonrLog.d(TAG, "ServiceHandler plugged processing...");
               startSonrService();
               break;
            case UN_PLUGGED:
               SonrLog.d(TAG, "ServiceHandler unplugged processing...");
               stopSonrService();
               break;
            default:
               break;
         }

         switch (msg.arg1) {
            case START_SELECTED_PLAYER:
               SonrLog.d(TAG, "Starting selected music player...");
               if (mClient == null || !mClient.foundDock()) {
                  // try once
                  startSonrService();
                  if (mClient == null || !mClient.foundDock()) {
                     Messenger messenger = msg.replyTo;
                     Message messageToTarget = Message.obtain();
                     try {
                        messenger.send(messageToTarget);
                     } catch (RemoteException e) {
                        SonrLog.e(TAG, e.toString());
                     }
                  }
               } else {
                  // if service not started yet, start the app?
                  AppUtils.doStart(SonrService.this, false);
               }
               break;
            default:
               break;
         }
      }
   }

   private static ServiceHandler mServiceHandler;

   private Messenger mMessenger;
   private Looper mServiceLooper;

   private SONRClient mClient;

   private Notification mNotification;

   @Override
   public void onCreate() {
      SonrLog.d(TAG, "onCreate()");
      sonrServiceInternalInit();
      startPhoneStateListener();
   }

   @Override
   public IBinder onBind(Intent intent) {
      SonrLog.d(TAG, "onBind()");
      sonrServiceInternalInit();
      return mMessenger.getBinder();
   }

   private synchronized void sonrServiceInternalInit() {
      if (headsetReceiver == null) {
         /**
          * Since HEADSET_PLUG uses FLAG_RECIEVER_REGISTERED_ONLY we need to
          * register and unregister the broadcast receiver in the service
          */
         headsetReceiver = new HeadphoneReceiver();
         IntentFilter plugIntentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
         registerReceiver(headsetReceiver, plugIntentFilter);
/*
         IntentFilter powerConnectedFilter = new IntentFilter(Intent.ACTION_POWER_CONNECTED);
         registerReceiver(headsetReceiver, powerConnectedFilter);

         IntentFilter powerDisconnectedFilter = new IntentFilter(Intent.ACTION_POWER_DISCONNECTED);
         registerReceiver(headsetReceiver, powerDisconnectedFilter);
*/
      }

      // Listen for speech recognition intents
      registerReceiver(speechRecognizerReceiver, new IntentFilter(SonrActivity.SPEECH_RECOGNIZER_ACTION));

      // Get the audio manager
      audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

      if (!mSonrServiceCreated) {
         HandlerThread handlerThread = new HandlerThread("SONR_HandlerThread");
         handlerThread.start();

         mServiceLooper = handlerThread.getLooper();

         mServiceHandler = new ServiceHandler(mServiceLooper);
         mMessenger = new Messenger(mServiceHandler);

         mSonrServiceCreated = true;
      }

      OnAudioFocusChangeListener afChangeListener = new OnAudioFocusChangeListener() {
         public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
               SonrLog.d(TAG, "audio focus gain");

               SonrLog.d(TAG, "Waiting");
               try {
                  Thread.sleep(5000);
               } catch (InterruptedException e) {
                  // TODO Auto-generated catch block
                  // throw new RuntimeException(e);
               }

               // boolean wasOff = mSonrServiceStarted == false;

               Message msg = mServiceHandler.obtainMessage();
               msg.what = PLUGGED_IN;
               mServiceHandler.sendMessage(msg);

               if (!mSonrServiceStarted/* && wasOff */) {
                  int key = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                  Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
                  // i.setPackage("com.pandora.android");

                  i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, key));
                  sendOrderedBroadcast(i, null);

                  i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, key));
                  sendOrderedBroadcast(i, null);
               }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
               Log.d("TAG", "audio focus loss");
            }
         }
      };
      audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
   }

   /**
    * Called when all clients have disconnected from a particular interface
    * published by the service. The default implementation does nothing and
    * returns false. Parameters intent The Intent that was used to bind to this
    * service, as given to Context.bindService. Note that any extras that were
    * included with the Intent at that point will not be seen here.
    * 
    * @return true if you would like to have the service's onRebind(Intent)
    *         method later called when new clients bind to it.
    */
   // @Override
   // public boolean onUnbind(Intent intent) {
   // //if (mClient != null && !mClient.foundDock()) {
   // // cleanup, reset mServiceStatus etc
   // // TODO: HEADSET_PLUG with UN_PLUGGED intent will destroy the service...
   // //}
   // return super.onUnbind(intent);
   // }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      SonrLog.d(TAG, "onStartCommand()");

      if (intent != null) {
         String action = intent.getAction();
         SonrLog.d(TAG, String.format("Received action: %s", action));

         if (INTENT_USER_TOGGLE_REQUEST.equals(action)) {
            toggleHeadset();
            Intent i = new Intent(this, SonrActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);

         } else if (Intent.ACTION_HEADSET_PLUG.equals(action)) {

            Message msg = mServiceHandler.obtainMessage();

            int state = intent.getExtras().getInt("state");
            switch (state) {
               case 0: // 0 for unplugged
                  msg.what = UN_PLUGGED;
                  SonrLog.d(TAG, "Headset plug intent recieved, state " + state + " UN_PLUGGED");
                  mServiceHandler.sendMessage(msg);
                  break;
               case 1: // 1 for plugged
               default:
                  msg.what = PLUGGED_IN;

                  if (!isRoutingHeadset()) {
                     /**
                      * Only change the headset toggle if not currently routing
                      * headset. If currently routing headset and the headset
                      * was unplugged the OS takes care of this for us.
                      */
                     toggleHeadset();
                  }

                  SonrLog.d(TAG, "Headset plug intent recieved, state " + state + " PLUGGED_IN");
                  if (!mSonrServiceStarted && !mSonrDiscoveryInProgress) {
                     mServiceHandler.sendMessage(msg);
                  }
                  break;
            }
         } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            /**
             * Do nothing - but this intent should wake the service up and allow
             * us to catch HEADSET_PLUG
             */
            SonrLog.d(TAG, "Caught POWER_CONNECTED_INTENT");
         } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            /**
             * Do nothing - but this intent should wake the service up and allow
             * us to refresh the icon if we were previously asleep
             */
            SonrLog.d(TAG, "Caught POWER_DISCONNECTED_INTENT");
         }
      }

      return Service.START_STICKY;
   }

   private synchronized void startSonrService() {
      if (!mSonrServiceStarted && !mSonrDiscoveryInProgress) {

         mSonrDiscoveryInProgress = true;

         route_headset(SonrService.this);
         boolean headsetRouted = isRoutingHeadset();

         if (headsetRouted) {

            newUpClient();

            if (mClient.foundDock()) {

               mClient.startListener();

               SonrLog.d(TAG, getString(R.string.DOCK_FOUND));

               statusBarNotification(SonrService.this);

               PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
               mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
               mWakeLock.acquire();

               SonrLog.d(TAG, "Headset routed");

               if (Preferences.getPreference(SonrService.this, getString(R.string.DEFAULT_PLAYER_SELECTED), false)) {
                  SonrLog.d(TAG, getString(R.string.DEFAULT_MEDIA_PLAYER_FOUND));

                  // TODO: this needs to be ironed out, when is user
                  // selecting default player, on a long click-hold or?
                  AppUtils.doStart(SonrService.this, true); // true?

               } else {
                  // DONT ENABLE UNTIL YOU FIGURE OUT
                  /*
                   * 
                   * 
                   * WHEN
                   */
                  // THE SONR ACTIVITY SHOULD BE STARTED, OTHERWISE YOU HAVE A
                  // LOOP

                  // SonrLog.d(TAG,
                  // getString(R.string.NO_DEFAULT_MEDIA_PLAYER));
                  // Intent startSonrActivity = new Intent(this,
                  // SonrActivity.class);
                  // startSonrActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  // startActivity(startSonrActivity);
               }

               updateIconON();

               SonrLog.i(TAG, "Started SONR Service");
               mSonrServiceStarted = true;
               startForeground(SonrService.SONR_ID, mNotification);
            }

         } else {
            SonrLog.d(TAG, getString(R.string.DOCK_NOT_FOUND));
            cleanUpClient();

            updateIcon();
         }

         mSonrDiscoveryInProgress = false;

      }
   }

   private synchronized void stopSonrService() {
      if (mSonrServiceStarted) {
         SonrLog.i(TAG, "Stopping SONR Service");

         mSonrServiceStarted = false;

         resetSonrService();
      } else {
         SonrLog.d(TAG, "Trying to shut down but sonr service not started");
      }
   }

   // when headset intent meant UNPLUGGED
   // SonrLogFile.SonrLog("ToggleSONR unrouting headset");
   // Intent stopintent = new Intent(this, StopSONR.class);
   // stopintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
   // startActivity(stopintent);

   /**
    * Don't call this unless you are sure.
    */
   private void resetSonrService() {
      cleanUpClient();
      stopForeground(true);
      unroute_headset(SonrService.this);

      // toggleHeadset(); // FIXME: sometimes SONR thinks it isn't unplugged
      AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
      routeToEarpiece(manager);

      if (mWakeLock != null) {
         mWakeLock.release();
      }
   }

   private void newUpClient() {
      cleanUpClient();
      mClient = new SONRClient(SonrService.this);
      mClient.createListener();
   }

   private void cleanUpClient() {
      if (mClient != null) {
         SonrLog.d(TAG, "releasing client");
         mClient.onDestroy();
         mClient = null;
      }
   }

   /**
    * Called when the service is destroyed (low memory conditions). We may miss
    * notification of headset plug
    */
   @Override
   public void onDestroy() {
      stopPhoneStateListener();
      mServiceLooper.quit();

      SonrLog.i(TAG, "Shutting down SONR Service");
      cleanUpHeadsetReceiver();

      mSonrServiceCreated = false;
      mSonrServiceStarted = false;

      resetSonrService();
   }

   private void cleanUpHeadsetReceiver() {
      if (headsetReceiver != null) {
         unregisterReceiver(headsetReceiver);
         unregisterReceiver(speechRecognizerReceiver);
         SonrLog.d(TAG, "unRegisterHeadsetReceiver()");
         headsetReceiver = null;
         speechRecognizerReceiver = null;
      }
   }

   /**
    * Toggles the current headset setting. If currently routed headset, routes
    * to speaker. If currently routed to speaker routes to headset
    */
   public void toggleHeadset() {
      AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
      Log.d(TAG, "toggleHeadset");
      if (isRoutingHeadset()) {
         routeToEarpiece(manager);
      } else {
         Log.d(TAG, "route to headset");
         if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
            /*
             * see AudioService.setRouting Use MODE_INVALID to force headset
             * routing change
             */
            manager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET);
         } else {
            setDeviceConnectionState(AudioSystemConstants.DEVICE_IN_WIRED_HEADSET, AudioSystemConstants.DEVICE_STATE_AVAILABLE, "");
            setDeviceConnectionState(AudioSystemConstants.DEVICE_OUT_WIRED_HEADSET, AudioSystemConstants.DEVICE_STATE_AVAILABLE, "");
         }
      }
   }

   /**
    * Routes audio to earpiece.
    * 
    * @param manager AudioManager instance.
    */
   private static void routeToEarpiece(AudioManager manager) {
      Log.d(TAG, "route to earpiece");
      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
         /*
          * see AudioService.setRouting Use MODE_INVALID to force headset
          * routing change
          */
         manager.setRouting(AudioManager.MODE_INVALID, 0, AudioManager.ROUTE_HEADSET);
      } else {
         setDeviceConnectionState(AudioSystemConstants.DEVICE_IN_WIRED_HEADSET, AudioSystemConstants.DEVICE_STATE_UNAVAILABLE, "");
         setDeviceConnectionState(AudioSystemConstants.DEVICE_OUT_WIRED_HEADSET, AudioSystemConstants.DEVICE_STATE_UNAVAILABLE, "");
         setDeviceConnectionState(AudioSystemConstants.DEVICE_OUT_EARPIECE, AudioSystemConstants.DEVICE_STATE_AVAILABLE, "");
      }
   }

   /**
    * Checks whether we are currently routing to headset
    * 
    * @return true if routing to headset, false if routing somewhere else
    */
   public boolean isRoutingHeadset() {
      boolean isRoutingHeadset = false;

      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
         /*
          * The code that works and is tested for Donut...
          */
         AudioManager manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

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

            int retVal = (Integer) getDeviceConnectionState.invoke(audioSystem, AudioSystemConstants.DEVICE_IN_WIRED_HEADSET, "");

            isRoutingHeadset = (retVal == 1);
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
   void updateIconON() {
      RemoteViews view = new RemoteViews(SonrService.this.getPackageName(), R.layout.toggle_apwidget);
      view.setImageViewResource(R.id.Icon, R.drawable.sonr_on);

      // Create an Intent to launch toggle headset
      Intent toggleIntent = new Intent(this, SonrService.class);
      toggleIntent.setAction(INTENT_USER_TOGGLE_REQUEST);
      PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

      // Get the layout for the App Widget and attach an on-click listener to
      // the icon
      view.setOnClickPendingIntent(R.id.Icon, pendingIntent);

      ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
      AppWidgetManager manager = AppWidgetManager.getInstance(this);
      manager.updateAppWidget(thisWidget, view);
   }

   void updateIcon() {
      SonrLog.d(TAG, "updateIcon");

      RemoteViews view = new RemoteViews(getPackageName(), R.layout.toggle_apwidget);

      if (isRoutingHeadset()) {
         SonrLog.d(TAG, "Headset is routed");
         view.setImageViewResource(R.id.Icon, R.drawable.sonr_on);
      } else {
         SonrLog.d(TAG, "Headset not routed");
         view.setImageViewResource(R.id.Icon, R.drawable.sonr_off);
      }

      // Create an Intent to launch toggle headset
      Intent toggleIntent = new Intent(this, SonrService.class);
      toggleIntent.setAction(SonrService.INTENT_USER_TOGGLE_REQUEST);
      PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

      // Get the layout for the App Widget and attach an on-click listener to
      // the icon
      view.setOnClickPendingIntent(R.id.Icon, pendingIntent);

      // Push update for this widget to the home screen
      ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
      AppWidgetManager manager = AppWidgetManager.getInstance(this);
      manager.updateAppWidget(thisWidget, view);
   }

   private void statusBarNotification(Context ctx) {
      NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

      int icon = R.drawable.sonr_icon;
      long when = System.currentTimeMillis();

      if (mNotification == null) {
         mNotification = new Notification(icon, ctx.getString(R.string.tickerText), when);
      }
      mNotification.flags |= Notification.FLAG_NO_CLEAR;

      Intent notificationIntent = new Intent(ctx, SonrActivity.class);
      notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

      PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

      mNotification.setLatestEventInfo(ctx, ctx.getString(R.string.widget_name), ctx.getString(R.string.notificationText),
                                       contentIntent);
      notificationManager.notify(SonrService.SONR_ID, mNotification);
   }

   static void route_headset(Context ctx) {
      SonrLog.d(TAG, "route to headset");
      AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
      if (Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT) {
         /*
          * see AudioService.setRouting Use MODE_INVALID to force headset
          * routing change
          */
         manager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET);
      } else {
         setDeviceConnectionState(AudioSystemConstants.DEVICE_IN_WIRED_HEADSET, AudioSystemConstants.DEVICE_STATE_AVAILABLE, "");
         setDeviceConnectionState(AudioSystemConstants.DEVICE_OUT_WIRED_HEADSET, AudioSystemConstants.DEVICE_STATE_AVAILABLE, "");

         SonrLog.d(TAG, Integer.toBinaryString(getDeviceConnectionState(AudioSystemConstants.DEVICE_IN_WIRED_HEADSET)));
         SonrLog.d(TAG, Integer.toBinaryString(getDeviceConnectionState(AudioSystemConstants.DEVICE_OUT_WIRED_HEADSET)));
      }
   }

   static void unroute_headset(Context ctx) {
      SonrLog.d(TAG, "unroute headset");
      AudioManager manager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);

      // Restore notification volume
      int savedNotificationVolume = Preferences.getPreference(ctx, ctx.getString(R.string.SAVED_NOTIFICATION_VOLUME), 10);
      manager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedNotificationVolume, AudioManager.FLAG_VIBRATE);
      routeToEarpiece(manager);
      
      Message msg = new Message();
      msg.what = UN_PLUGGED;
      mServiceHandler.sendMessage(msg);

   }

   /**
    * set device connection state through reflection for Android 2.1, 2.2, 2.3,
    * maybe others. Thanks Adam King!
    * 
    * @param device
    * @param state
    * @param address
    */
   static void setDeviceConnectionState(final int device, final int state, final String address) {
      try {
         Class<?> audioSystem = Class.forName("android.media.AudioSystem");
         Method setDeviceConnectionState = audioSystem.getMethod("setDeviceConnectionState", int.class, int.class, String.class);

         setDeviceConnectionState.invoke(audioSystem, device, state, address);
      } catch (Exception e) {
         SonrLog.e(TAG, "setDeviceConnectionState failed: " + e);
      }
   }

   /**
    * get device connection state through reflection for Android 2.1, 2.2, 2.3,
    */
   static int getDeviceConnectionState(final int device) {
      int deviceState = -1;
      try {
         Class<?> audioSystem = Class.forName("android.media.AudioSystem");
         Method getDeviceConnectionState = audioSystem.getMethod("getDeviceConnectionState", int.class, String.class);

         deviceState = (Integer) getDeviceConnectionState.invoke(audioSystem, device, "");
      } catch (Exception e) {
         SonrLog.e(TAG, "setDeviceConnectionState failed: " + e);
      }
      return deviceState;
   }

   private BroadcastReceiver speechRecognizerReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         if (intent != null && SonrActivity.SPEECH_RECOGNIZER_ACTION.equals(intent.getAction())) {
            SonrLog.d(TAG, "SPEECH RECOGNIZER COMMAND RECEIVED!");

            Message msg = mServiceHandler.obtainMessage();
            msg.what = UN_PLUGGED;
            mServiceHandler.sendMessage(msg);

            // startVoiceRecognitionActivity();
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(SonrActivity.GOOGLE_VOICE_SEARCH_PACKAGE_NAME);
            context.startActivity(launchIntent);
         }
      }
   };

   class ToggleHeadsetPhoneStateListener
   extends PhoneStateListener {
      @Override
      public void onCallStateChanged(int state, String incomingNumber) {
         Log.i(TAG, "Call state changed");
         if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            Log.i(TAG, "Call answered");
            if (isRoutingHeadset()) {
               Log.i(TAG, "Toggle to earpiece speaker to take call");
               toggleHeadset();
               updateIcon();
            }
         }
      }
   }

   PhoneStateListener mPhoneStateListener = null;

   synchronized void startPhoneStateListener() {
      if (mPhoneStateListener == null) {
         PhoneStateListener listener = new ToggleHeadsetPhoneStateListener();
         TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
         manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
         mPhoneStateListener = listener;
      }
   }

   synchronized void stopPhoneStateListener() {
      if (mPhoneStateListener != null) {
         TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
         manager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
         mPhoneStateListener = null;
      }
   }
}
