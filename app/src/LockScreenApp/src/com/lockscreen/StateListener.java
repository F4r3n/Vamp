package com.lockscreen;

import android.app.Activity;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class StateListener extends PhoneStateListener {
	private Context _ctx;
	
	public StateListener(Context ctx) {
		super();
		_ctx = ctx;
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);
		switch (state) {
		case TelephonyManager.CALL_STATE_RINGING:
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			((Activity)_ctx).finish();
			break;
		case TelephonyManager.CALL_STATE_IDLE:
			break;
		}
	}
}
