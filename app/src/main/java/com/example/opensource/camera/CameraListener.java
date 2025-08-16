package com.example.opensource.camera;

import android.graphics.Bitmap;

public interface CameraListener {
    void onRetryCapture();
    void onConfirmCapture(Bitmap bitmap); // Uri 대신 Bitmap으로!
}