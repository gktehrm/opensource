package com.example.opensource.camera.util;

import android.graphics.Bitmap;
import android.media.Image;

import androidx.camera.core.ImageProxy;
import androidx.exifinterface.media.ExifInterface;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenCvImageUtils {

    /**
     * PreviewView.ScaleType == FILL_CENTER(센터-크롭) 기준의 좌표 매핑.
     * 분석 이미지 좌표(원본 버퍼 기준)를 Overlay View 좌표로 변환한다.
     *
     * @param xs    분석 좌표들의 x 배열 (이미 "원본 버퍼 크기" 기준이어야 함)
     * @param ys    분석 좌표들의 y 배열 (이미 "원본 버퍼 크기" 기준이어야 함)
     * @param imgW  원본 버퍼 너비 (bgr.cols())
     * @param imgH  원본 버퍼 높이 (bgr.rows())
     * @param viewW 오버레이 뷰 너비 (overlay.getWidth())
     * @param viewH 오버레이 뷰 높이 (overlay.getHeight())
     * @return      [x0, y0, x1, y1, ..., xN, yN] 의 플랫 배열
     */
    public static float[] mapToOverlayFill(float[] xs, float[] ys,
                                           int imgW, int imgH,
                                           int viewW, int viewH) {
        if (xs == null || ys == null) return new float[0];
        if (xs.length != ys.length || xs.length == 0) return new float[0];
        if (imgW <= 0 || imgH <= 0 || viewW <= 0 || viewH <= 0) return new float[0];

        // FILL_CENTER = Center-crop: 축 확대비는 더 큰 쪽(=max), 남는 축은 잘려나감
        float scale = Math.max(viewW / (float) imgW, viewH / (float) imgH);
        float dx = (viewW - imgW * scale) * 0.5f; // 보통 0 또는 음수(크롭)
        float dy = (viewH - imgH * scale) * 0.5f;

        int n = xs.length;
        float[] out = new float[n * 2];
        for (int i = 0, j = 0; i < n; i++) {
            out[j++] = xs[i] * scale + dx;
            out[j++] = ys[i] * scale + dy;
        }
        return out;
    }

    /** ImageProxy → BGR(Mat), 회전보정 포함 */
    @androidx.camera.core.ExperimentalGetImage
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

    public static Mat bitmapToMat(Bitmap bmp) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bmp, mat);
        return mat;
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

    public static int getExifRotation(byte[] bytes) throws IOException {
        int exifRotation = 0;
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    exifRotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    exifRotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    exifRotation = 270;
                    break;
            }
        }
        return exifRotation;
    }
}
