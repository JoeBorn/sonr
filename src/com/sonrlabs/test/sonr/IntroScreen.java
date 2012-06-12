/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.view.View;
import android.widget.TextView;

import com.sonrlabs.prod.sonr.R;

public class IntroScreen extends Activity {

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
      setContentView(R.layout.intro);
      TextView t2 = (TextView) findViewById(R.id.intro_msg);
      t2.setText(Html.fromHtml(getResources().getText(R.string.home_text).toString()));
      Linkify.addLinks(t2, pattern, "com.sonrlabs.sonr", null, transformFilter);
      t2.setMovementMethod(LinkMovementMethod.getInstance());
   }

   public void recommendedPlayers(View view) {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      switch (view.getId()) {
         case R.id.winampButton:
            intent.setData(Uri.parse(getString(R.string.WINAMP_PKG)));
            break;
         case R.id.pandoraButton:
            intent.setData(Uri.parse(getString(R.string.PANDORA_PKG)));
            break;
      }
      startActivity(intent);
   }
   
   @Override
   public void onBackPressed() {
      // do nothing
   }

   public void acceptTerms(View view) {
      setResult(Activity.RESULT_OK);
      finish();
   }
}
