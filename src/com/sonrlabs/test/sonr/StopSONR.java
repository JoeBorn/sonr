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
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

public class StopSONR extends Activity {
   
   private static final String TAG = StopSONR.class.getSimpleName();
   
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      try {
         SonrLog.d(TAG, TAG);
         // LogFile.MakeLog("StopSONR sending broadcast");

         NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
         mNotificationManager.cancel(SonrService.SONR_ID);

         sendBroadcast(new Intent(SonrActivity.DISCONNECT_ACTION));
         finish();
         
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
