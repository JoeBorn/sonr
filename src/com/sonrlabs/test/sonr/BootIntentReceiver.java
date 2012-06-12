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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootIntentReceiver extends BroadcastReceiver {
   
   private static final String TAG = BootIntentReceiver.class.getSimpleName();

   @Override
   public void onReceive(Context context, Intent intent) {
      try {
         if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SonrLog.d(TAG, "Boot Intent Received");
            Intent i = new Intent(context, SonrService.class);
            context.startService(i);
         }
      } catch (RuntimeException e) {
         SonrLog.e(TAG, "Boot Intent Received");
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
