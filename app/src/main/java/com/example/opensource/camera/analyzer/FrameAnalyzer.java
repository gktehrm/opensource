package com.example.opensource.camera.analyzer;

import androidx.annotation.NonNull;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.opensource.camera.processing.ContourDetector;
import com.example.opensource.camera.util.OpenCvImageUtils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;

public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final boolean DEBUG = true;
    private long lastFpsTsNs = System.nanoTime();
    private int frameCount = 0;

    private final ContourResultListener listener;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private float[] lastQuadXs, lastQuadYs; // bestScreen(4점)
    private int lastSrcW, lastSrcH;

    public FrameAnalyzer(ContourResultListener listener) {
        this.listener = listener;
    }

    @Override
    @ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (busy.getAndSet(true)) { imageProxy.close(); return; }
        try (imageProxy) {
            // 1) YUV -> BGR(Mat), 회전 보정
            Mat bgr = OpenCvImageUtils.imageProxyToBgr(imageProxy);
            if (bgr == null) {
                listener.onNoContour(0, 0);
                return;
            }

            // 2) Resize (max=1000)
            Mat resized = OpenCvImageUtils.resizeMax(bgr, 1000.0);
            int imgW = resized.cols(), imgH = resized.rows();

            // 4) 대비/노출 보정 (contrast=1.5, exposure=-40)
            Mat ce = new Mat();
            resized.convertTo(ce, CvType.CV_8U, 1.50, -40);

            // 5) Canny → Close → Gradient
            Mat edges = new Mat();
            Imgproc.Canny(ce, edges, 125, 150);

            Mat closed = new Mat();
            Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

            Mat gradient = new Mat();
            Imgproc.morphologyEx(closed, gradient, Imgproc.MORPH_GRADIENT,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

            // 6) 문서 컨투어 검출 + 스무딩
            ContourDetector.Result pick = ContourDetector.findDocumentContour(resized, gradient);
            MatOfPoint sContour = (pick != null && pick.bestContour != null)
                    ? ContourDetector.smoothContour(pick.bestContour, new Size(imgW, imgH), new Size(100, 100), 1, 100)
                    : null;

            int srcW = bgr.cols(), srcH = bgr.rows();
            int rzW = resized.cols(), rzH = resized.rows();
            float sx = (float) srcW / rzW;
            float sy = (float) srcH / rzH;

            if (sContour != null) {
                float[][] pts = OpenCvImageUtils.toFloatPoints(sContour);

                for (int i = 0; i < pts[0].length; i++) {
                    pts[0][i] *= sx;
                    pts[1][i] *= sy;
                }

                // 원본 버퍼 크기를 넘겨야 이후 매핑이 정확
                listener.onContourResult(pts[0], pts[1], srcW, srcH);
            } else {
                listener.onNoContour(srcW, srcH);
            }

            // release
            bgr.release();
            resized.release();
            ce.release();
            edges.release();
            closed.release();
            gradient.release();
            if (pick != null && pick.bestScreen != null) {
                org.opencv.core.Point[] p = pick.bestScreen.toArray();
                float[] qx = new float[p.length];
                float[] qy = new float[p.length];
                for (int i = 0; i < p.length; i++) {
                    qx[i] = (float) (p[i].x * sx);
                    qy[i] = (float) (p[i].y * sy);
                }

                // 4점만 쓰도록 보정 (approxPolyDP 결과는 4점이 맞아야 함)
                if (qx.length >= 4) {
                    lastQuadXs = new float[]{qx[0], qx[1], qx[2], qx[3]};
                    lastQuadYs = new float[]{qy[0], qy[1], qy[2], qy[3]};
                    lastSrcW = srcW;
                    lastSrcH = srcH;
                }
            }
            if (pick != null) {
                if (pick.bestContour != null) pick.bestContour.release();
                if (pick.bestScreen != null) pick.bestScreen.release();
            }

            // ===== 디버그: FPS 출력 =====
            if (DEBUG) {
                frameCount++;
                long now = System.nanoTime();
                if (now - lastFpsTsNs > 1_000_000_000L) {
                    Log.d("Analyzer", "FPS=" + frameCount);
                    frameCount = 0;
                    lastFpsTsNs = now;
                }
            }
        } catch (Throwable t) {
            Log.e("Analyzer", "analyze error", t);
            listener.onNoContour(0, 0);
        } finally {
            busy.set(false);
        }
    }

    public synchronized float[] getLastQuadXs() { return lastQuadXs; }
    public synchronized float[] getLastQuadYs() { return lastQuadYs; }
    public synchronized int getLastSrcW() { return lastSrcW; }
    public synchronized int getLastSrcH() { return lastSrcH; }
}
