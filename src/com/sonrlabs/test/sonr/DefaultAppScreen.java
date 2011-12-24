package com.sonrlabs.test.sonr;

import java.util.List;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.sonrlabs.prod.sonr.R;
import com.sonrlabs.test.sonr.common.Common;
import com.sonrlabs.test.sonr.common.DialogCommon;

public class DefaultAppScreen
      extends Activity {

   private SONRClient theclient;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      try {
         setContentView(R.layout.default_app_main);

         List<ApplicationInfo> info = SONR.convert(this, SONR.findActivities(this));
         if (info.iterator().hasNext()) {
            ImageView icon = (ImageView) findViewById(R.id.img_default_app_icon);
            icon.setImageDrawable(info.iterator().next().loadIcon(getPackageManager()));
         }

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
      theclient.onCreate();
      theclient.searchSignal();

      if (theclient.foundDock()) {
         SONR.Start(this, true);
         finish();
      } else {
         Toast.makeText(getApplicationContext(), SONR.DOCK_NOT_FOUND, Toast.LENGTH_SHORT).show();
         // LogFile.MakeLog("DOCK NOT FOUND");
         DialogCommon.quickPopoutDialog(this, false, SONR.DOCK_NOT_FOUND_TRY_AGAIN, SONR.OK_TXT);
      }
   }

   public void buttonChangeSettings(View view) {
      Common.save(this, SONR.DEFAULT_PLAYER_SELECTED, false);
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
}
