package com.example.opensource.camera;

import android.net.Uri;

public interface CameraListener {
    void onRetryCapture();
    void onConfirmCapture(Uri uri);
}
