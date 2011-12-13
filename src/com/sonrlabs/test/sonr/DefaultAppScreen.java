package com.sonrlabs.test.sonr;

import java.util.List;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class DefaultAppScreen
      extends Activity {
   private SONRClient theclient;
   String[] app_package_and_name = new String[2];

   @Override
   public void onCreate(Bundle savedInstanceState) {
      try {

         super.onCreate(savedInstanceState);
         setContentView(R.layout.default_app_main);

         String prefs = SONR.LoadPreferences();
         String[] prefar = prefs.split(",");
         app_package_and_name[0] = prefar[0];
         app_package_and_name[1] = prefar[1];

         List<ApplicationInfo> info = SONR.convert(SONR.findActivities(this), app_package_and_name);

         ImageView icon = (ImageView) findViewById(R.id.img_default_app_icon);
         icon.setImageDrawable(info.get(0).loadIcon(getPackageManager()));

      } catch (Exception e) {
         e.printStackTrace();
         ErrorReporter.getInstance().handleException(e);

      }
   }

   public void buttonContinue(View view) {
      if (!ToggleSONR.SERVICE_ON) {
         Intent i = new Intent(this, ToggleSONR.class);
         startService(i);
      }

      theclient =
            new SONRClient(this, SONR.findAudioRecord(), SONR.bufferSize,
                           (AudioManager) this.getSystemService(Context.AUDIO_SERVICE));
      theclient.createListener();
      theclient.searchSignal();

      if (theclient.foundDock()) {
         SONR.Start(app_package_and_name, this, true);
         finish();
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

   public void buttonChangeSettings(View view) {
      SONR.WritePreferences("DEFAULT, false");
      Intent i = new Intent(this, SONR.class);
      i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      this.startActivity(i);
      finish();
   }

   @Override
   public void onBackPressed() {
      Intent stopintent = new Intent(this, StopSONR.class);
      stopintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(stopintent);
      finish();
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
   }
}
