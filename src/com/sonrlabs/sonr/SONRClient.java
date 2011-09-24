package com.sonrlabs.sonr;


import com.sonrlabs.sonr.MicSerialListener.ByteReceiver;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

public class SONRClient extends Service
{
	private static final String TAG = "SONR";
	
	//SONR commands ******************	
	private static final int PLAY_PAUSE                     = 0x1e;
	private static final int FAST_FORWARD                   =-2;
    private static final int REWIND                         =-3;
    private static final int NEXT_TRACK                     = 0x1d;
    private static final int PREVIOUS_TRACK                 = 0x21;
    private static final int VOLUME_UP                      = 0x17;
    private static final int VOLUME_DOWN                    = 0x18;
    private static final int MUTE                           = 0x1b;
    private static final int THUMBS_UP                      = 0x9;
    private static final int THUMBS_DOWN                    = 0xa;
    private static final int FAVORITE                       = 0x6;
    private static final int UP                             = 0xc;
    private static final int DOWN                           = 0xf;
    private static final int LEFT                           = 0x11;
    private static final int RIGHT                          = 0x12;
    private static final int SELECT                         = 0x14;
    private static final int POWER_ON                       = 0x1;
	private static final int POWER_OFF                      = 0x5;
    private static final int SONR_HOME                      = 0x22;
    private static final int SEARCH                         = 0x24;
	//end SONR commands ****************************************************************************************************************

    private static final int REPEAT_TIME = 500;			//for button repeats, in milliseconds
    private static final int SKIP_TIME = 300;
    private static final int BACK_TIME = 300;
    private static final int VOL_TIME = 200;
	
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
			
	public SONRClient(Context c, AudioRecord ar, int buffsize, final AudioManager am) {
		theAudioManager = am;
		theaudiorecord = ar;
		bufferSize = buffsize;
		ctx = c;
	}
	
	public void SearchSignal() {
		theListener.searchSignal();
		if(theListener.found_dock)
			found_dock = true;
	}
	
	public void StartListener() {
		theListener.start();
	}
	
