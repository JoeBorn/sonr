/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

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
