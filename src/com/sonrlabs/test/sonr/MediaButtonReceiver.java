/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This receiver intercepts non specific button events from other apps.
 * 
 * Fix for the Last person dialed and Play/Pause behavior on the Moto. Atrix.
 * Phone would initiate calls, IIRC when plugged in to dock-- JB
 * 
 */
public class MediaButtonReceiver extends BroadcastReceiver {

   @Override
   public void onReceive(Context context, Intent intent) {
      abortBroadcast();
   }
}
