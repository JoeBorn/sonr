// This is what happens when you open the SONR app on the home screen.
// It will initially just launch the service and provide a debug console.

package com.smartphonebeuro.sonr;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;



public class SONR extends ListActivity {
	public static final int SAMPLE_RATE = 44100;		//In Hz
	public static final int SONR_ID = 1;
	private List<ApplicationInfo> infos = null;
	private List<ResolveInfo> rinfos = null;
	private SONRClient theclient;
    public static int bufferSize = 0;
    private AudioRecord theaudiorecord = null;
    private Intent headsetservice = null;
    private AudioManager m_amAudioManager;
    private String prefs;
    
    public static boolean SONR_ON = false;
    public static final String DISCONNECT_ACTION = "android.intent.action.DISCONNECT_DOCK";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SONR_ON = true;
		
		prefs = LoadPreferences();
		
		registerReceiver(StopReceiver, new IntentFilter(DISCONNECT_ACTION));
		
		infos = convert(findActivities(this));
		ListAdapter adapter = new AppInfoAdapter(this, infos);
		this.setListAdapter(adapter);
		
		
		m_amAudioManager = (AudioManager) SONR.this.getSystemService(Context.AUDIO_SERVICE);

		//if(!ToggleSONR.SERVICE_ON) {
		//	startService(new Intent(this, ToggleSONR.class));
		//}
		
		theaudiorecord = findAudioRecord();
		theclient = new SONRClient(this, theaudiorecord, bufferSize, m_amAudioManager);
		theclient.onCreate();
	    
		setContentView(R.layout.main);
		
		final CheckBox checkBox = (CheckBox) findViewById(R.id.default_player);
		
