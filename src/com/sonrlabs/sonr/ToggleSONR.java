package com.sonrlabs.sonr;

import java.lang.reflect.Method;

import com.sonrlabs.sonr.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class ToggleSONR extends Service {

	private static String TAG = "ToggleHeadsetService";
	public static final String INTENT_UPDATE_ICON = "com.dwalkes.android.toggleheadset2.INTENT_UPDATE_ICON";
	public static final String INTENT_USER_TOGGLE_REQUEST = "com.dwalkes.android.toggleheadset2.INTENT_TOGGLE_HEADSET";

    /*
     *  Constants determined from AudioSystem source
     */
    public static final int DEVICE_IN_WIRED_HEADSET    = 0x400000;
    public static final int DEVICE_OUT_EARPIECE        = 0x1;
    public static final int DEVICE_OUT_WIRED_HEADSET   = 0x4;
    public static final int DEVICE_STATE_UNAVAILABLE   = 0;
    public static final int DEVICE_STATE_AVAILABLE     = 1;
    
    public static boolean SERVICE_ON = false;
    public static final int SONR_ID = 1;
    
    private int bufferSize = 0;
    
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	HeadphoneReciever headsetReceiver = null;

	/**
	 * Starts a service to monitor headset toggle or updates the current toggle state
	 * If this is the first start of the service, registers a broadcast receiver to receive headset plug intent.
	 * If intent for headset plug was received, check whether the state has changed to a value indicating
	 * headset route.  If it has and the headset is not currently routed, route the headset.
	 * If intent for power connected was received, do nothing - but hope this was enough to start the service
	 * in time to catch HEADSET_PLUG intent.  See issue 3 (http://code.google.com/p/toggleheadset2/issues/detail?id=3) 
	 * @param startId Not used
	 */
	@Override 
	public void onStart(Intent intent, int startId) {
		Log.d(TAG,"onStart");
		SERVICE_ON = true;
		if( intent.getAction() != null ) {
			Log.d(TAG, "Received " + intent.getAction() );
		}
		
		if(headsetReceiver == null )
		{
			/** Since HEADSET_PLUG uses FLAG_RECIEVER_REGISTERED_ONLY we need to register and
			 * unregister the broadcast receiver in the service
			 */
			headsetReceiver = new HeadphoneReciever();
			IntentFilter plugIntentFilter = new IntentFilter(HeadphoneReciever.HEADSET_PLUG_INTENT);
			registerReceiver(headsetReceiver, plugIntentFilter); 
			
			IntentFilter powerConnectedFilter = new IntentFilter(HeadphoneReciever.ACTION_POWER_CONNECTED);
			registerReceiver(headsetReceiver, powerConnectedFilter);
			
			IntentFilter powerDisconnectedFilter = new IntentFilter(HeadphoneReciever.ACTION_POWER_DISCONNECTED);
			registerReceiver(headsetReceiver, powerDisconnectedFilter);
			
		}
		if( intent != null && intent.getAction() != null ) 
		{
			if( intent.getAction().equals(INTENT_USER_TOGGLE_REQUEST)  )
			{
				//toggleHeadset();
				Intent i = new Intent(this, SONR.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.startActivity(i);
			}
			else if( intent.getAction().equals(HeadphoneReciever.HEADSET_PLUG_INTENT))
			{
				int state = intent.getExtras().getInt("state");
				
				Log.d(TAG,"Headset plug intent recieved, state " + Integer.toString(state));
				/**
				 *  Found by log and source code examine - state 2 is the state on the multi-function adapter where the
				 *  3.5mm audio jack is plugged in 
				 */
				if(state != 0) {
					AudioRecord theaudiorecord = findAudioRecord();
					MicSerialListener theListener = new MicSerialListener(theaudiorecord, SONR.bufferSize, null);
					theListener.searchSignal();
					boolean found = theListener.found_dock;
					theListener.onDestroy();
					
					if(found) {
						Log.d(TAG, "DOCK FOUND");
						String temp = SONR.LoadPreferences();
						String[] prefs = null;
						if(temp != null)
							prefs = temp.split("[:,]");
						if(prefs != null) {
							Log.d(TAG, "DEFAULT MEDIA PLAYER FOUND");
							SONRClient newclient = new SONRClient(this, findAudioRecord(), SONR.bufferSize, (AudioManager) this.getSystemService(Context.AUDIO_SERVICE));
							newclient.onCreate();
							newclient.StartListener();
							MakeNotification();
							Intent mediaApp = new Intent();
							mediaApp.setClassName(prefs[0], prefs[1]);
							mediaApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							this.startActivity(mediaApp);
						} else {
							Log.d(TAG, "NO DEFAULT MEDIA PLAYER");
							Intent i = new Intent(this, SONR.class);
							i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							this.startActivity(i);
						}
						updateIconON();
					} else {	//dock not found, probably headphones
						Log.d(TAG, "DOCK NOT FOUND");
						return;
					}
				} else {
					updateIconOFF();
				}
				
				if( state == 2 )
				{
					if( !isRoutingHeadset() )
					{
						/**
						 * Only change the headset toggle if not currently routing headset.
						 * If currently routing headset and the headset was unplugged the OS takes care of this for us.
						 */
						toggleHeadset();
					}
				}
			}
			else if( intent.getAction().equals(HeadphoneReciever.ACTION_POWER_CONNECTED)) 
			{
				/**
				 * Do nothing - but this intent should wake the service up and allow us to catch HEADSET_PLUG
				 */
				Log.d(TAG,"Caught POWER_CONNECTED_INTENT");
			}
			else if( intent.getAction().equals(HeadphoneReciever.ACTION_POWER_DISCONNECTED)) 
			{
				/**
				 * Do nothing - but this intent should wake the service up and allow us to refresh the icon if we were previously asleep
				 */
				Log.d(TAG,"Caught POWER_DISCONNECTED_INTENT");
			}
		}
	}
	
	private void MakeNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = R.drawable.sonr_icon;
		CharSequence tickerText = "SONR Connected";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		notification.flags |= Notification.FLAG_NO_CLEAR;
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "SONR";
		CharSequence contentText = "Disconnect from dock";
		Intent notificationIntent = new Intent(this, StopSONR.class);
		//notificationIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

		mNotificationManager.notify(SONR_ID, notification);
	}
	
	/**
	 * Called when the service is destroyed (low memory conditions).  We may miss
	 * notification of headset plug
	 */
	public void onDestroy() {
		SERVICE_ON = false;
		Log.i(TAG,"onDestroy");
		unregisterReceiver(headsetReceiver);
	}
	
	/**
	 * Toggles the current headset setting.  If currently routed headset, routes to
	 * speaker.  If currently routed to speaker routes to headset
	 */
	public void toggleHeadset() {
	    AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
		Log.d(TAG,"toggleHeadset"); 
    	if( isRoutingHeadset() )
    	{
    		Log.d(TAG,"route to earpiece"); 
    		if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
	    		/* see AudioService.setRouting
	    		* Use MODE_INVALID to force headset routing change */
	            manager.setRouting(AudioManager.MODE_INVALID, 0, AudioManager.ROUTE_HEADSET );
    		} else {
                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
                setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "");
    		}
    	}
    	else 
    	{
    		Log.d(TAG,"route to headset"); 
    		if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
	    		/* see AudioService.setRouting
	    		* Use MODE_INVALID to force headset routing change */
	            manager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET );
    		} else {
                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
    		}
    	}
	}
	
	/**
	 * Checks whether we are currently routing to headset
	 * @return true if routing to headset, false if routing somewhere else
	 */
	public boolean isRoutingHeadset() {
		boolean isRoutingHeadset = false;
		
		if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
			/*
			 * The code that works and is tested for Donut...
			 */
			AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
			
			int routing = manager.getRouting(AudioManager.MODE_NORMAL);
	    	Log.d(TAG,"getRouting returns " + routing); 
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
                Method getDeviceConnectionState = audioSystem.getMethod(
                        "getDeviceConnectionState", int.class, String.class);

                int retVal = (Integer)getDeviceConnectionState.invoke(audioSystem, DEVICE_IN_WIRED_HEADSET, "");
                
                isRoutingHeadset = (retVal == 1);
		    	Log.d(TAG,"getDeviceConnectionState " + retVal); 

            } catch (Exception e) {
                Log.e(TAG, "Could not determine status in isRoutingHeadset(): " + e);
            }
		}
    	return isRoutingHeadset; 
	}
	
	/**
	 * Updates the icon of the appwidget based on the current status of headphone routing
	 */
	public void updateIconON() {
		RemoteViews view = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);
		view.setImageViewResource(R.id.Icon, R.drawable.sonr_on);
		
	    // Create an Intent to launch toggle headset
	    Intent toggleIntent = new Intent(this, ToggleSONR.class);
	    toggleIntent.setAction(INTENT_USER_TOGGLE_REQUEST);
	    PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

	    // Get the layout for the App Widget and attach an on-click listener to the icon
	    view.setOnClickPendingIntent(R.id.Icon, pendingIntent);
		
        ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, view);
	}
	
	public void updateIconOFF() {
		RemoteViews view = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);
		view.setImageViewResource(R.id.Icon, R.drawable.sonr_off);
		
	    // Create an Intent to launch toggle headset
	    Intent toggleIntent = new Intent(this, ToggleSONR.class);
	    toggleIntent.setAction(INTENT_USER_TOGGLE_REQUEST);
	    PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

	    // Get the layout for the App Widget and attach an on-click listener to the icon
	    view.setOnClickPendingIntent(R.id.Icon, pendingIntent);
		
        ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, view);
	}
	
	/*public void updateIcon() {
    	Log.d(TAG,"updateIcon"); 

        RemoteViews view = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);

        if( isRoutingHeadset() )
        {
        	Log.d(TAG,"Routing Headset"); 
            view.setImageViewResource(R.id.Icon, R.drawable.sonr_on);
        }
        else
        {
        	Log.d(TAG,"Not Routing Headset"); 
            view.setImageViewResource(R.id.Icon, R.drawable.sonr_off);
        }
        
	    // Create an Intent to launch toggle headset
	    Intent toggleIntent = new Intent(this, ToggleSONR.class);
	    toggleIntent.setAction(INTENT_USER_TOGGLE_REQUEST);
	    PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

	    // Get the layout for the App Widget and attach an on-click listener to the icon
	    view.setOnClickPendingIntent(R.id.Icon, pendingIntent);
	    
        // Push update for this widget to the home screen
        ComponentName thisWidget = new ComponentName(this, SonrWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(thisWidget, view);
	}*/
	
	
	/**
	 * set device connection state through reflection for Android 2.1, 2.2, 2.3, maybe others.
	 * Thanks Adam King!
	 * @param device
	 * @param state
	 * @param address
	 */
    static public void setDeviceConnectionState(final int device, final int state, final String address) {
        try {
            Class<?> audioSystem = Class.forName("android.media.AudioSystem");
            Method setDeviceConnectionState = audioSystem.getMethod(
                    "setDeviceConnectionState", int.class, int.class, String.class);

            setDeviceConnectionState.invoke(audioSystem, device, state, address);
        } catch (Exception e) {
            Log.e(TAG, "setDeviceConnectionState failed: " + e);
        }
    }
    
	private AudioRecord findAudioRecord() {
        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
            for (short channelConfig : new short[] { AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.CHANNEL_IN_DEFAULT }) {
                try {
                    Log.d("SONR", "Attempting rate " + SONR.SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: "
                            + channelConfig);
                    bufferSize = 2*AudioRecord.getMinBufferSize(SONR.SAMPLE_RATE, channelConfig, audioFormat);

                    if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                        // check if we can instantiate and have a success
                        AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, SONR.SAMPLE_RATE, channelConfig, audioFormat, bufferSize);

                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                            return recorder;
                    }
                } catch (Exception e) {
                    Log.e("SONR", SONR.SAMPLE_RATE + "Exception, keep trying.",e);
                }
            }
        }
	    return null;
	}
	
}