package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.sonrlabs.prod.sonr.R;

public class FeedbackActivity extends Activity {
   CheckBox checkBox1;
   CheckBox checkBox2;
   CheckBox checkBox3;
   CheckBox checkBox4;
   EditText additionalInfoEditText;
   Button sendFeedbackButton;

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
         public void onClick(View v) {
             sendFeedback();
         }
     });
   }
   
   private StringBuilder getInstalledApps(StringBuilder builder, PackageManager packageManager) {
      Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
      mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
      boolean first = true;
      List<ResolveInfo> queryIntentActivities = packageManager.queryIntentActivities(mainIntent, 0);
      for (ResolveInfo info : queryIntentActivities) {
         PackageItemInfo activityInfo = info.activityInfo;
         int flags = info.activityInfo.applicationInfo.flags;
         boolean isSystemPackage = (flags & ApplicationInfo.FLAG_SYSTEM) !=0;
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

   
   private void sendFeedback()
   {
      Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  
    
      StringBuilder form = new StringBuilder();
      form.append("Feedback");
      form.append('\n');
      form.append("---");
      form.append('\n');

      if(checkBox1.isChecked())
      {
         form.append(checkBox1.getText().toString());
         form.append('\n');
      }
      
      if(checkBox2.isChecked())
      {
         form.append(checkBox2.getText().toString());
         form.append('\n');
      }
      
      if(checkBox3.isChecked())
      {
         form.append(checkBox3.getText().toString());
         form.append('\n');
      }
      
      if(checkBox4.isChecked())
      {
         form.append(checkBox4.getText().toString());
         form.append('\n');
      }
      
      form.append("---");
      form.append('\n');
      form.append("Comments: " + additionalInfoEditText.getText().toString());
      form.append('\n');
      
      form.append("---");
      form.append('\n');
      
      form.append("Manufacturer: " + Build.MANUFACTURER).toString();
      form.append('\n');

      form.append("Model: " + Build.MODEL).toString();  
      form.append('\n');

      form.append("Android Version: " + Build.VERSION.RELEASE).toString();
      form.append('\n');
      
      form.append("Hardware: " + Build.HARDWARE).toString();
      form.append('\n');
      
      form.append("Radio: " + Build.RADIO).toString();
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
      
      String emailList[] = { "info@sonrlabs.com"};  
      emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailList);  
      emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "SONR Feedback");
      emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, form.toString());
      emailIntent.setType("plain/text");    
        
      startActivity(emailIntent);
   }
}
