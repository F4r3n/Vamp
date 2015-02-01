package com.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StartLockScreen extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startService(new Intent(this,MagnificationService.class));
		finish();
	}

}
