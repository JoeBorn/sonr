package com.sonrlabs.test.sonr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.sonrlabs.test.sonr.common.Common;

public class IntroScreen
      extends Activity {

   private static final String COM_SONRLABS_SONR = "com.sonrlabs.sonr";

   private static Pattern pattern = Pattern.compile("terms of service and privacy policy");
   private static TransformFilter transformFilter = new TransformFilter() {
      @Override
      public final String transformUrl(final Matcher match, String url) {
         return ".TermsScreen://";
      }
   };

   @Override
   public void onCreate(Bundle savedInstanceState) {
      try {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.intro);
         TextView t2 = (TextView) findViewById(R.id.intro_msg);
         Linkify.addLinks(t2, pattern, COM_SONRLABS_SONR, null, transformFilter);
         t2.setMovementMethod(LinkMovementMethod.getInstance());
      } catch (RuntimeException e) {
         Log.d(SONR.TAG, e.toString());
         ErrorReporter.getInstance().handleException(e);
      }
   }

   @Override
   public void onBackPressed() {
      // do nothing
   }

   public void acceptTerms(View view) {
      Common.save(this, SONR.DEFAULT_PLAYER_SELECTED, false);
      finish();
   }
}
