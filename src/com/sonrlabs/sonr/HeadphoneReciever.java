/*
 * Copyright (C) 2009 Dan Walkes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sonrlabs.sonr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * 
 * This class receives the HEADSET_PLUG intent, passes back to the service
 * for handling.
 * 
 * @author Dan Walkes
 *
 */

public class HeadphoneReciever extends BroadcastReceiver {
	private static final String TAG = HeadphoneReciever.class.getName();
	public static final String HEADSET_PLUG_INTENT = "android.intent.action.HEADSET_PLUG";
	public static final String ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";
	public static final String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
	
	public void onReceive(Context context, Intent intent ) {
		Log.d(TAG, "Receive intent= " + intent );
		Intent serviceIntent = new Intent(context, ToggleSONR.class);
		if( intent.getAction() != null ) {
			serviceIntent.setAction(intent.getAction());
		}
		
		if( intent.getExtras() != null ) {
			serviceIntent.putExtras(intent.getExtras());
		}
		context.startService(serviceIntent);
	}
}
