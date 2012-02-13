package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;
import com.sonrlabs.prod.sonr.R;

public class TermsScreen extends Activity {

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
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

   public void terms(View view) {
      switch (view.getId()) {
         case R.id.agree:
            Preferences.savePreference(this, getString(R.string.DEFAULT_PLAYER_SELECTED), false);
            break;
         case R.id.disagree:
            startActivity(new Intent(this, StopSONR.class));
            break;
      }
      finish();
   }
}
