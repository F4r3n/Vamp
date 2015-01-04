package com.example.magnificationvideo.mv;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DisplayedFace extends View {
    private Paint paint = new Paint();
    private Context ctx;
    List<Camera.Face> faces = new ArrayList<Camera.Face>();
    Matrix matrix = new Matrix();
    RectF rect = new RectF();
    private int mDisplayOrientation;
    private int mOrientation;
    private static final String TAG = "facedetection";
    private Paint textPaint = new Paint();

    
    public DisplayedFace(Context context) {
        super(context);
        this.ctx = context;
        initialize();
    }

    private void initialize() {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.GREEN);
        paint.setAlpha(128);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawARGB(0, 0, 0, 0);

        if(faces.size()>0) {

            prepareMatrix(matrix, 0, getWidth(), getHeight());
            canvas.save();
            matrix.postRotate(90);
            int score=0;

            canvas.rotate(-90);

            for (Face face : faces) {
                rect.set(face.rect);
                matrix.mapRect(rect);
                canvas.drawRect(rect, paint);
                score =face.score;

            }
            canvas.restore();
            canvas.drawText("score " + score, 30,30, paint);
        }

    }

    public RectF getRect(){
        return rect;
    }

    public void setDisplayOrientation(int orientation) {
        mDisplayOrientation = orientation;
    }

    public DisplayedFace(Context context, AttributeSet attr) {
        super(context, attr);
        this.ctx = context;
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        textPaint.setAntiAlias(true);
        textPaint.setDither(true);
        textPaint.setTextSize(20);
        textPaint.setColor(Color.GREEN);
        textPaint.setStyle(Paint.Style.FILL);
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
        invalidate();
    }


    public void setFaces(List<Camera.Face> faces) {
        this.faces = faces;
        invalidate();
    }

    public static void prepareMatrix(Matrix matrix, int displayOrientation,
                                     int viewWidth, int viewHeight) {

        boolean mirror = (1 == Camera.CameraInfo.CAMERA_FACING_FRONT);
        matrix.setScale(mirror ? -1 : 1, 1);


        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

}