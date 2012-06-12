/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;


import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.sonrlabs.prod.sonr.R;

public class FeedbackActivity
      extends Activity {

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.feedback_activity);
      Button sendFeedbackButton = (Button) findViewById(R.id.sendFeedbackButton);
      sendFeedbackButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            startActivity(new FeedbackIntent(FeedbackActivity.this));
         }
      });
   }

}