	@Override
	public void onCreate() {
		clientStopReceiver = new StopReceiver();
		ctx.registerReceiver(clientStopReceiver, new IntentFilter(SONR.DISCONNECT_ACTION));
		myByteReceiver = new SONRByteReceiver();
		theListener = new MicSerialListener(theaudiorecord, bufferSize, myByteReceiver);
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
			
	private class StopReceiver extends BroadcastReceiver {
		  @Override
		  public void onReceive(Context context, Intent intent) {
		    // Handle reciever
		    String mAction = intent.getAction();

		    if(mAction.equals(SONR.DISCONNECT_ACTION)) {
		      onDestroy();
		    }
		}
	}
	
	@Override
	public void onDestroy() {
		ctx.unregisterReceiver(clientStopReceiver);
		theListener.onDestroy();
		super.onDestroy();
	}
	
	
	private class SONRByteReceiver implements ByteReceiver {
		private long lastplaytime = 0;
		private long lastmutetime = 0;
		private long lastskiptime = 0;
		private long lastvolutime = 0;
		private long lastbacktime = 0;
		
		public void receiveByte(int receivedByte) {
			theKeyEvent = -1;
			
			switch(receivedByte) {
			case PLAY_PAUSE:
				if(lastplaytime < SystemClock.elapsedRealtime() - REPEAT_TIME) {
					theKeyEvent = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
					lastplaytime = SystemClock.elapsedRealtime();
					Log.d(TAG, "PLAY");
				}
				break;
			case FAST_FORWARD:
				theKeyEvent = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
				Log.d(TAG, "FAST_FORWARD");
				break;
			case REWIND:
				theKeyEvent = KeyEvent.KEYCODE_MEDIA_REWIND;
				Log.d(TAG, "REWIND");
				break;
			case NEXT_TRACK:
				if(lastskiptime < SystemClock.elapsedRealtime() - SKIP_TIME) {
					theKeyEvent = KeyEvent.KEYCODE_MEDIA_NEXT;
					lastskiptime = SystemClock.elapsedRealtime();
					Log.d(TAG, "NEXT_TRACK");
				}
				break;
			case PREVIOUS_TRACK:
				if(lastbacktime < SystemClock.elapsedRealtime() - BACK_TIME) {
					theKeyEvent = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
					lastskiptime = SystemClock.elapsedRealtime();
					Log.d(TAG, "PREVIOUS_TRACK");
				}
				break;
			case VOLUME_UP:
				if(lastvolutime < SystemClock.elapsedRealtime() - VOL_TIME) {
					theAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
					lastvolutime = SystemClock.elapsedRealtime();
					Log.d(TAG, "VOLUME_UP");
				}
				break;
			case VOLUME_DOWN:
				if(lastvolutime < SystemClock.elapsedRealtime() - VOL_TIME) {
					theAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
					lastvolutime = SystemClock.elapsedRealtime();
					Log.d(TAG, "VOLUME_DOWN");
				}
				break;
			case MUTE:
				if(lastmutetime < SystemClock.elapsedRealtime() - REPEAT_TIME) {
					if(ismuted) {
						theAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
						ismuted = false;
					} else {
						volume = theAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
						theAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
						ismuted = true;
					}
					lastmutetime = SystemClock.elapsedRealtime();
					Log.d(TAG, "MUTE");
				}
				break;
			case THUMBS_UP:
				Log.d(TAG, "THUMBS_UP");
				break;
			case THUMBS_DOWN:
				Log.d(TAG, "THUMBS_DOWN");
				break;
			case FAVORITE:
				Log.d(TAG, "FAVORITE");
				break;
			case UP:
				theKeyEvent = KeyEvent.KEYCODE_DPAD_UP;
				Log.d(TAG, "UP");
				break;
			case DOWN:
				theKeyEvent = KeyEvent.KEYCODE_DPAD_DOWN;
				Log.d(TAG, "DOWN");
				break;
			case LEFT:
				theKeyEvent = KeyEvent.KEYCODE_DPAD_LEFT;
				Log.d(TAG, "LEFT");
				break;
			case RIGHT:
				theKeyEvent = KeyEvent.KEYCODE_DPAD_RIGHT;
				Log.d(TAG, "RIGHT");
				break;
			case SELECT:
				Log.d(TAG, "SELECT");
				theKeyEvent = KeyEvent.KEYCODE_DPAD_CENTER;
				break;
			case POWER_ON:
				Log.d(TAG, "POWER_ON");
				break;
			case POWER_OFF:
				Log.d(TAG, "POWER_OFF");
				break;
			case SONR_HOME:
				Log.d(TAG, "PAUSE");
				break;
			case SEARCH:
				Log.d(TAG, "SEARCH");
				break;
			/*case 98:
				theKeyEvent = KeyEvent.KEYCODE_DPAD_CENTER;
				//InputConnection ic = getCurrentInputConnection();
				//sendDownUpKeyEvents(theKeyEvent);
		        getCurrentInputConnection().sendKeyEvent(
		                new KeyEvent(KeyEvent.ACTION_DOWN, theKeyEvent));
		        getCurrentInputConnection().sendKeyEvent(
		                new KeyEvent(KeyEvent.ACTION_UP, theKeyEvent));
				Log.d(TAG, "RIGHT");*/
			default:
				Log.d(TAG, "default");
				break;
			}
			
			if(theKeyEvent >= 0) {
				sendbroadcast();
			}
		}
	}
	
	void sendbroadcast() {
		//Intent i = null;
		//if(theKeyEvent == PLAY || theKeyEvent == PAUSE || theKeyEvent == FAST_FORWARD || theKeyEvent == REWIND || theKeyEvent == NEXT_TRACK || theKeyEvent == PREVIOUS_TRACK) {
		Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
		synchronized(this) {
			i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, theKeyEvent));
			ctx.sendOrderedBroadcast(i, null);
			
			i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, theKeyEvent));
			ctx.sendOrderedBroadcast(i, null);
		}
		//}
		//else i = new Intent(Intent.);
		//(new Instrumentation()).sendCharacterSync(KeyEvent.KEYCODE_DPAD_RIGHT);
	}
}
