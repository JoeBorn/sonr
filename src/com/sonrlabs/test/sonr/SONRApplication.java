package com.sonrlabs.test.sonr;

//import org.acra.ACRA;
//import org.acra.ReportingInteractionMode;
//import org.acra.annotation.ReportsCrashes;

import android.app.Application;

//@ReportsCrashes(formKey = "dGROUU9tcVNBY3lRSmdPMW5uQzBNMHc6MQ", mode = ReportingInteractionMode.TOAST, forceCloseDialogAfterToast = false, resToastText = R.string.crash_toast_text)
public class SONRApplication
      extends Application {
   @Override
   public void onCreate() {
      // The following line triggers the initialization of ACRA
      //ACRA.init(this);
      super.onCreate();
   }
}
