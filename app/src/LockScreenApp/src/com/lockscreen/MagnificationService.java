package com.lockscreen;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class MagnificationService extends Service {
	BroadcastReceiver _receiver;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		KeyguardManager.KeyguardLock keyguardLock;
		KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		keyguardLock = km.newKeyguardLock("IN");
		keyguardLock.disableKeyguard();

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);

		_receiver = new LockScreenReceiver();
		registerReceiver(_receiver, filter);

		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(_receiver);
		super.onDestroy();
	}
}
