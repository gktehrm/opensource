package com.example.opensource.camera.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.util.Objects;

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

    /**
     * (선택) FIT_CENTER(레터박스)용 매핑.
     * PreviewView.ScaleType이 FIT_CENTER라면 이걸 사용.
     */
    public static float[] mapToOverlayLetterbox(float[] xs, float[] ys,
                                                int imgW, int imgH,
                                                int viewW, int viewH) {
        if (xs == null || ys == null) return new float[0];
        if (xs.length != ys.length || xs.length == 0) return new float[0];
        if (imgW <= 0 || imgH <= 0 || viewW <= 0 || viewH <= 0) return new float[0];

        float scale = Math.min(viewW / (float) imgW, viewH / (float) imgH);
        float dx = (viewW - imgW * scale) * 0.5f; // 레터박스 여백(+)
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
    /** Uri → BGR(Mat) */
    public static Mat uriToBgr(Context ctx, Uri uri) {
        try {
            ContentResolver cr = ctx.getContentResolver();

            // 1) Bitmap 로드
            Bitmap bmp = BitmapFactory.decodeStream(cr.openInputStream(uri));
            if (bmp == null) return null;

            // 2) EXIF Orientation 읽기
            int exifDeg = 0;
            try (InputStream is = cr.openInputStream(uri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    exifDeg = switch (ori) {
                        case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
                        case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
                        case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
                        default -> exifDeg;
                    };
                }
            } catch (Exception ignored) {}

            // 3) Bitmap → Mat(RGBA)
            Mat rgba = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(bmp, rgba);

            // 4) EXIF 각도만큼 픽셀 회전 적용하여 "upright" 만들기
            if (exifDeg != 0) {
                Mat rotated = new Mat();
                switch (exifDeg) {
                    case 90:  Core.rotate(rgba, rotated, Core.ROTATE_90_CLOCKWISE); break;
                    case 180: Core.rotate(rgba, rotated, Core.ROTATE_180); break;
                    case 270: Core.rotate(rgba, rotated, Core.ROTATE_90_COUNTERCLOCKWISE); break;
                }
                rgba.release();
                rgba = rotated;
            }

            // 5) RGBA → BGR
            Mat bgr = new Mat();
            Imgproc.cvtColor(rgba, bgr, Imgproc.COLOR_RGBA2BGR);
            rgba.release();
            return bgr;

        } catch (Exception e) {
            Log.e("OpenCvImageUtils", Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }

    /** BGR(Mat) → 같은 Uri로 JPEG 덮어쓰기 */
    public static boolean saveMatToUri(Context ctx, Uri uri, Mat bgr, int jpegQuality /*0..100*/) {
        try {
            // BGR → RGBA → Bitmap → JPEG
            Mat rgba = new Mat();
            Imgproc.cvtColor(bgr, rgba, Imgproc.COLOR_BGR2RGBA);
            Bitmap bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgba, bmp);
            rgba.release();

            ContentResolver cr = ctx.getContentResolver();
            try (java.io.OutputStream os = cr.openOutputStream(uri, "rwt")) {
                if (os == null) return false;
                boolean ok = bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, os);
                os.flush();
                bmp.recycle();
                return ok;
            }
        } catch (Exception e) {
            Log.e("OpenCvImageUtils", Objects.requireNonNull(e.getMessage()));
            return false;
        }
    }
}
