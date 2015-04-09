package com.example.magnificationvideo.mv;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback, PreviewCallback {
	private SurfaceHolder _holder;
	private Camera mCamera;
	private String TAG = "";
	private boolean _finished = false, _endOfTimer = false, hasTouched = false, launchedTimer = false;
	private Size mPreviewSize, _previewSize;
	private SurfaceView _surfaceView;
	private List<Size> _supportedPreviewSizes;
	private Context _context;
	private DisplayedFace _df;
	private Activity _a;
	private int _rotationCompensation = 0;
	private int _width, _height;
	private LinkedList<Double> _averages = new LinkedList<Double>();
	private LinkedList<byte[]> _bytes = new LinkedList<byte[]>();
	private LinkedList<RectF> _rects = new LinkedList<RectF>();
	private int cpt = 0;
	private int PMIN = 5, PMAX = 200, FRAME = 15;

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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (hasTouched == false) {
			_bytes.clear();
			_averages.clear();
			_rects.clear();
			launchedTimer = false;
			hasTouched = true;
			_endOfTimer = false;
		}
		return super.onTouchEvent(event);
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
			_df = ((DisplayedFace) (((Activity) getContext()).findViewById(R.id.viewfinder_view)));
			_df.setFaces(Arrays.asList(faces));
		}
	};

	public void onPreviewFrame(byte[] data, Camera camera) {
		if (_finished) {
			return;
		}
		RectF rect = null;
		if (_df != null) {
			rect = _df.getRect();
		}
		if (rect != null) {
			int left = Math.abs((int) rect.left);

			if (left != 0) {
				if (!launchedTimer) {
					_previewSize = camera.getParameters().getPreviewSize();
					timer();
					launchedTimer = true;
				}
				if(!_endOfTimer) {
					_bytes.add(data);
					_rects.add(rect);
				}
			}
		}
		mCamera.addCallbackBuffer(data);
	}

	public void timer() {
		new CountDownTimer(2300,1000) {

			public void onTick(long millisUntilFinished) {
				if(_bytes.size() > 52) {
					onFinish();
				}
			}

			public void onFinish() {
				_endOfTimer = true;
				int size = _previewSize.width * _previewSize.height;
				int[] rgb = new int[size];
				int sizeRect = 0;

				for (int i = 0; i < _bytes.size(); i++) {
					int left = Math.abs((int) _rects.get(i).left);
					int right = Math.abs((int) _rects.get(i).right);
					int top = Math.abs((int) _rects.get(i).top);
					int bottom = Math.abs((int) _rects.get(i).bottom);

					decodeYUV420RGB(rgb, _bytes.get(i), (int) _rects.get(i).width(), (int) _rects.get(i).height());

					double avg = 0;
					int k = 0, l = 0, rsize = 0;
					for (int a = top; a < bottom; a++) {
						for (int j = left; j < right; j++) {
							avg += Color.green(rgb[(k* (int) _rects.get(i).width() + a)+ (j + l)]);
							l = l + 1;
							rsize = rsize + 1;
						}
						k = k + 1;
						l = 0;
					}

					if (rsize != 0) {
						Double dAvg = Double.valueOf(avg / rsize);
						_averages.add(dAvg);
					}
				}

				System.err.print("Averages list : ");
				for (int i = 0; i < _averages.size(); i++) {
					System.err.print(" " + _averages.get(i));
				}
				System.err.println(".");

				System.err.println("La taille est de " + _averages.size());

				System.err.print("Averages list : ");
				for (int i = 0; i < _averages.size(); i++) {
					System.err.print(" " + _averages.get(i));
				}
				System.err.println(".");

				derive(_averages);
				System.err.print("Derive list : ");
				for (int i = 0; i < _averages.size(); i++) {
					System.err.print(" " + _averages.get(i));
				}
				System.err.println(".");

				amplification(_averages, 3);
				System.err.print("Amplification list : ");
				for (int i = 0; i < _averages.size(); i++) {
					System.err.print(" " + _averages.get(i));
				}
				System.err.println(".");

				double v = variations(_averages) / 4.f;
				System.err.println("Variations size : " + v * 4);
				System.err.println("Average Size : " + _averages.size());
				String str = "Nb var. " + ((v - 1) * (15.f / _averages.size())) * 60 + " "+ ((v + 1) * (15.f / _averages.size())) * 60;

				Toast.makeText(_context, str,Toast.LENGTH_LONG).show();
			}
		}.start();
	}

	public void derive(LinkedList<Double> avgs) {
		double tmp = 0;
		for (int i = 1; i < avgs.size()-1; i++) {
			tmp = avgs.get(i+1);
			avgs.set(i, (avgs.get(i) - tmp));
		}
		avgs.set(0,0.0);
	}

	public void amplification(LinkedList<Double> avgs, int factor) {
		Double[] tab = new Double[avgs.size()];
		for (int i = 0; i < avgs.size(); i++) {
			tab[i] = avgs.get(i) * factor;
		}
		for (int i = 0; i < avgs.size(); i++) {
			avgs.set(i, avgs.get(i) + tab[i]);
		}
	}

	public int variations(LinkedList<Double> avgs) {
		int i = 0;
		boolean up = false, down = false;

		for (Double y : avgs) {
			if (y < 0 && up == true) {
				i++;
			}
			if (y > 0 && down == true) {
				i++;
			}
			if (y < 0) {
				down = true;
				up = false;
			}
			if (y > 0) {
				up = true;
				down = false;
			}
		}
		return i;
	}

	public double maxValue(LinkedList<Double> avgs) {
		double max = 0;
		for (double i : avgs) {
			if (i > max)
				max = i;
		}
		return max;
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

	private void decodeYUV420RGB(int[] rgb, byte[] yuv420sp, int width, int height) {
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

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
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
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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

	private void faceDetectionSupported() {
		Camera.Parameters params = mCamera.getParameters();
		if (params.getMaxNumDetectedFaces() <= 0) {
			Log.e(TAG, "Face Detection not supported");
		} else {
			Toast toast = Toast.makeText(_context, "Face detected supported",
			Toast.LENGTH_SHORT);
			toast.show();
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
				final int scaledChildWidth = previewWidth * height / previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width / previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
			}
		}
	}
}
