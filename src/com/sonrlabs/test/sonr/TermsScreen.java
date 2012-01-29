package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;


public class TermsScreen
      extends Activity {
   @Override
   public void onCreate(Bundle savedInstanceState) {
      try {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.terms);

         TextView terms = (TextView) findViewById(R.id.text_terms);
         terms.setMovementMethod(new ScrollingMovementMethod());
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }

   @Override
   public void onBackPressed() {
      // do nothing
   }

   public void acceptTerms(View view) {
      Preferences.savePreference(this, SONR.DEFAULT_PLAYER_SELECTED, false);
      finish();
   }

   public void declineTerms(View view) {
      try {
         Intent i = new Intent(this, StopSONR.class);
         startActivity(i);
         finish();
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