		if(prefs != null) {
			String[] prefar = prefs.split(",");
		    if(prefar[0] != null) {
		        checkBox.setChecked(true);
		    }
		}
	}
	
	public static String LoadPreferences() {
		try {
			FileInputStream fstream = new FileInputStream("/sdcard/SONR/pref");
			//file format: DEFAULT:[true/false]
			//	  if true: [package name],[class name]
		
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			String line = br.readLine();
			String[] split = line.split("[:,]");
			if(split[0].compareTo("DEFAULT") == 0 && split[1].compareTo("true") == 0) {
				String temp = br.readLine();
				fstream.close();
				return temp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public void WritePreferences(String prefs) {
		try {
			File sdcard = Environment.getExternalStorageDirectory();
			File dir = new File (sdcard.getAbsolutePath() + "/SONR");
			dir.mkdirs();
			File file = new File(dir, "pref");
			FileOutputStream outstream = new FileOutputStream(file);
			String[] split = prefs.split("[:,]");
			if(split[0].compareTo("DEFAULT") == 0 && split[1].compareTo("true") == 0) {
				outstream.write("DEFAULT:true\n".getBytes());
				outstream.write((split[2] + "," + split[3]).getBytes());
			} else outstream.write("DEFAULT:false\n".getBytes());
			
			outstream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Override
	//public void onResume() {
	//}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		if(!theclient.found_dock)
			theclient.SearchSignal();

		if(theclient.found_dock) {
			ApplicationInfo ai = infos.get(position);
			Intent mediaApp = new Intent();
			rinfos = findActivitiesForPackage(this, ai.packageName);
			ResolveInfo ri = rinfos.get(0);
			mediaApp.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
			mediaApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			final CheckBox checkBox = (CheckBox) findViewById(R.id.default_player);
			if(checkBox.isChecked()) {
				WritePreferences("DEFAULT:true," + ri.activityInfo.packageName + "," + ri.activityInfo.name);
			} else WritePreferences("DEFAULT:false");
	
			MakeNotification();
			theclient.StartListener();
			this.startActivity(mediaApp);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Dock not detected, check connections and try again");
			builder.setCancelable(false);
			builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {		//retry
			           public void onClick(DialogInterface dialog, int id) {}
			       });
			
			builder.create();
			builder.show();
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
	
	@Override
	public void onBackPressed() {
		onDestroy();
	}
	
	@Override
	public void onDestroy() {		//shut it down
		super.onDestroy();
		try {
			this.unregisterReceiver(StopReceiver);
		} catch(Exception e) {}
		if(headsetservice != null) this.stopService(headsetservice);
		if(theaudiorecord != null) theaudiorecord.release();
		finish();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if(resultCode == 0) {
	    	onDestroy();
	    }
	}
	
	private static List<ResolveInfo> findActivitiesForPackage(Context context,
			String packageName) {
		final PackageManager packageManager = context.getPackageManager();

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps = packageManager.queryIntentActivities(
				mainIntent, 0);
		final List<ResolveInfo> matches = new ArrayList<ResolveInfo>();

		if (apps != null) {
			// Find all activities that match the packageName
			int count = apps.size();
			for (int i = 0; i < count; i++) {
				final ResolveInfo info = apps.get(i);
				final ActivityInfo activityInfo = info.activityInfo;
				if (packageName.equals(activityInfo.packageName)) {
					matches.add(info);
				}
			}
		}

		return matches;
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		if (item.getItemId() == R.id.item01) {
			// TODO About Dialog
			// Dialog d = new Dialog(this);
			// d.setTitle("Something");
			// d.show();
		}

		return true;
	}

	private static List<ApplicationInfo> convert(List<ResolveInfo> infos) {
		final List<ApplicationInfo> result = new ArrayList<ApplicationInfo>();

		final Set<ApplicationInfo> apps = new HashSet<ApplicationInfo>();
		for (ResolveInfo r : infos) {
			if(r.activityInfo.packageName.contains("music") || r.activityInfo.packageName.contains("Music") || r.activityInfo.packageName.contains("Pandora") || r.activityInfo.packageName.contains("pandora") || r.activityInfo.packageName.contains("winamp") || r.activityInfo.packageName.contains("Winamp")
					|| r.activityInfo.name.contains("music") || r.activityInfo.name.contains("Music") || r.activityInfo.name.contains("Pandora") || r.activityInfo.name.contains("winamp") || r.activityInfo.name.contains("Winamp"))
				apps.add(r.activityInfo.applicationInfo);
		}

		result.addAll(apps);

		return result;
	}

	private static List<ResolveInfo> findActivities(Context context) {
		final PackageManager packageManager = context.getPackageManager();

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> activities = packageManager
				.queryIntentActivities(mainIntent, 0);

		return activities;
	}	

	private static class AppInfoAdapter extends BaseAdapter {
		public List<ApplicationInfo> ApplicationInfos = new ArrayList<ApplicationInfo>();
		private final LayoutInflater mInflater;
		private final PackageManager pm;

		public AppInfoAdapter(Context c) {
			mInflater = (LayoutInflater) c
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			pm = c.getPackageManager();
		}

		public AppInfoAdapter(Context c,
				Collection<? extends ApplicationInfo> applicationInfos) {
			this(c);
			this.ApplicationInfos.addAll(applicationInfos);
		}

		@Override
		public int getCount() {
			return this.ApplicationInfos.size();
		}

		@Override
		public Object getItem(int position) {
			return ApplicationInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return this.ApplicationInfos.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ApplicationInfo info = this.ApplicationInfos.get(position);

			if (convertView == null) {
				convertView = mInflater.inflate(
						R.layout.manage_applications_item, null);

			}

			TextView name = (TextView) convertView.findViewById(R.id.app_name);
			name.setText(info.loadLabel(pm));

			ImageView icon = (ImageView) convertView
					.findViewById(R.id.app_icon);
			icon.setImageDrawable(info.loadIcon(pm));
			TextView description = (TextView) convertView
					.findViewById(R.id.app_size);
			description.setText(info.loadDescription(pm));

			return convertView;
		}

	}
	
	private final BroadcastReceiver StopReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
		    // Handle reciever
		    String mAction = intent.getAction();

		    if(mAction.equals(DISCONNECT_ACTION)) {
		      onDestroy();
		    }
		}
	};
	
	
	private AudioRecord findAudioRecord() {
        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
            for (short channelConfig : new short[] { AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.CHANNEL_IN_DEFAULT }) {
                try {
                    Log.d("SONR", "Attempting rate " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: "
                            + channelConfig);
                    bufferSize = 2*AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);

                    if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                        // check if we can instantiate and have a success
                        AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, SAMPLE_RATE, channelConfig, audioFormat, bufferSize);

                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                            return recorder;
                    }
                } catch (Exception e) {
                    Log.e("SONR", SAMPLE_RATE + "Exception, keep trying.",e);
                }
            }
        }
	    return null;
	}
	
	private void ForceRouting() {
		if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
    		/* see AudioService.setRouting
    		* Use MODE_INVALID to force headset routing change */
			m_amAudioManager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET );
		} else {
			ToggleSONR.setDeviceConnectionState(ToggleSONR.DEVICE_IN_WIRED_HEADSET, ToggleSONR.DEVICE_STATE_AVAILABLE, "");
			ToggleSONR.setDeviceConnectionState(ToggleSONR.DEVICE_OUT_WIRED_HEADSET, ToggleSONR.DEVICE_STATE_AVAILABLE, "");
		}
	}
}