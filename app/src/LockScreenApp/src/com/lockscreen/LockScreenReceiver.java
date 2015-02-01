package com.lockscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LockScreenReceiver extends BroadcastReceiver {
	public static boolean _wasScreenOn = true;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			_wasScreenOn = false;
			Intent intentLockScreen = new Intent(context, LockScreenAppActivity.class);
			intentLockScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intentLockScreen);
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			_wasScreenOn = true;
			Intent intentLockScreen = new Intent(context, LockScreenAppActivity.class);
			intentLockScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		} else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent intentLockScreen = new Intent(context, LockScreenAppActivity.class);
			intentLockScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intentLockScreen);
		}
	}

}