package com.example.magnificationvideo.mv;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback,
		PreviewCallback {
	private SurfaceHolder _holder;
	private Camera mCamera;
	private String TAG = "";
	private boolean _finished = false, _endOfTimer = false;
	private Size mPreviewSize;
	private SurfaceView _surfaceView;
	private List<Size> _supportedPreviewSizes;
	private Context _context;
	private DisplayedFace _df;
	private Activity _a;
	private int _rotationCompensation = 0;
	private int _width, _height;
	private LinkedList<Integer> _averages = new LinkedList<Integer>();
	private boolean isCounting = false;

	public CameraPreview(Context context, AttributeSet attr) {
		super(context, attr);

		_context = context;
		_a = (Activity) context;
		_surfaceView = new SurfaceView(context);
		addView(_surfaceView);

		_holder = _surfaceView.getHolder();
		_holder.addCallback(this);
		_holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		mCamera = camera;
		if (mCamera != null) {
			_supportedPreviewSizes = mCamera.getParameters()
					.getSupportedPreviewSizes();
			requestLayout();
			faceDetectionSupported();
			mCamera.setFaceDetectionListener(fdl);
		}
	}

	Camera.FaceDetectionListener fdl = new Camera.FaceDetectionListener() {
		@Override
		public void onFaceDetection(Camera.Face[] faces, Camera camera) {
			Log.d("facedetection", "Faces Found: " + faces.length);
			_df = ((DisplayedFace) (((Activity) getContext())
					.findViewById(R.id.viewfinder_view)));
			_df.setFaces(Arrays.asList(faces));

		}

	};

	public void onPreviewFrame(byte[] data, Camera camera) {
		if (_finished) {
			return;
		}
		Size previewSize = camera.getParameters().getPreviewSize();

		int size = previewSize.width * previewSize.height;
		int[] rgb = new int[size];

		decodeYUV420RGB(rgb, data, previewSize.width, previewSize.height);

		RectF rect = null;
		if (_df != null) {
			rect = _df.getRect();
		}
		if (rect != null) {

			int left = Math.abs((int) rect.left);
			int right = Math.abs((int) rect.right);
			int top = Math.abs((int) rect.top);
			int bottom = Math.abs((int) rect.bottom);

			// On lance dès qu'on obtient un rect valide
			if (left != 0 && isCounting == false) {
				timer();
				isCounting = true;
			}

			// On calcule tant que le timer est encore en cours
			if (!_endOfTimer) {
				int avg = 0;
				int k = 0, l = 0, rsize = 0;
				for (int i = top; i < bottom; i++) {
					for (int j = left; j < right; j++) {
						avg += (int) Color
								.green(rgb[(k * (int) rect.width() + i)
										+ (j + l)]);
						l = l + 1;
						rsize = rsize + 1;
					}
					k = k + 1;
					l = 0;
				}
				// Au début, on a des valeurs nulles donc on vérifie qu'on
				// parcours bien qqch
				if (rsize != 0) {
					Integer iInt = Integer.valueOf(avg / rsize);
					_averages.add(iInt);
				}
			}
		}

		mCamera.addCallbackBuffer(data);
	}

	public void timer() {
		new CountDownTimer(4500, 1000) {

			public void onTick(long millisUntilFinished) {
			}

			public void onFinish() {
				_endOfTimer = true;
				// System.err.println((Integer)_averages.size());

			}
		}.start();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.setRotation(90);
				mCamera.setParameters(parameters);
				mCamera.setFaceDetectionListener(fdl);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	// public void changeOrientation() {
	// Camera.Parameters parameters = mCamera.getParameters();
	// System.out.println("test");
	//
	// if (getResources().getConfiguration().orientation ==
	// Configuration.ORIENTATION_PORTRAIT){
	// parameters.set("orientation", "portrait");
	// parameters.set("rotation",90);
	// mCamera.setDisplayOrientation(90);
	//
	// } else if (getResources().getConfiguration().orientation ==
	// Configuration.ORIENTATION_LANDSCAPE){
	// parameters.set("orientation", "landscape");
	// parameters.set("rotation", 90);
	// mCamera.setDisplayOrientation(90);
	// }
	// }

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		requestLayout();
		mCamera.setPreviewCallback(this);
		mCamera.startPreview();
		mCamera.startFaceDetection();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (_holder != null) {
			_holder.removeCallback(this);
			_holder = null;
		}
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.setPreviewCallback(null);
			mCamera.release();
			mCamera = null;
		}
	}

	private void decodeYUV420RGB(int[] rgb, byte[] yuv420sp, int width,
			int height) {
		final int frameSize = width * height;
		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0) {
					r = 0;
				} else if (r > 262143) {
					r = 262143;
				}
				if (g < 0) {
					g = 0;
				} else if (g > 262143) {
					g = 262143;
				}
				if (b < 0) {
					b = 0;
				} else if (b > 262143) {
					b = 262143;
				}

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
				continue;
			}
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public int getDisplayRotation(Activity activity) {
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();

		switch (rotation) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return rotation;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (_supportedPreviewSizes != null) {
			mPreviewSize = getOptimalPreviewSize(_supportedPreviewSizes, width,
					height);
		}
	}

	/*
	 * float ratio; if(mPreviewSize.height >= mPreviewSize.width) ratio =
	 * (float) mPreviewSize.height / (float) mPreviewSize.width; else ratio =
	 * (float) mPreviewSize.width / (float) mPreviewSize.height;
	 * 
	 * // One of these methods should be used, second method squishes preview
	 * slightly
	 * 
	 * Camera.Parameters parameters = mCamera.getParameters(); }
	 */

	private void faceDetectionSupported() {

		Camera.Parameters params = mCamera.getParameters();
		if (params.getMaxNumDetectedFaces() <= 0) {

			Log.e(TAG, "Face Detection not supported");
		} else {
			// Toast toast = Toast.makeText(_context, "Face detected supported",
			// Toast.LENGTH_SHORT);
			// toast.show();
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (mPreviewSize != null) {
				previewWidth = mPreviewSize.width;
				previewHeight = mPreviewSize.height;
			}

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height
						/ previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0,
						(width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width
						/ previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width,
						(height + scaledChildHeight) / 2);
			}
		}
	}

}