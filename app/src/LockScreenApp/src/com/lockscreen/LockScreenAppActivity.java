package com.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class LockScreenAppActivity extends Activity {
	private LayoutParams _layoutParams;
	private ImageView _droid, _home;
	
	private int _wWidth;
	private int _wHeight;
	private int home_x, home_y;
	private int[] droidpos;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.main);
		_droid = (ImageView) findViewById(R.id.droid);

		if (getIntent() != null && getIntent().hasExtra("kill") && getIntent().getExtras().getInt("kill") == 1) {
			finish();
		}

		try {
			startService(new Intent(this, MagnificationService.class));

			StateListener phoneStateListener = new StateListener(this);
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

			_wWidth = getWindowManager().getDefaultDisplay().getWidth();
			_wHeight = getWindowManager().getDefaultDisplay().getHeight();

			MarginLayoutParams marginParams2 = new MarginLayoutParams(_droid.getLayoutParams());
			marginParams2.setMargins((_wWidth / 24) * 10,((_wHeight / 32) * 8), 0, 0);

			RelativeLayout.LayoutParams layoutdroid = new RelativeLayout.LayoutParams(marginParams2);
			_droid.setLayoutParams(layoutdroid);

			LinearLayout homelinear = (LinearLayout) findViewById(R.id.homelinearlayout);
			homelinear.setPadding(0, 0, 0, (_wHeight / 32) * 3);
			_home = (ImageView) findViewById(R.id.home);
			
			MarginLayoutParams marginParams1 = new MarginLayoutParams(_home.getLayoutParams());
			marginParams1.setMargins((_wWidth / 24) * 10, 0,(_wHeight / 32) * 8, 0);
			LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(marginParams1);

			_home.setLayoutParams(layout);
			_droid.setOnTouchListener(onTouch);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	View.OnTouchListener onTouch = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			_layoutParams = (LayoutParams) v.getLayoutParams();

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					int[] hompos = new int[2];
					droidpos = new int[2];
					_home.getLocationOnScreen(hompos);
					home_x = hompos[0];
					home_y = hompos[1];
					break;
	
				case MotionEvent.ACTION_MOVE:
					int x_cord = (int) event.getRawX();
					int y_cord = (int) event.getRawY();
	
					if (x_cord > _wWidth - (_wWidth / 24)) {
						x_cord = _wWidth - (_wWidth / 24) * 2;
					}
					if (y_cord > _wHeight - (_wHeight / 32)) {
						y_cord = _wHeight - (_wHeight / 32) * 2;
					}
	
					_layoutParams.leftMargin = x_cord;
					_layoutParams.topMargin = y_cord;
	
					_droid.getLocationOnScreen(droidpos);
					v.setLayoutParams(_layoutParams);
	
					if (((x_cord - home_x) <= (_wWidth / 24) * 5 && (home_x - x_cord) <= (_wWidth / 24) * 4) && ((home_y - y_cord) <= (_wHeight / 32) * 5)) {
						v.setVisibility(View.GONE);
						finish();
					}
					break;
					
				case MotionEvent.ACTION_UP:
					int x_cord1 = (int) event.getRawX();
					int y_cord2 = (int) event.getRawY();
	
					if (!((x_cord1 - home_x) <= (_wWidth / 24) * 5 && (home_x - x_cord1) <= (_wWidth / 24) * 4) && !((home_y - y_cord2) <= (_wHeight / 32) * 5)) {
						_layoutParams.leftMargin = (_wWidth / 24) * 10;
						_layoutParams.topMargin = (_wHeight / 32) * 8;
						v.setLayoutParams(_layoutParams);
					}
					break;
					
				default:
					break;
			}
			return true;
		}
	};
	
	@Override
	public void onAttachedToWindow() {
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG| WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.TYPE_KEYGUARD);
		super.onAttachedToWindow();
	}

	public void onSlideTouch(View view, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				break;
			case MotionEvent.ACTION_MOVE:
				int x_cord = (int) event.getRawX();
				int y_cord = (int) event.getRawY();
	
				if (x_cord > _wWidth) {
					x_cord = _wWidth;
				}
				if (y_cord > _wHeight) {
					y_cord = _wHeight;
				}
	
				_layoutParams.leftMargin = x_cord - 25;
				_layoutParams.topMargin = y_cord - 75;
				view.setLayoutParams(_layoutParams);
				break;
			default:
				break;
		}
	}

	@Override
	public void onBackPressed() {
		return;
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || (keyCode == KeyEvent.KEYCODE_POWER) || (keyCode == KeyEvent.KEYCODE_VOLUME_UP) || (keyCode == KeyEvent.KEYCODE_CAMERA)) {
			return true;
		}
		if ((keyCode == KeyEvent.KEYCODE_HOME)) {
			return false;
		}
		return false;
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_POWER || (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) || (event.getKeyCode() == KeyEvent.KEYCODE_POWER)) {
			return false;
		}
		if ((event.getKeyCode() == KeyEvent.KEYCODE_HOME)) {
			return false;
		}
		return false;
	}

}