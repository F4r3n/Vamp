package com.example.magnificationvideo.mv;

import java.io.File;
import java.io.FileWriter;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Environment;


public class MyFaceDetectionListener implements FaceDetectionListener {

    private Context ctx;
    private Rect rect;

    public MyFaceDetectionListener(Context ctx) {
        this.ctx = ctx;
    }

    public void onFaceDetection(Face[] faces, Camera camera) {
        if(faces.length == 0) { return;}


        int left = faces[0].rect.left;
        int right = faces[0].rect.right;
        int top = faces[0].rect.top;
        int bottom = faces[0].rect.bottom;
        Rect uRect = new Rect(left, top, right, bottom);
        rect = uRect;
    }

    public Rect getRect() {
        return rect;
    }
}