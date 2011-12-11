package com.sonrlabs.test.sonr;

import org.acra.ErrorReporter;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;


public class SONRClient
        extends Service {
    // private static final String TAG = "SONRClient";
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
    private static final int SEARCH = 0x24;
    // end SONR commands
    // ****************************************************************************************************************

    private static final int REPEAT_TIME = 500; // for button repeats, in
    // milliseconds
    private static final int SKIP_TIME = 300;
    private static final int BACK_TIME = 300;
    private static final int VOL_TIME = 100;

    static private MicSerialListener theListener;
    private ByteReceiver myByteReceiver;
    private int theKeyEvent;
    private final AudioManager theAudioManager;
    private int volume = -1;
    private boolean ismuted = false;
    private AudioRecord theaudiorecord;
    private int bufferSize;
    public boolean found_dock = false;
    private Context ctx = null;
    private StopReceiver clientStopReceiver = null;

    public static boolean CLIENT_ON = false;

    public SONRClient(Context c, AudioRecord ar, int buffsize, final AudioManager am) {
        theAudioManager = am;
        theaudiorecord = ar;
        bufferSize = buffsize;
        ctx = c;
        CLIENT_ON = true;
    }

    public void SearchSignal() {
        int i = 0;
        while (i < 5) {
            Log.d("searching...", Integer.toString(i));
            theListener.searchSignal();
            if (theListener.foundDock()) {
                found_dock = true;
                break;
            }
            i++;
        }
    }

    public void StartListener() {
        if (!theListener.isAlive()) {
            // LogFile.MakeLog("Start Listener");
            try {
                theListener.start();
            } catch (Exception e) {
                e.printStackTrace();
                ErrorReporter.getInstance().handleException(e);
            }
        }
    }

    @Override
    public void onCreate() {
        try {
            // LogFile.MakeLog("\n\nSONRClient CREATED");
            clientStopReceiver = new StopReceiver();
            ctx.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
            myByteReceiver = new SONRByteReceiver();
            theListener = new MicSerialListener(theaudiorecord, bufferSize, myByteReceiver);
        } catch (Exception e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    private class StopReceiver
            extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Handle reciever
            String mAction = intent.getAction();

            if (mAction.equals(SONR.DISCONNECT_ACTION)) {
                onDestroy();
            }
        }
    }

    @Override
    public void onDestroy() {
        // LogFile.MakeLog("SONRClient DESTROY\n\n");
        try {
            synchronized (this) {
                super.onDestroy();
                try {
                    ctx.unregisterReceiver(clientStopReceiver);
                } catch (Exception e) {
                }
                if (theListener != null) {
                    theListener.onDestroy();
                }
            }
            CLIENT_ON = false;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorReporter.getInstance().handleException(e);
        }
    }

    private class SONRByteReceiver
            implements ByteReceiver {
        private long lastplaytime = 0;
        private long lastmutetime = 0;
        private long lastskiptime = 0;
        private long lastvolutime = 0;
        private long lastbacktime = 0;

        @Override
        public void receiveByte(int receivedByte) {
            try {
                theKeyEvent = -1;

                if (ismuted) {
                    if (receivedByte != MUTE) {
                        volume = 0;
                        ismuted = false;
                    }
                }

                switch (receivedByte) {
                    case PLAY_PAUSE:
                        if (lastplaytime < SystemClock.elapsedRealtime() - REPEAT_TIME) {
                            theKeyEvent = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                            lastplaytime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "PLAY");
                            // Toast.makeText(getApplicationContext(), "PLAY",
                            // Toast.LENGTH_SHORT).show();
                            // LogFile.MakeLog("RECEIVED PLAY");
                        }
                        break;
                    case FAST_FORWARD:
                        theKeyEvent = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                        Log.d(TAG, "FAST_FORWARD");
                        // Toast.makeText(getApplicationContext(),
                        // "FAST FORWARD",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED FAST FORWARD");
                        break;
                    case REWIND:
                        theKeyEvent = KeyEvent.KEYCODE_MEDIA_REWIND;
                        Log.d(TAG, "REWIND");
                        // Toast.makeText(getApplicationContext(), "REWIND",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED REWIND");
                        break;
                    case NEXT_TRACK:
                        if (lastskiptime < SystemClock.elapsedRealtime() - SKIP_TIME) {
                            theKeyEvent = KeyEvent.KEYCODE_MEDIA_NEXT;
                            lastskiptime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "NEXT_TRACK");
                            // Toast.makeText(getApplicationContext(),
                            // "NEXT TRACK",
                            // Toast.LENGTH_SHORT).show();

                            // LogFile.MakeLog("RECEIVED NEXT TRACK");
                        }
                        break;
                    case PREVIOUS_TRACK:
                        if (lastbacktime < SystemClock.elapsedRealtime() - BACK_TIME) {
                            theKeyEvent = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                            lastskiptime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "PREVIOUS_TRACK");
                            // Toast.makeText(getApplicationContext(),
                            // "PREVIOUS TRACK", Toast.LENGTH_SHORT).show();
                            // LogFile.MakeLog("RECEIVED PREVIOUS TRACK");
                        }
                        break;
                    case VOLUME_UP:
                        if (lastvolutime < SystemClock.elapsedRealtime() - VOL_TIME) {
                            theAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                                                               AudioManager.FLAG_SHOW_UI);
                            lastvolutime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "VOLUME_UP");
                            // Toast.makeText(getApplicationContext(),
                            // "VOLUME UP",
                            // Toast.LENGTH_SHORT).show();
                            // LogFile.MakeLog("RECEIVED VOLUME UP");
                        }
                        break;
                    case VOLUME_DOWN:
                        if (lastvolutime < SystemClock.elapsedRealtime() - VOL_TIME) {
                            theAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,
                                                               AudioManager.FLAG_SHOW_UI);
                            lastvolutime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "VOLUME_DOWN");
                            // Toast.makeText(getApplicationContext(),
                            // "VOLUME_DOWN", Toast.LENGTH_SHORT).show();
                            // LogFile.MakeLog("RECEIVED VOLUME DOWN");
                        }
                        break;
                    case MUTE:
                        if (lastmutetime < SystemClock.elapsedRealtime() - REPEAT_TIME) {
                            if (ismuted) {
                                volume = theAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2;
                                theAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                                ismuted = false;
                            } else {
                                // volume =
                                // theAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                volume = 0;
                                theAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
                                ismuted = true;
                            }
                            lastmutetime = SystemClock.elapsedRealtime();
                            Log.d(TAG, "MUTE");
                            // Toast.makeText(getApplicationContext(), "MUTE",
                            // Toast.LENGTH_SHORT).show();
                            // LogFile.MakeLog("RECEIVED MUTE");
                        }
                        break;
                    case THUMBS_UP:
                        Log.d(TAG, "THUMBS_UP");
                        // LogFile.MakeLog("RECEIVED THUMBS UP");
                        break;
                    case THUMBS_DOWN:
                        Log.d(TAG, "THUMBS_DOWN");
                        // Toast.makeText(getApplicationContext(),
                        // "THUMBS_DOWN",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED THUMBS DOWN");
                        break;
                    case FAVORITE:
                        Log.d(TAG, "FAVORITE");
                        // Toast.makeText(getApplicationContext(), "FAVORITE",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED FAVORITE");
                        break;
                    case UP:
                        theKeyEvent = KeyEvent.KEYCODE_DPAD_UP;
                        Log.d(TAG, "UP");
                        // Toast.makeText(getApplicationContext(), "UP",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED UP");
                        break;
                    case DOWN:
                        theKeyEvent = KeyEvent.KEYCODE_DPAD_DOWN;
                        Log.d(TAG, "DOWN");
                        // Toast.makeText(getApplicationContext(), "DOWN",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED DOWN");
                        break;
                    case LEFT:
                        theKeyEvent = KeyEvent.KEYCODE_DPAD_LEFT;
                        Log.d(TAG, "LEFT");
                        // Toast.makeText(getApplicationContext(), "LEFT",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED LEFT");
                        break;
                    case RIGHT:
                        theKeyEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
                        Log.d(TAG, "RIGHT");
                        // Toast.makeText(getApplicationContext(), "RIGHT",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED RIGHT");
                        break;
                    case SELECT:
                        theKeyEvent = KeyEvent.KEYCODE_DPAD_CENTER;
                        Log.d(TAG, "SELECT");
                        // Toast.makeText(getApplicationContext(), "SELECT",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED SELECT");
                        break;
                    case POWER_ON:
                        Log.d(TAG, "POWER_ON");
                        // Toast.makeText(getApplicationContext(), "POWER ON",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED POWER ON");
                        break;
                    case POWER_OFF:
                        Log.d(TAG, "POWER_OFF");
                        // Toast.makeText(getApplicationContext(), "POWER OFF",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED POWER OFF");
                        break;
                    case SONR_HOME:
                        Log.d(TAG, "HOME");
                        Intent i = new Intent(ctx, SONR.class);
                        Log.d("CONTEXT", ctx.getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ctx.getApplicationContext().startActivity(i);
                        break;
                    case SEARCH:
                        Log.d(TAG, "SEARCH");
                        // Toast.makeText(getApplicationContext(), "SEARCH",
                        // Toast.LENGTH_SHORT).show();
                        // LogFile.MakeLog("RECEIVED SEARCH");
                        break;
                    default:
                        Log.d(TAG, "default");
                        Log.d(TAG, "RECEIVED " + receivedByte);
                        // LogFile.MakeLog("RECEIVED " + receivedByte);
                        break;
                }

                if (theKeyEvent >= 0) {
                    sendbroadcast();
                }
            } catch (Exception e) {
                e.printStackTrace();
                ErrorReporter.getInstance().handleException(e);
            }
        }
    }

    void sendbroadcast() {
        // Intent i = null;
        // if(theKeyEvent == PLAY || theKeyEvent == PAUSE || theKeyEvent ==
        // FAST_FORWARD || theKeyEvent == REWIND || theKeyEvent == NEXT_TRACK ||
        // theKeyEvent == PREVIOUS_TRACK) {
        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
        SharedPreferences settings = ctx.getSharedPreferences(SONR.SHARED_PREFERENCES, 0);
        String selectedMediaPlayer = settings.getString("selectedMediaPlayer", "MediaPlayerNotFound");
        Log.d("BROADCAST PLAYER", selectedMediaPlayer);
        i.setPackage(selectedMediaPlayer);

        synchronized (this) {
            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, theKeyEvent));
            ctx.sendOrderedBroadcast(i, null);

            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, theKeyEvent));
            ctx.sendOrderedBroadcast(i, null);
        }
        // }
        // else i = new Intent(Intent.);
        // (new
        // Instrumentation()).sendCharacterSync(KeyEvent.KEYCODE_DPAD_RIGHT);
    }
}
