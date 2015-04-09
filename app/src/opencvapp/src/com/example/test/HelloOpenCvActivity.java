package com.example.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

public class HelloOpenCvActivity extends Activity implements
		CvCameraViewListener2 {
	private CameraBridgeViewBase mOpenCvCameraView;
	private CascadeClassifier cascadeClassifier;
	private Mat grayscaleImage;
	private int absoluteFaceSize;
	private boolean running = false;
	private Point up;
	private Point down;
	private LinkedList<Double> _averages = new LinkedList<Double>();
	private LinkedList<Mat> images = new LinkedList<Mat>();
	private LinkedList<Point> upTab = new LinkedList<Point>();
	private LinkedList<Point> downTab = new LinkedList<Point>();
	private boolean isAnalyzing = false;
	private boolean hasTouched = false;
	private static boolean frontCamera = false;
	private double fps = 0;
	private int numberOfDetect = 0;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (hasTouched == false) {
			images.clear();
			upTab.clear();
			downTab.clear();
			_averages.clear();
			timer();
			hasTouched = true;
			isAnalyzing = false;
			System.err.println("Début !!!!!!!!!!!!!!");
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.helloopencvlayout);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCameraIndex(getCameraInstance());
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	synchronized public void analyse() {
		int index = 0;
		for (Mat image : images) {
			Point a = upTab.get(index);
			Point b = downTab.get(index);
			double green = 0;
			int size = 0;
			for (int k = (int) a.y; k < b.y; k++) {
				for (int l = (int) a.x; l < b.x; l++) {

					double[] t = image.get(k, l);
					green += t[1];
					size++;
				}
			}
			_averages.add(green / size);
			index++;
		}
	}

	public double max(double[] re) {
		int end = 150 * _averages.size() / ((int) fps * 60);
		int deb = 50 * _averages.size() / ((int) fps * 60);
		double max = 0;
		int pos = 0;
		for (int i = deb; i < end; i++) {
			if (max < re[i]) {
				max = re[i];
				pos = i;
			}
		}
		return pos;
	}

	public void timer() {
		new CountDownTimer(10000, 1000) {

			public void onTick(long millisUntilFinished) {
			}

			public void onFinish() {
				boolean isHuman = false;
				double trig = 0;
				isAnalyzing = true;
				if (images.size() != 0) {
					if (images.size() > 50) {
						isHuman = true;
						fps = images.size() / 10;
						System.err.println("FPS " + fps);
						analyse();
						System.err.println("OVER");
						System.err.println(_averages);
						derive(_averages);
						System.err.println(_averages);
						System.err.println(_averages.size());
						int v = variations(_averages) / 4;
						trig = v * fps * 60 / _averages.size();

					}
					System.err.println("Trigger " + trig);
					Toast toast = Toast.makeText(getApplicationContext(),
							" Trigger " + trig, Toast.LENGTH_LONG);

					toast.show();
					String str = "";
					if (isHuman) {
						if ((trig > 150 || trig < 50))
							isHuman = false;
						else
							isHuman = true;
					}
					if (isHuman)
						str = "Human";
					else
						str = "No Human";

					Toast result = Toast.makeText(getApplicationContext(), str,
							Toast.LENGTH_LONG);

					result.show();
					System.gc();
				}
				hasTouched = false;
			}
		}.start();
	}

	public static int getCameraInstance() {
		int c = 0;
		try {
			int nbCameras = Camera.getNumberOfCameras();
			if (nbCameras > 1) {
				c = 1;
				frontCamera = true;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return c;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
		grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

		// The faces will be a 20% of the height of the screen
		absoluteFaceSize = (int) (height * 0.2);
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(CvCameraViewFrame aInputFrame) {
		if (frontCamera) {
			Mat mRgba = aInputFrame.rgba();
			Mat mRgbaT = mRgba.t();
			Core.flip(mRgba.t(), mRgbaT, 1);
			Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());
		}
		Imgproc.cvtColor(aInputFrame.rgba(), grayscaleImage,
				Imgproc.COLOR_RGBA2RGB);

		MatOfRect faces = new MatOfRect();

		// Use the classifier to detect faces
		if (cascadeClassifier != null) {
			cascadeClassifier
					.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
							new Size(absoluteFaceSize, absoluteFaceSize),
							new Size());
		}

		// If there are any faces found, draw a rectangle around it
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++) {
			Core.rectangle(aInputFrame.gray(), facesArray[i].tl(),
					facesArray[i].br(), new Scalar(255, 255, 255, 255), 3);

		}

		if (isAnalyzing == false) {
			if (facesArray.length != 0) {
				running = true;
				up = facesArray[0].tl();
				down = facesArray[0].br();
			}

			if (running) {
				if (facesArray.length != 0) {
					System.err.println("yop");

					images.add(aInputFrame.rgba());
					upTab.add(up);
					downTab.add(down);
				}

			}
		}

		// Core.rectangle(aInputFrame.rgba(),new Point(10,10),new
		// Point(30,30),new Scalar(0,0,255));

		return aInputFrame.rgba();
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
		boolean up = false;
		boolean down = false;
		float bande = 0.01f;

		int i = 0;

		for (Double y : avgs) {
			if (y < bande && up == true) {
				i++;
			}
			if (y > bande && down == true) {
				i++;
			}
			if (y < bande) {
				down = true;
				up = false;
			}
			if (y > bande) {
				up = true;
				down = false;
			}

		}
		return i;
	}

	public void derive(LinkedList<Double> avgs) {
		double tmp = 0;
		for (int i = 1; i < avgs.size() - 1; i++) {
			tmp = avgs.get(i + 1);
			avgs.set(i, (avgs.get(i) - tmp));
		}
		if (avgs.size() != 0) {
			avgs.set(avgs.size() - 1, 0.0);
			avgs.set(0, 0.0);
		}
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {

				try {
					// Copy the resource into a temp file so OpenCV can load it
					InputStream is = getResources().openRawResource(
							R.raw.lbpcascade_frontalface);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					File mCascadeFile = new File(cascadeDir,
							"lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					// Load the cascade classifier
					cascadeClassifier = new CascadeClassifier(
							mCascadeFile.getAbsolutePath());
				} catch (Exception e) {
					Log.e("OpenCVActivity", "Error loading cascade", e);
				}

				// Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this,
				mLoaderCallback);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hello_open_cv, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
