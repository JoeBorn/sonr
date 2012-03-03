package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.sonrlabs.prod.sonr.R;

public class FeedbackActivity extends Activity {
   CheckBox checkBox1;
   CheckBox checkBox2;
   CheckBox checkBox3;
   CheckBox checkBox4;
   EditText additionalInfoEditText;
   Button sendFeedbackButton;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.feedback_activity);
      checkBox1 = (CheckBox) findViewById(R.id.checkBox1);
      checkBox2 = (CheckBox) findViewById(R.id.checkBox2);
      checkBox3 = (CheckBox) findViewById(R.id.checkBox3);
      checkBox4 = (CheckBox) findViewById(R.id.checkBox4);
      additionalInfoEditText = (EditText) findViewById(R.id.additionalnfoEditText);
      sendFeedbackButton = (Button) findViewById(R.id.sendFeedbackButton);
      sendFeedbackButton.setOnClickListener(new View.OnClickListener() {
         public void onClick(View v) {
             sendFeedback();
         }
     });
   }
   
   private void sendFeedback()
   {
      Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  
      
      String manufacturer = new String(Build.MANUFACTURER);
      String model = new String(Build.MODEL);
      String os = new String(Build.VERSION.RELEASE);
      String deviceInfo = "Manufacturer: " + manufacturer +" " + "Model: " + model + "" + "OS: " + os;
      
      String form = new String();
      
      if(checkBox1.isChecked())
      {
         form = form + checkBox1.getText().toString();
      }
      
      if(checkBox2.isChecked())
      {
         form = form + checkBox2.getText().toString();
      }
      
      if(checkBox3.isChecked())
      {
         form = form + checkBox3.getText().toString();
      }
      
      if(checkBox4.isChecked())
      {
         form = form + checkBox4.getText().toString();
      }
      
      String extraComments = "Extra Comments: " + additionalInfoEditText.getText().toString();
      
      String emailList[] = { "info@sonrlabs.com"};  
      emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, emailList);  
      emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "SONR Feedback");
      emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Feedback: " + form + extraComments + deviceInfo);
      emailIntent.setType("plain/text");    
        
      startActivity(emailIntent);
   }
}
