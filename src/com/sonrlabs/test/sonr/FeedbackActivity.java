package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.sonrlabs.prod.sonr.R;

public class FeedbackActivity
      extends Activity {
   private CheckBox checkBox1;
   private CheckBox checkBox2;
   private CheckBox checkBox3;
   private CheckBox checkBox4;
   private EditText additionalInfoEditText;
   private Button sendFeedbackButton;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.feedback_activity);
      checkBox1 = (CheckBox) findViewById(R.id.checkBox1);
      checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
      checkBox3 = (CheckBox) findViewById(R.id.checkBox3);
      checkBox4 = (CheckBox) findViewById(R.id.checkBox4);
      additionalInfoEditText = (EditText) findViewById(R.id.additionalnfoEditText);
      sendFeedbackButton = (Button) findViewById(R.id.sendFeedbackButton);
      sendFeedbackButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            startActivity(new FeedbackIntent());
         }
      });
   }

   private final class FeedbackIntent
         extends Intent {

      FeedbackIntent() {
         super(ACTION_SEND);
         String emailList[] = {
            "info@sonrlabs.com"
         };
         putExtra(EXTRA_EMAIL, emailList);
         putExtra(EXTRA_SUBJECT, "SONR Feedback");
         setType("plain/text");
         StringBuilder builder = new StringBuilder();
         constructFeedbackText(builder);
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

      void constructFeedbackText(StringBuilder form) {
         form.append("Feedback");
         form.append('\n');
         form.append("---");
         form.append('\n');

         if (checkBox1.isChecked()) {
            form.append(checkBox1.getText());
            form.append('\n');
         }

         if (checkBox2.isChecked()) {
            form.append(checkBox2.getText());
            form.append('\n');
         }

         if (checkBox3.isChecked()) {
            form.append(checkBox3.getText());
            form.append('\n');
         }

         if (checkBox4.isChecked()) {
            form.append(checkBox4.getText());
            form.append('\n');
         }

         form.append("---");
         form.append('\n');
         form.append("Comments: ").append(additionalInfoEditText.getText());
         form.append('\n');

         form.append("---");
         form.append('\n');

         form.append("Manufacturer: ").append(Build.MANUFACTURER);
         form.append('\n');

         form.append("Model: ").append(Build.MODEL);
         form.append('\n');

         form.append("Android Version: ").append(Build.VERSION.RELEASE).append(": Sdk: ").append(Build.VERSION.SDK_INT);
         form.append('\n');

         form.append("Hardware: ").append(Build.HARDWARE);
         form.append('\n');

         form.append("Radio: ").append(Build.RADIO);
         form.append('\n');

         form.append("SONR: ");
         PackageManager packageManager = getPackageManager();
         try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            form.append(packageInfo.versionName);
         } catch (NameNotFoundException e) {
            form.append("Unable to determine version");
         }
         form.append('\n');

         form.append("Installed Apps: ");
         getInstalledApps(form, packageManager).append('\n');
      }
   }

}
