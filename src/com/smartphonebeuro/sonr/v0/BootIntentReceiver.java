package com.smartphonebeuro.sonr.v0;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BootIntentReceiver extends BroadcastReceiver {
	private static final String TAG = "BootIntentReceiver";
	  @Override
	  public void onReceive(Context context, Intent intent) {
		//getApplicationContext().registerReceiver(headphonerec, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	  }
}
