/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.widget.CheckBox;
import android.widget.EditText;

import com.sonrlabs.prod.sonr.R;

class FeedbackIntent
      extends Intent {
   private static final String emailList[] = {
      "info@sonrlabs.com"
   };
   
   FeedbackIntent(Activity activity) {
      super(ACTION_SEND);
      CheckBox[] checkBoxes = new CheckBox[] {
         (CheckBox) activity.findViewById(R.id.checkBox1),
         (CheckBox) activity.findViewById(R.id.checkBox2),
         (CheckBox) activity.findViewById(R.id.checkBox3),
         (CheckBox) activity.findViewById(R.id.checkBox4)
      };
      EditText additionalInfoEditText = (EditText) activity.findViewById(R.id.additionalnfoEditText);
      StringBuilder builder = new StringBuilder();
      constructFeedbackText(activity, builder, checkBoxes, additionalInfoEditText.getText().toString());
      putExtra(EXTRA_EMAIL, emailList);
      putExtra(EXTRA_SUBJECT, "SONR Feedback");
      setType("plain/text");
      putExtra(EXTRA_TEXT, builder.toString());
   }

   private StringBuilder getInstalledApps(StringBuilder builder, PackageManager packageManager) {
      Intent mainIntent = new Intent(ACTION_MAIN, null);
      mainIntent.addCategory(CATEGORY_LAUNCHER);
      boolean first = true;
      List<ResolveInfo> queryIntentActivities = packageManager.queryIntentActivities(mainIntent, 0);
      for (ResolveInfo info : queryIntentActivities) {
         PackageItemInfo activityInfo = info.activityInfo;
         int flags = info.activityInfo.applicationInfo.flags;
         boolean isSystemPackage = (flags & ApplicationInfo.FLAG_SYSTEM) != 0;
         if (isSystemPackage) {
            // ignore system packages
            continue;
         }
         if (!first) {
            builder.append(", ");
         }
         String name = activityInfo.name;
         builder.append(name);
         first = false;
      }
      return builder;
   }

   private void constructFeedbackText(Activity activity, StringBuilder form, CheckBox[] checkBoxes, String comment) {
      form.append("Feedback");
      form.append('\n');
      form.append("---");
      form.append('\n');

      for (CheckBox box : checkBoxes) {
         if (box.isChecked()) {
            form.append(box.getText()).append("\n");
         }
      }

      form.append("---");
      form.append('\n');
      form.append("Comments: ").append(comment);
      form.append('\n');

      form.append("---");
      form.append('\n');

      form.append("Manufacturer: ").append(Build.MANUFACTURER);
      form.append('\n');

      form.append("Model: ").append(Build.MODEL);
      form.append('\n');

      form.append("Android Version: ").append(Build.VERSION.RELEASE).append(", SDK: ").append(Build.VERSION.SDK_INT);
      form.append('\n');

      form.append("Hardware: ").append(Build.HARDWARE);
      form.append('\n');

      form.append("Radio: ").append(Build.RADIO);
      form.append('\n');

      form.append("SONR: ");
      PackageManager packageManager = activity.getPackageManager();
      try {
         PackageInfo packageInfo = packageManager.getPackageInfo(activity.getPackageName(), 0);
         form.append(packageInfo.versionName);
      } catch (NameNotFoundException e) {
         form.append("Unable to determine version");
      }
      form.append('\n');

      form.append("Installed Apps: ");
      getInstalledApps(form, packageManager).append('\n');
   }
}