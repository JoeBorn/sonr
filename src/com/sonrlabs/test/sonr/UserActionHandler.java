package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

//import com.flurry.android.FlurryAgent;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import com.sonrlabs.prod.sonr.R;

class UserActionHandler
      implements IUserActionHandler {

   private static final String TAG = "SONR audio processor";

   // SONR commands ******************
   private static final int PLAY_PAUSE = 0x1e;
   private static final int FAST_FORWARD = -2;
   private static final int REWIND = -3;
   private static final int NEXT_TRACK = 0x1d;
   private static final int PREVIOUS_TRACK = 0x21;
   private static final int VOLUME_UP = 0x17;
   private static final int VOLUME_DOWN = 0x18;
   private static final int MUTE = 0x1b;
   private static final int THUMBS_UP = 0x9;
   private static final int THUMBS_DOWN = 0xa;
   private static final int FAVORITE = 0x6;
   private static final int UP = 0xc;
   private static final int DOWN = 0xf;
   private static final int LEFT = 0x11;
   private static final int RIGHT = 0x12;
   private static final int SELECT = 0x14;
   private static final int POWER_ON = 0x1;
   private static final int POWER_OFF = 0x5;
   private static final int SONR_HOME = 0x22;
   private static final int SHARE = 0x28; // 0x2b;
   private static final int SEARCH = 0x24;
   // end SONR commands
   // ****************************************************************************************************************

   // for button repeats, in milliseconds
   private static final int REPEAT_TIME = 500;
   private static final int SKIP_TIME = 300;
   private static final int BACK_TIME = 300;
   private static final int VOL_TIME = 100;

   private final AudioManager manager;
   private final Context appContext;

   private long lastPlayTime = 0;
   private long lastMuteTime = 0;
   private long lastSkipTime = 0;
   private long lastVolumeTime = 0;
   private long lastBackTime = 0;
   private int volume = -1;
   private boolean muted = false;

   private static final String CURRENT_VOLUME = "CURRENT_VOLUME";

   UserActionHandler(Context appContext) {
      SonrLog.d(TAG, "RemoteListener started");
      this.manager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
      this.appContext = appContext;
   }

   @Override
   public void processAction(int receivedByte) {
      try {
         processUserCommand(receivedByte);
      } catch (RuntimeException e) {
         e.printStackTrace();
         // ErrorReporter.getInstance().handleException(e);
      }
   }

   private void processUserCommand(int receivedByte) {
      checkAutoUnmute(receivedByte);
      int key = Integer.MIN_VALUE;

      switch (receivedByte) {
         case PLAY_PAUSE:
            if (lastPlayTime < SystemClock.elapsedRealtime() - REPEAT_TIME) {
               key = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
               lastPlayTime = SystemClock.elapsedRealtime();
               Log.d(TAG, "PLAY");
               // FlurryAgent.logEvent("PLAY_PRESSED");
            }
            break;
         case FAST_FORWARD:
            key = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
            Log.d(TAG, "FAST_FORWARD");
            // FlurryAgent.logEvent("FAST_FORWARD_PRESSED");
            break;
         case REWIND:
            key = KeyEvent.KEYCODE_MEDIA_REWIND;
            Log.d(TAG, "REWIND");
            // FlurryAgent.logEvent("REWIND_PRESSED");
            break;
         case NEXT_TRACK:
            if (lastSkipTime < SystemClock.elapsedRealtime() - SKIP_TIME) {
               key = KeyEvent.KEYCODE_MEDIA_NEXT;
               lastSkipTime = SystemClock.elapsedRealtime();
               Log.d(TAG, "NEXT_TRACK");
               // FlurryAgent.logEvent("NEXT_TRACK_PRESSED");
            }
            break;
         case PREVIOUS_TRACK:
            if (lastBackTime < SystemClock.elapsedRealtime() - BACK_TIME) {
               key = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
               lastBackTime = SystemClock.elapsedRealtime();
               Log.d(TAG, "PREVIOUS_TRACK");
               // FlurryAgent.logEvent("PREVIOUS_TRACK_PRESSED");
            }
            break;
         case VOLUME_UP:
            if (lastVolumeTime < SystemClock.elapsedRealtime() - VOL_TIME) {
               manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
               lastVolumeTime = SystemClock.elapsedRealtime();
               Log.d(TAG, "VOLUME_UP");
               // FlurryAgent.logEvent("VOLUME_UP_PRESSED");
            }
            break;
         case VOLUME_DOWN:
            if (lastVolumeTime < SystemClock.elapsedRealtime() - VOL_TIME) {
               manager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
               lastVolumeTime = SystemClock.elapsedRealtime();
               Log.d(TAG, "VOLUME_DOWN");
               // FlurryAgent.logEvent("VOLUME_DOWN_PRESSED");
            }
            break;
         case MUTE:
            if (lastMuteTime < SystemClock.elapsedRealtime() - REPEAT_TIME) {
               if (muted) {
                  volume =
                        Preferences.getPreference(appContext, CURRENT_VOLUME,
                                                  manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2); // manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                  // /
                  // 2;
                  manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                  muted = false;
               } else {
                  volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
                  Preferences.savePreference(appContext, CURRENT_VOLUME, volume);
                  volume = 0;
                  manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                  muted = true;
               }
               lastMuteTime = SystemClock.elapsedRealtime();
               Log.d(TAG, "MUTE");
               // FlurryAgent.logEvent("MUTE_PRESSED");
            }
            break;
         case THUMBS_UP:
            Log.d(TAG, "THUMBS_UP");
            key = THUMBS_UP;
            // FlurryAgent.logEvent("THUMBS_UP_PRESSED");
            break;
         case THUMBS_DOWN:
            Log.d(TAG, "THUMBS_DOWN");
            key = THUMBS_DOWN;
            // FlurryAgent.logEvent("THUMBS_DOWN_PRESSED");
            break;
         case FAVORITE:
            Log.d(TAG, "FAVORITE");
            key = FAVORITE;
            // FlurryAgent.logEvent("FAVORITE_PRESSED");
            break;
         case UP:
            key = KeyEvent.KEYCODE_DPAD_UP;
            Log.d(TAG, "UP");
            synchronized (this) {
               try {
                  new Thread(new Runnable() {
                     @Override
                     public void run() {
                        new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);
                     }
                  }).start();
               } catch (Exception e) {
                  Log.d("UserActionHandler", "prevent injection crash, this is a hack");
               }
            }
            break;
         case DOWN:
            key = KeyEvent.KEYCODE_DPAD_DOWN;
            Log.d(TAG, "DOWN");
            synchronized (this) {
               try {
                  new Thread(new Runnable() {
                     @Override
                     public void run() {
                        new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
                     }
                  }).start();
               } catch (Exception e) {
                  Log.d("UserActionHandler", "prevent injection crash, this is a hack");
               }
            }
            break;
         case LEFT:
            key = KeyEvent.KEYCODE_DPAD_LEFT;
            Log.d(TAG, "LEFT");
            synchronized (this) {
               try {
                  new Thread(new Runnable() {
                     @Override
                     public void run() {
                        new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);
                     }
                  }).start();
               } catch (Exception e) {
                  Log.d("UserActionHandler", "prevent injection crash, this is a hack");
               }
            }
            break;
         case RIGHT:
            key = KeyEvent.KEYCODE_DPAD_RIGHT;
            Log.d(TAG, "RIGHT");
            synchronized (this) {
               try {
                  new Thread(new Runnable() {
                     @Override
                     public void run() {
                        new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);
                     }
                  }).start();
               } catch (Exception e) { //java.lang.SecurityException
                  Log.d("UserActionHandler", "prevent injection crash, this is a hack"); 
               }
            }
            break;
         case SELECT:
            key = KeyEvent.KEYCODE_DPAD_CENTER;
            Log.d(TAG, "CENTER");
            synchronized (this) {
               try {
                  new Thread(new Runnable() {
                     @Override
                     public void run() {
                        new Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
                     }
                  }).start();
               } catch (Exception e) {
                  Log.d("UserActionHandler", "prevent injection crash, this is a hack");
               }
            }
            break;
         case SHARE:
            Log.d(TAG, "SHARE");
            key = SHARE;
            // FlurryAgent.logEvent("SHARE_PRESSED");
            break;
         case POWER_ON:
            Log.d(TAG, "POWER_ON");
            key = POWER_ON;
            // FlurryAgent.logEvent("POWER_ON_PRESSED");
            break;
         case POWER_OFF:
            Log.d(TAG, "POWER_OFF");
            key = POWER_OFF;
            // FlurryAgent.logEvent("POWER_OFF_PRESSED");
            break;
         case SONR_HOME:
            Log.d(TAG, "SONR HOME");
            Intent launchSonrHome = new Intent(appContext, SONR.class);
            launchSonrHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(launchSonrHome);
            break;
         case SEARCH:
            Log.d(TAG, "SEARCH");
            key = SEARCH;
            break;

         case 0:
            // consider this a no-op for now.
            break;

         default:
            SonrLog.d(TAG, "Unknown signal " + Integer.toHexString(receivedByte));
      }

      if (key != Integer.MIN_VALUE) {
         sendbroadcast(key);
      }
   }

   /*
    * If currently muted, unmute on any action other mute itself, but leave the
    * volume at 0.
    */
   private void checkAutoUnmute(int receivedByte) {
      if (muted) {
         if (receivedByte != MUTE) {
            volume = 0;
            muted = false;
         }
      }
   }

   private void sendbroadcast(int keyEvent) {

      synchronized (this) {
         String playerPackage = Preferences.getPreference(appContext, appContext.getString(R.string.APP_PACKAGE_NAME), null);

         if (playerPackage != null) {
            Log.d(TAG, playerPackage);

            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
            i.setPackage(playerPackage);

            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyEvent));
            appContext.sendOrderedBroadcast(i, null);

            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keyEvent));
            appContext.sendOrderedBroadcast(i, null);
         }
      }

   }
}