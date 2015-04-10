package com.example.magnificationvideo.mv;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.util.AttributeSet;
import android.view.View;

/**
 * La classe DisplayedFace permet de réaliser un rectangle autour du visage de la personne pour ce faire on va utiliser un canvas et la fonction onDraw.
 * @author Vimont Ludovic
 * @author Kotulski Guillaume
 */
public class DisplayedFace extends View {
	private Paint paint = new Paint();
	private Context ctx;
	List<Camera.Face> faces = new ArrayList<Camera.Face>();
	Matrix matrix = new Matrix();
	RectF rect = new RectF();
	RectF rectTransformed;
	private int mDisplayOrientation;
	private int mOrientation;
	private static final String TAG = "facedetection";
	private int _x, _y;

	public DisplayedFace(Context context, AttributeSet attrs) {
		super(context, attrs);
		ctx = context;
		initialize();
	}

	private void initialize() {
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(2f);
		paint.setStyle(Paint.Style.STROKE);
		paint.setAntiAlias(true);
		rectTransformed = new RectF();
	}

	public void setRealSize(int x, int y) {
		_x = x;
		_y = y;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawARGB(0, 0, 0, 0);
		if (faces.size() > 0) {
			int score = 0;
			prepareMatrix(matrix, 0, getWidth(), getHeight());
			canvas.save();
			matrix.postRotate(mDisplayOrientation);

			canvas.rotate(-mDisplayOrientation);

			for (Face face : faces) {
				rect.set(face.rect);
				matrix.mapRect(rect);

				rectTransformed.set(rect);
				canvas.drawRect(rect, paint);
				score = face.score;
			}
			canvas.restore();
			canvas.drawText("score " + score, 30, 30, paint);
		}
	}

	public RectF getRect() {
		return rectTransformed;
	}

	public void setDisplayOrientation(int orientation) {
		mDisplayOrientation = orientation;
	}

	public void setOrientation(int orientation) {
		mOrientation = orientation;
		invalidate();
	}

	public void setFaces(List<Camera.Face> faces) {
		this.faces = faces;
		invalidate();
	}

	public static void prepareMatrix(Matrix matrix, int displayOrientation, int viewWidth, int viewHeight) {
		boolean mirror = (1 == Camera.CameraInfo.CAMERA_FACING_FRONT);
		matrix.setScale(mirror ? -1 : -1, 1);
		matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
		matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
	}

}
