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
import android.content.IntentFilter;

class SONRClient {

   private final static String TAG = SONRClient.class.getSimpleName();
   
   private MicSerialListener singletonListener;
   private Context applicationContext;
   private boolean clientStopRegistered;
   
   private final BroadcastReceiver clientStopReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
         if (intent != null && SonrActivity.DISCONNECT_ACTION.equals(intent.getAction())) {
            onDestroy();
         }
      }
   };

   SONRClient(Context context) {
      this.applicationContext = context.getApplicationContext();
      this.clientStopRegistered = false;
   }

   boolean foundDock() {
      return singletonListener != null && singletonListener.foundDock();
   }

   void startListener() {
      if (!singletonListener.isAlive()) {
         // LogFile.MakeLog("Start Listener");
         try {
            MicSerialListener.startNewListener(singletonListener);
         } catch (RuntimeException e) {
            e.printStackTrace();
            //ErrorReporter.getInstance().handleException(e);
         }
      }
   }

   public void createListener() {
      try {
         synchronized (this) {
            SonrLog.d(TAG, "createListener()");
            unregisterReceiver();
            registerReceiver();
            UserActionHandler controller = new UserActionHandler(applicationContext);
            AudioProcessorQueue.setUserActionHandler(controller);
            if (singletonListener != null) {
               singletonListener.stopRunning();
            }
            singletonListener = new MicSerialListener();
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }


   public void onDestroy() {
      try {
         synchronized (this) {
            unregisterReceiver();
            if (singletonListener != null) {
               singletonListener.stopRunning();
            }
         }
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }

   private void registerReceiver() {
      applicationContext.registerReceiver(clientStopReceiver, new IntentFilter(SonrActivity.DISCONNECT_ACTION));
      clientStopRegistered = true;
      SonrLog.i(TAG, "Registered broadcast receiver " + clientStopReceiver + " in context " + applicationContext);
   }

   private void unregisterReceiver() {
      try {
         if (clientStopRegistered) {
            applicationContext.unregisterReceiver(clientStopReceiver);
            clientStopRegistered = false;
            SonrLog.i(TAG, "Unregistered broadcast receiver " + clientStopReceiver + " in context " + applicationContext);
         }
      } catch (RuntimeException e) {
         SonrLog.e(TAG, String.format("Failed to unregister broadcast receiver %s in context %s. \n Exception: %s", clientStopReceiver, applicationContext, e));
      }
   }
}
