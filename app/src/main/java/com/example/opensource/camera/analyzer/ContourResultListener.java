package com.example.opensource.camera.analyzer;

public interface ContourResultListener {
    void onContourResult(float[] xs, float[] ys, int imgW, int imgH);
    void onNoContour(int imgW, int imgH);
}
