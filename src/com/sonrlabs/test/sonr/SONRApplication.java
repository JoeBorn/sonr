/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

//import org.acra.ACRA;
//import org.acra.ReportingInteractionMode;
//import org.acra.annotation.ReportsCrashes;
// <-- uses-permission android:name="android.permission.INJECT_EVENTS"/>

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
