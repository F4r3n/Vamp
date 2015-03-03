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

public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback, PreviewCallback {
	private SurfaceHolder _holder;
	private Camera mCamera;
	private String TAG = "";
	private boolean _finished = false, _endOfTimer = false, launchedTimer = false;
	private Size mPreviewSize;
	private SurfaceView _surfaceView;
	private List<Size> _supportedPreviewSizes;
	private Context _context;
	private DisplayedFace _df;
	private Activity _a;
	private int _rotationCompensation = 0;
	private int _width, _height;
	private LinkedList<Integer> _averages = new LinkedList<Integer>();
	private int cptValidRect = 0, cptIndex = 0;
	
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
			_supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
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
			int rotation = getDisplayRotation(_a);
			_df.setRealSize(_width, _height);

			if (_df != null) {
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_UNDEFINED) {
					return;
				}

				_df.setRealSize(_width, _height);
				_rotationCompensation = MagnificationVideo.roundOrientation(rotation);
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					_df.setDisplayOrientation(0);
					_df.setRealSize(_height, _width);

				}

				int orientationCompensation = _rotationCompensation + rotation;
				if (_rotationCompensation != orientationCompensation) {
					_rotationCompensation = orientationCompensation;
					_df.setDisplayOrientation(_rotationCompensation);
				}

			}
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
			Matrix m = new Matrix();
			m.setRotate(180, 0, 0);
			m.mapRect(rect);
			
			int left = Math.abs((int)rect.left);
			int right = Math.abs((int)rect.right);
			int top = Math.abs((int)rect.top);
			int bottom = Math.abs((int)rect.bottom);
			
			if(left > right) {
				int temp = right;
				right = left;
				left = temp;
			}
			if(top > bottom) {
				int temp = bottom;
				bottom = top;
				top = temp;
			}
			
			// On lance dès qu'on obtient un rect valide
			if(left != 0) {
				if(!launchedTimer) {
					timer();	
					launchedTimer = true;
				}
			}
			
			// On calcule tant que le timer est encore en cours
			if(!_endOfTimer) {
				cptValidRect++;
				System.out.println("cptValidRect : "+cptValidRect);
				
				int avg = 0;
				int k = 0, l = 0, rsize = 0;
				for (int i = top; i < bottom; i++) {
					for (int j = left; j < right; j++) {
						avg += (int)Color.green(rgb[(k*(int)rect.width()+i)+(j+l)]);			
						l = l+1;
						rsize = rsize + 1;
					}
					k = k +1;
					l = 0;
				}
				
				System.out.println("l: "+left+", r: "+right+", t: "+top+" et b: "+bottom+"  --- rsize"+rsize);
				
				// Au début, on a des valeurs nulles donc on vérifie qu'on parcours bien qqch
				if(rsize != 0) {
					Integer iInt = Integer.valueOf(avg/rsize);
					_averages.add(iInt);
					cptIndex++;
				}
			}
		}
		
		mCamera.addCallbackBuffer(data);
	}
	
	public void timer() {
		new CountDownTimer(5000, 1000) {

			public void onTick(long millisUntilFinished) {
				// Toast.makeText(_context, "seconds remaining: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
			}

			public void onFinish() {
				_endOfTimer	= true;
				Toast.makeText(_context,"Ok, list size : "+_averages.size(),Toast.LENGTH_SHORT).show();
				derive(_averages);
				amplification(_averages, 3);
				
				for (int i=0; i< _averages.size(); i++) {
					System.out.print((Integer)_averages.get(i)+" ");
				}
				System.out.println();
			}
		}.start();
	}

	public void derive(LinkedList<Integer> avgs) {
		for (int i = 1; i < avgs.size(); i++) {
			avgs.set(i,(avgs.get(i) - avgs.get(i-1)));
		}
	}
	
	public void amplification(LinkedList<Integer> avgs, int factor) {
		int[] tab = new int[avgs.size()];
		for (int i = 0; i < avgs.size(); i++) {
			tab[i] = avgs.get(i)*factor;
		}
		for (int i = 0; i < avgs.size(); i++) {
			avgs.set(i, avgs.get(i) + tab[i]);
		}		
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (mCamera != null) {
				mCamera.setPreviewDisplay(holder);

				Camera.Parameters parameters = mCamera.getParameters();
				// changeOrientation();
				mCamera.setParameters(parameters);
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
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = resolveSize(getSuggestedMinimumWidth(),
				widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(),
				heightMeasureSpec);
		_width = width;
		_height = height;
		// Log.d("Size", "s" + _width +" " +_height);

		// setMeasuredDimension(width, height);

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

			mCamera.setDisplayOrientation(90);
			setMeasuredDimension(height, width);

			return;

		} else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int rotation = getDisplayRotation(_a);
			switch (rotation) {
			case 90:
				mCamera.setDisplayOrientation(0);
				break;
			case 270:
				mCamera.setDisplayOrientation(180);
				break;
			default:
				break;
			}
			setMeasuredDimension(width, height);

		}

		/*
		 * float ratio; if(mPreviewSize.height >= mPreviewSize.width) ratio =
		 * (float) mPreviewSize.height / (float) mPreviewSize.width; else ratio
		 * = (float) mPreviewSize.width / (float) mPreviewSize.height;
		 * 
		 * // One of these methods should be used, second method squishes
		 * preview slightly
		 * 
		 * Camera.Parameters parameters = mCamera.getParameters(); }
		 */
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