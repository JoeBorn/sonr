// This is what happens when you open the SONR app on the home screen.

package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.acra.ErrorReporter;

import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.Menu;
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

import com.flurry.android.FlurryAgent;
import com.sonrlabs.test.sonr.common.Common;
import com.sonrlabs.test.sonr.common.DialogCommon;
import com.sonrlabs.test.sonr.signal.AudioProcessor;

public class SONR
      extends ListActivity {

   public static final String TAG = SONR.class.getSimpleName();
   public static final int SONR_ID = 1;
   public static final String DOCK_NOT_FOUND = "DOCK NOT FOUND";
   public static final String DOCK_FOUND = "DOCK FOUND";
   public static final String DEFAULT_PLAYER_SELECTED = "DEFAULT_PLAYER_SELECTED";
   public static final String APP_PACKAGE_NAME = "APP_PACKAGE_NAME";
   public static boolean SONR_ON = false;
   public static boolean MAIN_SCREEN = false;
   public static final String DISCONNECT_ACTION = "android.intent.action.DISCONNECT_DOCK";
   public static final String SHARED_PREFERENCES = "SONRSharedPreferences";
   private final static HashMap<String, String> flurryParams = new HashMap<String, String>();


   public static int bufferSize = 0;

   private static final String SAMPLE_URI = "\\";
   private static final String AUDIO_MIME_TYPE = "audio/*";
   private static final String DOCK_NOT_FOUND_TRY_AGAIN = "Dock not detected, check connections and try again";
   private static final String OK_TXT = "Ok";
   private static final String SELECT_PLAYER = "Please select a music player";
   private static final String APP_FULL_NAME = "APP_FULL_NAME";
   private static final String PLAYER_SELECTED = "PLAYER_SELECTED";
   private static final String FIRST_LAUNCH = "FIRST_LAUNCH";
   private List<ApplicationInfo> infos = null;
   private int currentlySelectedApplicationInfoIndex;
   private SONRClient theclient;
   private AudioRecord theaudiorecord = null;
   private AudioManager m_amAudioManager;

   private final BroadcastReceiver StopReceiver = new StopReceiver();
   private boolean isRegistered = false;
   private PowerManager.WakeLock mWakeLock;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      try {
         super.onCreate(savedInstanceState);

         Common.save(this, SONR.PLAYER_SELECTED, false);

         // LogFile.MakeLog("\n\nSONR CREATE");
         MAIN_SCREEN = true;

         currentlySelectedApplicationInfoIndex = -1;

         setContentView(R.layout.music_select_main);
         
         if (isFirstLaunch()){
            //Show Intro screen
            Intent intent = new Intent(this, IntroScreen.class);
            startActivity(intent);
            Common.save(this, SONR.FIRST_LAUNCH, false);
         }

         if (!isRegistered) {
            registerReceiver(StopReceiver, new IntentFilter(DISCONNECT_ACTION));
            isRegistered = true;
         }

         infos = convert(this, findActivities(this));
         ListAdapter adapter = new AppInfoAdapter(this, infos);
         this.setListAdapter(adapter);

         m_amAudioManager = (AudioManager) SONR.this.getSystemService(Context.AUDIO_SERVICE);
         int savedNotificationVolume = m_amAudioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
         SharedPreferences sharedPreferences = getSharedPreferences(SONR.SHARED_PREFERENCES, 0);
         sharedPreferences.edit().putInt("savedNotificationVolume", savedNotificationVolume).commit();

         if (!ToggleSONR.SERVICE_ON) {
            Intent i = new Intent(this, ToggleSONR.class);
            startService(i);
         }

         theaudiorecord = AudioProcessor.findAudioRecord();
         theclient = new SONRClient(this, theaudiorecord, bufferSize, m_amAudioManager);
         theclient.onCreate();

         final Button noneButton = (Button) findViewById(R.id.noneButton);
         noneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               new CheckDockOnNoneSelection(v).start();
            }
         });

         final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
         this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
         this.mWakeLock.acquire();

         //Intent intent;
         if (Common.get(this, SONR.DEFAULT_PLAYER_SELECTED, false)) {
            theclient.onDestroy();
            if (theaudiorecord != null) {
               theaudiorecord.release();
            }
            //intent = new Intent(this, DefaultAppScreen.class);
         } else {
            //intent = new Intent(this, IntroScreen.class);
         }
         //startActivity(intent);
      } catch (RuntimeException e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);
      }
   }

   private boolean isFirstLaunch() {
      // Restore preferences
      return Common.get(this, SONR.FIRST_LAUNCH, true);
   }
   
   @Override
   protected void onListItemClick(ListView listView, View clickedView, int position, long id) {
      super.onListItemClick(listView, clickedView, position, id);

      ApplicationInfo ai;
      ai = infos.get(position);
      List<ResolveInfo> rinfos = findActivitiesForPackage(this, ai.packageName);
      ResolveInfo ri = rinfos.get(0);

      Common.save(this, SONR.APP_PACKAGE_NAME, ri.activityInfo.packageName);
      Common.save(this, SONR.APP_FULL_NAME, ri.activityInfo.name);
      Common.save(this, SONR.PLAYER_SELECTED, true);

      currentlySelectedApplicationInfoIndex = position;
      listView.invalidateViews();
   }

   public void buttonOK(View view) {
      try {
         if (!Common.get(this, SONR.PLAYER_SELECTED, false) && !Common.get(this, SONR.DEFAULT_PLAYER_SELECTED, false)) {
            DialogCommon.quickPopoutDialog(this, false, SELECT_PLAYER, OK_TXT);
         } else {
            new CheckDockOnPlayerSelection(view).start();
         }
      } catch (RuntimeException e) {
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

      CharSequence contentTitle = TAG;
      CharSequence contentText = "Disconnect from dock";
      Intent notificationIntent = new Intent(ctx, StopSONR.class);
      PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

      notification.setLatestEventInfo(ctx, contentTitle, contentText, contentIntent);

      mNotificationManager.notify(SONR_ID, notification);
   }

   @Override
   public void onBackPressed() {
      // finish(); Do Nothing?
   }
   
   @Override
   public void onStart()
   {
      super.onStart();
      FlurryAgent.onStartSession(this, "NNCR41GZ52ZYBXPZPTGT");
   }
   
   @Override
   public void onStop()
   {
      super.onStop();
      FlurryAgent.onEndSession(this);
   }
   
   @Override
   public void onDestroy() { // shut it down
      try {
         // LogFile.MakeLog("SONR DESTROY\n\n");
         try {
            this.unregisterReceiver(StopReceiver);
            isRegistered = false;
         } catch (RuntimeException e) {
            // log something?
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
      } catch (RuntimeException e) {
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
      if (m_amAudioManager == null) {
         m_amAudioManager = (AudioManager) SONR.this.getSystemService(Context.AUDIO_SERVICE);
      }
      m_amAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 1, AudioManager.FLAG_VIBRATE);
      
      if(theaudiorecord == null)
      {
         theaudiorecord = AudioProcessor.findAudioRecord();
         theclient = new SONRClient(this, theaudiorecord, bufferSize, m_amAudioManager);
         theclient.onCreate();
      }
         
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
      super.onActivityResult(requestCode, resultCode, data);
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
         for (ResolveInfo info : apps) {
            ActivityInfo activityInfo = info.activityInfo;
            if (packageName.equals(activityInfo.packageName)) {
               matches.add(info);
            }
         }
      }

      return matches;
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      boolean showMenu = super.onCreateOptionsMenu(menu);
      try {
         this.getMenuInflater().inflate(R.menu.main, menu);
      } catch (InflateException e) {
         Log.d(TAG, "Unable to inflate menu: " + e);
         showMenu = false;
      }
      return showMenu;
   }

   private static List<ApplicationInfo> convert(Context c, Collection<ResolveInfo> infos) {
      final List<ApplicationInfo> result = new ArrayList<ApplicationInfo>();

      final Set<ApplicationInfo> apps = new HashSet<ApplicationInfo>();
      for (ResolveInfo resolveInfo : infos) {

         if (!Common.get(c, SONR.DEFAULT_PLAYER_SELECTED, false)) {

            String[] appNames = c.getResources().getStringArray(R.array.mediaAppsNames);

            for (String appName : appNames) {
               String pkgName = resolveInfo.activityInfo.packageName.toLowerCase();
               String actvName = resolveInfo.activityInfo.name.toLowerCase();

               if (pkgName.contains(appName) || actvName.contains(appName)) {
                  apps.add(resolveInfo.activityInfo.applicationInfo);
               }
            }
         } else {
            String packageName = Common.get(c, SONR.APP_PACKAGE_NAME, Common.N_A);
            String appName = Common.get(c, SONR.APP_FULL_NAME, Common.N_A);

            if (packageName.equals(resolveInfo.activityInfo.packageName) && appName.equals(resolveInfo.activityInfo.name)) {
               result.add(resolveInfo.activityInfo.applicationInfo);
            }
            return result;
         }
      }

      result.addAll(apps);

      return result;
   }

   private static Collection<ResolveInfo> findActivities(Context context) {
      Map<String, ResolveInfo> finalMap = new HashMap<String, ResolveInfo>();

      final PackageManager packageManager = context.getPackageManager();

      final Intent musicPlayerIntent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER, null);
      final Intent musicPlayFromSearchIntent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH, null);
      final Intent mediaSearchIntent = new Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH, null);
      final Intent viewIntent = new Intent(Intent.ACTION_VIEW, null);
      viewIntent.setDataAndType(Uri.parse(SAMPLE_URI), AUDIO_MIME_TYPE);

      final Intent allLauncherAppsIntent = new Intent(Intent.ACTION_MAIN, null);
      allLauncherAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);

      List<ResolveInfo> queryResults = new ArrayList<ResolveInfo>();
      queryResults.addAll(packageManager.queryIntentActivities(musicPlayerIntent, 0));
      queryResults.addAll(packageManager.queryIntentActivities(musicPlayFromSearchIntent, 0));
      queryResults.addAll(packageManager.queryIntentActivities(mediaSearchIntent, 0));
      queryResults.addAll(packageManager.queryIntentActivities(viewIntent, 0));
      queryResults.addAll(packageManager.queryIntentActivities(allLauncherAppsIntent, 0));

      for (ResolveInfo resolveInfo : queryResults) {
         if (!finalMap.containsKey(resolveInfo.activityInfo.packageName)) {
            finalMap.put(resolveInfo.activityInfo.packageName, resolveInfo);
         }
      }

      return finalMap.values();
   }

   /**
    *  TODO: replace this junk with javadoc for this class.
    */
   private final class StopReceiver
         extends BroadcastReceiver {
      @Override
      public void onReceive(Context context, Intent intent) {
         // Handle reciever
         String mAction = intent.getAction();

         if (DISCONNECT_ACTION.equals(mAction)) {
            finish();
         }
      }
   }

   /**
    * Run the signal search in its own thread to avoid blocking the main thread.
    */
   private abstract class DockChecker
         extends Thread {
   
      private final View view;
      
      DockChecker(View view) {
         setDaemon(true);
         this.view = view;
      }
      
      @Override
      public void run() {
         if (!theclient.foundDock()) {
            theclient.searchSignal();
         }
         /*
          * Only the search runs in this thread. The response runs in the main
          * thread.
          */
         Runnable task;
         if (theclient.foundDock()) {
            theclient.startListener();
            task = new Runnable() {
               @Override
               public void run() {
                  dockFound();
               }
            };
            view.post(task);
         } else {
            task = new Runnable() {
               @Override
               public void run() {
                  dockNotFound();
               }
            };
         }
         view.post(task);
      }
      
      void dockFound() {
         Toast.makeText(getApplicationContext(), DOCK_FOUND, Toast.LENGTH_SHORT).show();
      }
      
      void dockNotFound() {
         Toast.makeText(getApplicationContext(), DOCK_NOT_FOUND, Toast.LENGTH_SHORT).show();
         DialogCommon.quickPopoutDialog(SONR.this, false, DOCK_NOT_FOUND_TRY_AGAIN, OK_TXT);
      }
   }

   private class CheckDockOnPlayerSelection
         extends DockChecker {
      
      CheckDockOnPlayerSelection(View view) {
         super(view);
      }

      @Override
      void dockFound() {
         super.dockFound();
         final CheckBox checkBox = (CheckBox) findViewById(R.id.checkbox_default_player);
         Start(SONR.this, checkBox.isChecked());
      }
   }

   private class CheckDockOnNoneSelection
         extends DockChecker {
      
      CheckDockOnNoneSelection(View view) {
         super(view);
      }

      @Override
      void dockFound() {
         super.dockFound();
         SONR_ON = true;
         MakeNotification(getApplicationContext());
         
         Intent startMain = new Intent(Intent.ACTION_MAIN);
         startMain.addCategory(Intent.CATEGORY_HOME);
         startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         startActivity(startMain);
      }
      
      @Override
      void dockNotFound() {
         // do nothing
      }
   
   }

   private class AppInfoAdapter
         extends BaseAdapter {
      public final List<ApplicationInfo> ApplicationInfos = new ArrayList<ApplicationInfo>();
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
            convertView = mInflater.inflate(R.layout.manage_applications_item, null);
         }

         TextView name = (TextView) convertView.findViewById(R.id.app_name);
         name.setText(info.loadLabel(pm));

         ImageView icon = (ImageView) convertView.findViewById(R.id.app_icon);
         icon.setImageDrawable(info.loadIcon(pm));
         TextView description = (TextView) convertView.findViewById(R.id.app_size);
         description.setText(info.loadDescription(pm));

         convertView.setBackgroundColor(currentlySelectedApplicationInfoIndex == position ? 0xFF666666 : 0xFF444444);

         return convertView;
      }

   }

   public static void Start(Context context, boolean defaultplayer) {
      Common.save(context, SONR.DEFAULT_PLAYER_SELECTED, defaultplayer);

      SONR_ON = true;
      MakeNotification(context);

      if (Common.get(context, SONR.PLAYER_SELECTED, false)) {
         flurryParams.put("MediaPlayer", SONR.APP_FULL_NAME);
         FlurryAgent.logEvent("APP_FULL_NAME", flurryParams);
         Intent mediaApp = new Intent();
         mediaApp.setClassName(Common.get(context, SONR.APP_PACKAGE_NAME, Common.N_A),
                               Common.get(context, SONR.APP_FULL_NAME, Common.N_A));
         mediaApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         context.startActivity(mediaApp);
      }
   }
}