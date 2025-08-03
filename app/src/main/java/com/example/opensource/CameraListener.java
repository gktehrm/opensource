package com.example.opensource;

import android.net.Uri;

public interface CameraListener {
    void onRetryCapture();
    void onConfirmCapture(Uri uri);
}
