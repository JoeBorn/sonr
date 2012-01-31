package com.sonrlabs.test.sonr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


public class IntroScreen
      extends Activity {

   private static final String PANDORA_PKG = "market://details?id=com.pandora.android";
   private static final String WINAMP_PKG = "market://details?id=com.nullsoft.winamp";

   private static final String COM_SONRLABS_SONR = "com.sonrlabs.sonr";

   private static final Pattern pattern = Pattern.compile("terms of service and privacy policy");
   private static final TransformFilter transformFilter = new TransformFilter() {
      @Override
      public final String transformUrl(final Matcher match, String url) {
         return ".TermsScreen://";
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
         setContentView(R.layout.intro);
         TextView t2 = (TextView) findViewById(R.id.intro_msg);
         t2.setText(Html.fromHtml(getResources().getText(R.string.home_text).toString()));
         Linkify.addLinks(t2, pattern, COM_SONRLABS_SONR, null, transformFilter);
         t2.setMovementMethod(LinkMovementMethod.getInstance());
      } catch (RuntimeException e) {
         Log.d("IntroScreen", e.toString());
         //ErrorReporter.getInstance().handleException(e);
      }
   }

   public void recommendedPlayers(View view) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      switch (view.getId()) {
         case R.id.winampButton:
            intent.setData(Uri.parse(WINAMP_PKG));
            break;
         case R.id.pandoraButton:
            intent.setData(Uri.parse(PANDORA_PKG));
            break;
      }
      startActivity(intent);
   }
   
   @Override
   public void onBackPressed() {
      // do nothing
   }

   public void acceptTerms(View view) {
      Preferences.savePreference(this, SONR.DEFAULT_PLAYER_SELECTED, false);
      finish();
   }
}
