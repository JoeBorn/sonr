package com.sonrlabs.test.sonr;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "dE5ULWJIMmswOFpHWEdDWnB2RDhIVXc6MQ", mode = ReportingInteractionMode.TOAST, forceCloseDialogAfterToast = false, resToastText = R.string.crash_toast_text)
public class SONRApplication extends Application {
	@Override
	public void onCreate() {
		// The following line triggers the initialization of ACRA
		ACRA.init(this);
		super.onCreate();
	}
}
