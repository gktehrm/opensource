package com.example.opensource.camera.util;

import android.media.Image;

import androidx.camera.core.ImageProxy;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class OpenCvImageUtils {

    /** ImageProxy → BGR(Mat), 회전보정 포함 */
    public static Mat imageProxyToBgr(ImageProxy imageProxy) {
        Image y = imageProxy.getImage();
        if (y == null) return null;

        byte[] nv21 = yuv420888ToNv21(y);
        Mat yuv = new Mat(imageProxy.getHeight() + imageProxy.getHeight() / 2, imageProxy.getWidth(), CvType.CV_8UC1);
        yuv.put(0, 0, nv21);
        Mat bgr = new Mat();
        Imgproc.cvtColor(yuv, bgr, Imgproc.COLOR_YUV2BGR_NV21);
        yuv.release();

        int rot = imageProxy.getImageInfo().getRotationDegrees();
        if (rot != 0) {
            bgr = rotateMat(bgr, rot);
        }
        return bgr;
    }

    /** 회전 보정 */
    public static Mat rotateMat(Mat src, int degrees) {
        Mat dst = new Mat();
        switch (degrees) {
            case 90:  Core.rotate(src, dst, Core.ROTATE_90_CLOCKWISE); break;
            case 180: Core.rotate(src, dst, Core.ROTATE_180); break;
            case 270: Core.rotate(src, dst, Core.ROTATE_90_COUNTERCLOCKWISE); break;
            default:  return src;
        }
        src.release();
        return dst;
    }

    /** YUV_420_888 → NV21 변환 (row/pixel stride 고려 X: 대부분 기기에서 OK) */
    public static byte[] yuv420888ToNv21(Image image) {
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        int ySize = yPlane.getBuffer().remaining();
        int uSize = uPlane.getBuffer().remaining();
        int vSize = vPlane.getBuffer().remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yPlane.getBuffer().get(nv21, 0, ySize);

        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];
        uPlane.getBuffer().get(uBytes, 0, uSize);
        vPlane.getBuffer().get(vBytes, 0, vSize);

        // NV21: VU VU VU...
        for (int i = 0; i < vSize; i++) {
            nv21[ySize + i * 2] = vBytes[i];
            nv21[ySize + i * 2 + 1] = uBytes[i];
        }
        return nv21;
    }

    /** 최대 변 길이 기준 리사이즈 */
    public static Mat resizeMax(Mat src, double maxSize) {
        int h = src.rows(), w = src.cols();
        int max = Math.max(h, w);
        if (max <= maxSize) return src.clone();
        double r = maxSize / max;
        Mat dst = new Mat();
        Imgproc.resize(src, dst, new Size(Math.round(w * r), Math.round(h * r)));
        return dst;
    }

    /** MatOfPoint → float[][] */
    public static float[][] toFloatPoints(MatOfPoint cnt) {
        Point[] pts = cnt.toArray();
        float[] xs = new float[pts.length];
        float[] ys = new float[pts.length];
        for (int i = 0; i < pts.length; i++) { xs[i] = (float) pts[i].x; ys[i] = (float) pts[i].y; }
        return new float[][]{xs, ys};
    }

    /** 분석 이미지 좌표 → 오버레이 뷰 좌표 (letterbox 보정) */
    public static float[] mapToOverlay(float[] xs, float[] ys, int imgW, int imgH, int viewW, int viewH) {
        int n = xs.length;
        float[] out = new float[n * 2];
        float fImgW = imgW, fImgH = imgH;
        float scale = Math.min(viewW / fImgW, viewH / fImgH);
        float dx = (viewW - fImgW * scale) / 2f;
        float dy = (viewH - fImgH * scale) / 2f;
        for (int i = 0, j = 0; i < n; i++) {
            out[j++] = xs[i] * scale + dx;
            out[j++] = ys[i] * scale + dy;
        }
        return out;
    }
}
