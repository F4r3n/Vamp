package com.example.magnificationvideo.mv;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;


public class MagnificationVideo extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private DisplayedFace df;
//    private MyOrientationEventListener mOrientationListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.g);

        mCamera = getCameraInstance();
     //   mCamera.setDisplayOrientation(90);

        mPreview = (CameraPreview) findViewById(R.id.surface_view);
        df = (DisplayedFace)findViewById(R.id.viewfinder_view);

        mPreview.setCamera(mCamera);
//        mOrientationListener = new MyOrientationEventListener(this);
//        mOrientationListener.enable();
    }

    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            int nbCameras = Camera.getNumberOfCameras();
            if (nbCameras > 1) {
                c = Camera.open(CameraInfo.CAMERA_FACING_FRONT);
            } else {
                c = Camera.open(0);

            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return c;
    }

    public static int roundOrientation(int orientation) {
        return ((orientation + 45) / 90 * 90) % 360;
    }
    

    
//    private class MyOrientationEventListener extends OrientationEventListener {
//
//        public MyOrientationEventListener(Context context) {
//            super(context);
//        }
//
//        @Override
//        public void onOrientationChanged(int orientation) {
//            // We keep the last known orientation. So if the user first orient the camera then point the camera to floor or sky, we still have the correct orientation.
//            if (orientation == ORIENTATION_UNKNOWN) {
//                return;	
//            }
//            mOrientation = roundOrientation(orientation);
//            
//            // When the screen is unlocked, display rotation may change. Always calculate the up-to-date orientationCompensation.
//            int orientationCompensation = mOrientation+ MagnificationVideo.getDisplayRotation(MagnificationVideo.this);
//            System.out.println(orientationCompensation);
//            
//            if (mOrientationCompensation != orientationCompensation) {
//                mOrientationCompensation = orientationCompensation;
//                setOrientationIndicator(mOrientationCompensation);
//            }
//        }
//    }
//    
//    private void setOrientationIndicator(int degree) {
//       if (df != null) {
//    	   df.setOrientation(degree);
//       }
//    }
    
}