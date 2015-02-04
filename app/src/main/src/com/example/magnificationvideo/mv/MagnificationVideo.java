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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.g);

        mCamera = getCameraInstance();
        mPreview = (CameraPreview) findViewById(R.id.surface_view);
        df = (DisplayedFace)findViewById(R.id.viewfinder_view);

        mPreview.setCamera(mCamera);
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
    
}