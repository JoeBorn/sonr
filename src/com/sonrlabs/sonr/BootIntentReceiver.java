package com.sonrlabs.sonr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BootIntentReceiver extends BroadcastReceiver {
	private static final String TAG = "BootIntentReceiver";
	  @Override
	  public void onReceive(Context context, Intent intent) {
		//getApplicationContext().registerReceiver(headphonerec, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
	  }
}
