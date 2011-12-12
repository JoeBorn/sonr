// This is what happens when you open the SONR app on the home screen.

package com.sonrlabs.test.sonr;

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

import org.acra.ErrorReporter;

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
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SONR
      extends ListActivity {
   public static final int SAMPLE_RATE = 44100; // In Hz
   public static final int SONR_ID = 1;
   private List<ApplicationInfo> infos = null;
   private List<ResolveInfo> rinfos = null;
   private SONRClient theclient;
   public static int bufferSize = 0;
   private AudioRecord theaudiorecord = null;
   private AudioManager m_amAudioManager;
   private String prefs;
   private boolean isRegistered = false;
   private String[] app_package_and_name = null;

   public static boolean SONR_ON = false;
   public static boolean MAIN_SCREEN = false;
   public static final String DISCONNECT_ACTION = "android.intent.action.DISCONNECT_DOCK";
   public static final String SHARED_PREFERENCES = "SONRSharedPreferences";

   private SharedPreferences sharedPreferences;
   protected PowerManager.WakeLock mWakeLock;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      try {
         super.onCreate(savedInstanceState);
         // LogFile.MakeLog("\n\nSONR CREATE");
         MAIN_SCREEN = true;

         setContentView(R.layout.music_select_main);

         if (!isRegistered) {
            registerReceiver(StopReceiver, new IntentFilter(DISCONNECT_ACTION));
            isRegistered = true;
         }

         infos = convert(findActivities(this), null);
         ListAdapter adapter = new AppInfoAdapter(this, infos);
         this.setListAdapter(adapter);

         m_amAudioManager = (AudioManager) SONR.this.getSystemService(Context.AUDIO_SERVICE);
         int savedNotificationVolume = m_amAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
         sharedPreferences = getSharedPreferences(SONR.SHARED_PREFERENCES, 0);
         sharedPreferences.edit().putInt("savedNotificationVolume", savedNotificationVolume).commit();

         if (!ToggleSONR.SERVICE_ON) {
            Intent i = new Intent(this, ToggleSONR.class);
            startService(i);
         }

         theaudiorecord = findAudioRecord();
         theclient = new SONRClient(this, theaudiorecord, bufferSize, m_amAudioManager);
         theclient.onCreate();

         prefs = LoadPreferences();
         if (prefs != null && prefs != "..") {
            String[] prefar = prefs.split(",");
            if (prefar[0] != null) {
               theclient.onDestroy();
               if (theaudiorecord != null) {
                  theaudiorecord.release();
               }
               Intent i = new Intent(this, DefaultAppScreen.class);
               startActivity(i);
            }
         }

         if (prefs == null) {
            Intent i = new Intent(this, IntroScreen.class);
            startActivity(i);
         }

         final Button noneButton = (Button) findViewById(R.id.noneButton);
         noneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if (!theclient.foundDock()) {
                  theclient.searchSignal();
               }

               if (theclient.foundDock()) {
                  Toast.makeText(getApplicationContext(), "DOCK FOUND", Toast.LENGTH_SHORT).show();
                  theclient.startListener();
                  SONR_ON = true;
                  MakeNotification(getApplicationContext());

                  Intent startMain = new Intent(Intent.ACTION_MAIN);
                  startMain.addCategory(Intent.CATEGORY_HOME);
                  startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  startActivity(startMain);
               }
            }
         });

         final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
         this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "SONR");
         this.mWakeLock.acquire();

      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   public static String LoadPreferences() {
      try {
         FileInputStream fstream = new FileInputStream("/sdcard/SONR/pref");
         // file format: ..
         // DEFAULT:[true/false]
         // if true: [package name],[class name]

         DataInputStream in = new DataInputStream(fstream);
         BufferedReader br = new BufferedReader(new InputStreamReader(in));

         if (br.readLine().compareTo("..") != 0) {
            return null;
         }
         String line = br.readLine();
         String[] split = line.split("[:,]");
         if (split[0].compareTo("DEFAULT") == 0 && split[1].compareTo("true") == 0) {
            String temp = br.readLine();
            fstream.close();
            return temp;
         }
      } catch (Exception e) {
         return null;
         // e.printStackTrace();
      }

      return "..";
   }

   public static void WritePreferences(String prefs) {
      try {
         File sdcard = Environment.getExternalStorageDirectory();
         File dir = new File(sdcard.getAbsolutePath() + "/SONR");
         dir.mkdirs();
         File file = new File(dir, "pref");
         FileOutputStream outstream = new FileOutputStream(file);
         outstream.write("..\n".getBytes());

         String[] split = prefs.split("[:,]");
         if (split[0].compareTo("DEFAULT") == 0 && split[1].compareTo("true") == 0) {
            outstream.write("DEFAULT:true\n".getBytes());
            outstream.write((split[2] + "," + split[3]).getBytes());
         } else {
            outstream.write("DEFAULT:false\n".getBytes());
         }

         outstream.close();
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   // @Override
   // public void onResume() {
   // }

   @Override
   protected void onListItemClick(ListView l, View v, int position, long id) {
      // super.onListItemClick(l, v, position, id);
      for (int i = 0; i <= l.getLastVisiblePosition() - l.getFirstVisiblePosition(); i++) {
         if (i != position) {
            l.getChildAt(i).setBackgroundColor(0xFF444444);
         }
      }
      v.setBackgroundColor(0xFF666666);
      // v.setClickable(true);
      // test.setColorFilter(Color.parseColor("#444444"),
      // PorterDuff.Mode.DARKEN);
      ApplicationInfo ai = null;
      ai = infos.get(position);
      rinfos = findActivitiesForPackage(this, ai.packageName);
      ResolveInfo ri = rinfos.get(0);
      Log.d("MEDIA PLAYER", ri.activityInfo.packageName);

      SharedPreferences settings = getSharedPreferences(SHARED_PREFERENCES, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString("selectedMediaPlayer", ri.activityInfo.packageName);
      editor.commit();

      app_package_and_name = new String[] {
         ri.activityInfo.packageName,
         ri.activityInfo.name
      };
   }

   public void buttonOK(View view) {
      try {
         if (app_package_and_name == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please select a music player");
            builder.setCancelable(false);
            builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() { // retry
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                        }
                                     });

            builder.create();
            builder.show();
         } else {
            if (!theclient.foundDock()) {
               theclient.searchSignal();
            }

            if (theclient.foundDock()) {
               Toast.makeText(getApplicationContext(), "DOCK FOUND", Toast.LENGTH_SHORT).show();

               // LogFile.MakeLog("DOCK FOUND");

               theclient.startListener();
               final CheckBox checkBox = (CheckBox) this.findViewById(R.id.checkbox_default_player);
               Start(app_package_and_name, this, checkBox.isChecked());
            } else {
               Toast.makeText(getApplicationContext(), "DOCK NOT FOUND", Toast.LENGTH_SHORT).show();

               // LogFile.MakeLog("DOCK NOT FOUND");
               AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setMessage("Dock not detected, check connections and try again");
               builder.setCancelable(false);
               builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() { // retry
                                           @Override
                                           public void onClick(DialogInterface dialog, int id) {
                                           }
                                        });

               builder.create();
               builder.show();
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   private static void MakeNotification(Context ctx) {
      String ns = Context.NOTIFICATION_SERVICE;
      NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(ns);

      int icon = R.drawable.sonr_icon;
      CharSequence tickerText = "SONR Connected";
      long when = System.currentTimeMillis();

      Notification notification = new Notification(icon, tickerText, when);
      notification.flags |= Notification.FLAG_NO_CLEAR;

      CharSequence contentTitle = "SONR";
      CharSequence contentText = "Disconnect from dock";
      Intent notificationIntent = new Intent(ctx, StopSONR.class);
      PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

      notification.setLatestEventInfo(ctx, contentTitle, contentText, contentIntent);

      mNotificationManager.notify(SONR_ID, notification);
   }

   @Override
   public void onBackPressed() {
      // finish();
   }

   @Override
   public void onDestroy() { // shut it down
      try {
         // LogFile.MakeLog("SONR DESTROY\n\n");
         try {
            this.unregisterReceiver(StopReceiver);
            isRegistered = false;
         } catch (Exception e) {
         }
         super.onDestroy();
         if (theaudiorecord != null) {
            theaudiorecord.release();
         }
         // if(theaudiorecord != null) theaudiorecord.release();
         // if(theclient != null) theclient.onDestroy();
         MAIN_SCREEN = false;
         SONR.SONR_ON = false;
         String ns = Context.NOTIFICATION_SERVICE;
         NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
         mNotificationManager.cancel(SONR.SONR_ID);
         mWakeLock.release();
      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   @Override
   public void onPause() {
      super.onPause();
      // LogFile.MakeLog("SONR paused");
      // try {
      // this.unregisterReceiver(StopReceiver);
      // isRegistered = false;
      // } catch(Exception e) {}
   }

   @Override
   public void onResume() {
      super.onResume();
      m_amAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 1, AudioManager.FLAG_VIBRATE);

      // LogFile.MakeLog("SONR resumed");
      // if(!isRegistered) {
      // registerReceiver(StopReceiver, new IntentFilter(DISCONNECT_ACTION));
      // isRegistered = true;
      // }
   }

   @Override
   public void onRestart() {
      super.onRestart();
      // LogFile.MakeLog("SONR restarted");
      if (!isRegistered) {
         registerReceiver(StopReceiver, new IntentFilter(DISCONNECT_ACTION));
         isRegistered = true;
      }
   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (resultCode == 0) {
         finish();
      }
   }

   private static List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
      final PackageManager packageManager = context.getPackageManager();

      final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

      final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
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

      switch (item.getItemId()) {
         case R.id.aboutMenuItem:
            Log.d("SONR", "About clicked");
            break;
         case R.id.configurationMenuItem:
            Log.d("SONR", "Configuration Clicked");
            Intent intent = new Intent(SONR.this, ConfigurationActivity.class);
            startActivity(intent);
            break;
         default:
            break;
      }

      return true;
   }

   public static List<ApplicationInfo> convert(List<ResolveInfo> infos, String app_package_and_name[]) {
      final List<ApplicationInfo> result = new ArrayList<ApplicationInfo>();

      final Set<ApplicationInfo> apps = new HashSet<ApplicationInfo>();
      for (ResolveInfo r : infos) {
         if (app_package_and_name == null) {
            if (r.activityInfo.packageName.contains("music") || r.activityInfo.packageName.contains("Music")
                  || r.activityInfo.packageName.contains("Pandora") || r.activityInfo.packageName.contains("pandora")
                  || r.activityInfo.packageName.contains("winamp") || r.activityInfo.packageName.contains("Winamp")
                  || r.activityInfo.packageName.contains("audioplayer") || r.activityInfo.name.contains("music")
                  || r.activityInfo.packageName.contains("spotify") || r.activityInfo.packageName.contains("Spotify")
                  || r.activityInfo.packageName.contains("mediafly") || r.activityInfo.packageName.contains("MediaFly")
                  || r.activityInfo.packageName.contains("audible") || r.activityInfo.packageName.contains("Audible")
                  || r.activityInfo.packageName.contains("listen") || r.activityInfo.packageName.contains("Listen")
                  || r.activityInfo.packageName.contains("mp3") || r.activityInfo.packageName.contains("MP3")
                  || r.activityInfo.name.contains("Music") || r.activityInfo.name.contains("Pandora")
                  || r.activityInfo.name.contains("winamp") || r.activityInfo.name.contains("Winamp")
                  || r.activityInfo.name.contains("mediaplayer") || r.activityInfo.name.contains("Spotify")
                  || r.activityInfo.name.contains("MediaFly") || r.activityInfo.name.contains("Audible")
                  || r.activityInfo.name.contains("Listen") || r.activityInfo.name.contains("MP3")) {
               apps.add(r.activityInfo.applicationInfo);
            }
         } else if (app_package_and_name[0].compareTo(r.activityInfo.packageName) == 0
               && app_package_and_name[1].compareTo(r.activityInfo.name) == 0) {
            result.add(r.activityInfo.applicationInfo);
            return result;
         }
      }

      result.addAll(apps);

      return result;
   }

   public static List<ResolveInfo> findActivities(Context context) {
      final PackageManager packageManager = context.getPackageManager();

      final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

      final List<ResolveInfo> activities = packageManager.queryIntentActivities(mainIntent, 0);

      return activities;
   }

   private static class AppInfoAdapter
         extends BaseAdapter {
      public List<ApplicationInfo> ApplicationInfos = new ArrayList<ApplicationInfo>();
      private final LayoutInflater mInflater;
      private final PackageManager pm;

      public AppInfoAdapter(Context c) {
         mInflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         pm = c.getPackageManager();
      }

      public AppInfoAdapter(Context c, Collection<? extends ApplicationInfo> applicationInfos) {
         this(c);
         this.ApplicationInfos.addAll(applicationInfos);
      }

      // @Override
      @Override
      public int getCount() {
         return this.ApplicationInfos.size();
      }

      // @Override
      @Override
      public Object getItem(int position) {
         return ApplicationInfos.get(position);
      }

      // @Override
      @Override
      public long getItemId(int position) {
         return this.ApplicationInfos.get(position).hashCode();
      }

      // @Override
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         ApplicationInfo info = this.ApplicationInfos.get(position);

         if (convertView == null) {
            convertView = mInflater.inflate(R.layout.manage_applications_item, null);
         }

         TextView name = (TextView) convertView.findViewById(R.id.app_name);
         name.setText(info.loadLabel(pm));

         ImageView icon = (ImageView) convertView.findViewById(R.id.app_icon);
         icon.setImageDrawable(info.loadIcon(pm));
         TextView description = (TextView) convertView.findViewById(R.id.app_size);
         description.setText(info.loadDescription(pm));

         return convertView;
      }

   }

   private final BroadcastReceiver StopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         // Handle reciever
         String mAction = intent.getAction();

         if (mAction.equals(DISCONNECT_ACTION)) {
            finish();
         }
      }
   };

   public static void Start(String[] app_package_and_name, Context ctx, boolean defaultplayer) {
      Intent mediaApp = new Intent();
      mediaApp.setClassName(app_package_and_name[0], app_package_and_name[1]);
      mediaApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      if (defaultplayer) {
         WritePreferences("DEFAULT:true," + app_package_and_name[0] + "," + app_package_and_name[1]);
      } else {
         WritePreferences("DEFAULT:false");
      }

      SONR_ON = true;
      MakeNotification(ctx);
      ctx.startActivity(mediaApp);
   }

   public static AudioRecord findAudioRecord() {
      for (short audioFormat : new short[] {
         AudioFormat.ENCODING_PCM_8BIT,
         AudioFormat.ENCODING_PCM_16BIT
      }) {
         for (short channelConfig : new short[] {
            AudioFormat.CHANNEL_CONFIGURATION_MONO,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.CHANNEL_CONFIGURATION_DEFAULT,
            AudioFormat.CHANNEL_IN_DEFAULT
         }) {
            try {
               Log.d("SONR", "Attempting rate " + SAMPLE_RATE + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
               bufferSize = 3 * AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat);

               if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                  // check if we can instantiate and have a success
                  AudioRecord recorder = new AudioRecord(AudioSource.DEFAULT, SAMPLE_RATE, channelConfig, audioFormat, bufferSize);

                  if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                     return recorder;
                  }
               }
            } catch (IllegalArgumentException e) {
               /*
                * Encoding configuration not supported by current hardware.
                * These are expected, don't bother logging.
                */
            } catch (Exception e) {
               String message = "Unexpected condition while testing audio encoding.";
               Log.e("SONR", message, e);
            }
         }
      }
      Log.e("SONR", SAMPLE_RATE + " No usable audio encoding for this hardware.");
      return null;
   }
}