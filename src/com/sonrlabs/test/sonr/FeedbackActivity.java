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
