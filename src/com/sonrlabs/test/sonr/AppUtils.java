/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.MediaStore;

import com.sonrlabs.prod.sonr.R;

class AppUtils {
   
   static final String TAG = AppUtils.class.getSimpleName();
   
   static List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
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

   static Collection<ResolveInfo> findActivities(Context context) {
      Map<String, ResolveInfo> finalMap = new HashMap<String, ResolveInfo>();

      final PackageManager packageManager = context.getPackageManager();

      final Intent musicPlayerIntent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER, null);
      //final Intent musicPlayFromSearchIntent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH, null);
      final Intent mediaSearchIntent = new Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH, null);
      final Intent viewIntent = new Intent(Intent.ACTION_VIEW, null);
      viewIntent.setDataAndType(Uri.parse(context.getString(R.string.SAMPLE_URI)), context.getString(R.string.AUDIO_MIME_TYPE));

      final Intent allLauncherAppsIntent = new Intent(Intent.ACTION_MAIN, null);
      allLauncherAppsIntent.addCategory(Intent.CATEGORY_LAUNCHER);

      List<ResolveInfo> queryResults = new ArrayList<ResolveInfo>();
      queryResults.addAll(packageManager.queryIntentActivities(musicPlayerIntent, 0));
      //queryResults.addAll(packageManager.queryIntentActivities(musicPlayFromSearchIntent, 0));
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
    * TODO: Simplify this method
    * 
    * @param c
    * @param activities
    * @return
    */
   static List<ApplicationInfo> convert(Context c, Collection<ResolveInfo> activities) {
      final List<ApplicationInfo> result = new ArrayList<ApplicationInfo>();

      final Set<ApplicationInfo> apps = new HashSet<ApplicationInfo>();
      for (ResolveInfo resolveInfo : activities) {

         if (!Preferences.getPreference(c, c.getString(R.string.DEFAULT_PLAYER_SELECTED), false)) {

            String[] appNames = c.getResources().getStringArray(R.array.mediaAppsNames);

            for (String appName : appNames) {
               String pkgName = resolveInfo.activityInfo.packageName.toLowerCase();
               String actvName = resolveInfo.activityInfo.name.toLowerCase();

               if (pkgName.contains(appName) || actvName.contains(appName)) {
                  apps.add(resolveInfo.activityInfo.applicationInfo);
               }
            }
         } else {
            String packageName = Preferences.getPreference(c, c.getString(R.string.APP_PACKAGE_NAME), Preferences.N_A);
            String appName = Preferences.getPreference(c, c.getString(R.string.APP_FULL_NAME), Preferences.N_A);

            if (packageName.equals(resolveInfo.activityInfo.packageName) && appName.equals(resolveInfo.activityInfo.name)) {
               result.add(resolveInfo.activityInfo.applicationInfo);
            }
            return result;
         }
      }

      result.addAll(apps);

      return result;
   }
   
   static void doStart(Context context, boolean defaultplayer) {
      Preferences.savePreference(context, context.getString(R.string.DEFAULT_PLAYER_SELECTED), defaultplayer);
      
      boolean mediaPlayerSelected = Preferences.getPreference(context, context.getString(R.string.PLAYER_SELECTED), false);
      
      SonrLog.d(TAG, "media player selected: " + mediaPlayerSelected);
      
      if (mediaPlayerSelected) {
         SonrLog.d(TAG,Preferences.getPreference(context, context.getString(R.string.APP_PACKAGE_NAME), Preferences.N_A));
         Intent mediaApp = new Intent();
         mediaApp.setClassName(Preferences.getPreference(context, context.getString(R.string.APP_PACKAGE_NAME), Preferences.N_A),
                               Preferences.getPreference(context, context.getString(R.string.APP_FULL_NAME), Preferences.N_A));
         mediaApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
         context.startActivity(mediaApp);
      }
   }
}
