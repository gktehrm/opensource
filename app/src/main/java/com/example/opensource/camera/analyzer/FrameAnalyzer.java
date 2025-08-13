package com.example.opensource.camera.analyzer;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.opensource.camera.processing.ContourDetector;
import com.example.opensource.camera.processing.LightingUtils;
import com.example.opensource.camera.util.OpenCvImageUtils;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;
import org.opencv.core.Core;

public class FrameAnalyzer implements ImageAnalysis.Analyzer {

    private static final boolean DEBUG = true;
    private long lastFpsTsNs = System.nanoTime();
    private int frameCount = 0;

    private final ContourResultListener listener;
    private final AtomicBoolean busy = new AtomicBoolean(false);

    public FrameAnalyzer(ContourResultListener listener) {
        this.listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (busy.getAndSet(true)) { imageProxy.close(); return; }
        try {
            // 1) YUV -> BGR(Mat), 회전 보정
            Mat bgr = OpenCvImageUtils.imageProxyToBgr(imageProxy);
            if (bgr == null) { listener.onNoContour(0,0); return; }

            // 2) Resize (max=1000)
            Mat resized = OpenCvImageUtils.resizeMax(bgr, 1000.0);
            int imgW = resized.cols(), imgH = resized.rows();

            // 3) 조명 보정 (무거우면 끄고 테스트)
            Mat lighting = LightingUtils.reduceLighting(resized, 10.0, 0.3, 1.5);

            // 4) 대비/노출 보정 (contrast=1.5, exposure=-40)
            Mat ce = new Mat();
            lighting.convertTo(ce, CvType.CV_8U, 1.50, -40);

            // 5) Canny → Close → Gradient
            Mat gray = new Mat();
            Imgproc.cvtColor(ce, gray, Imgproc.COLOR_BGR2GRAY);

            Mat edges = new Mat();
            Imgproc.Canny(gray, edges, 125, 150);

            Mat closed = new Mat();
            Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5)));

            Mat gradient = new Mat();
            Imgproc.morphologyEx(closed, gradient, Imgproc.MORPH_GRADIENT,
                    Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

            // 6) 문서 컨투어 검출 + 스무딩
            ContourDetector.Result pick = ContourDetector.findDocumentContour(resized, gradient);
            MatOfPoint sContour = (pick != null && pick.bestContour != null)
                    ? ContourDetector.smoothContour(pick.bestContour, new Size(imgW, imgH), new Size(100,100), 1, 100)
                    : null;

            if (sContour != null) {
                float[][] pts = OpenCvImageUtils.toFloatPoints(sContour);
                listener.onContourResult(pts[0], pts[1], imgW, imgH);
            } else {
                listener.onNoContour(imgW, imgH);
            }

            // release
            bgr.release(); resized.release(); lighting.release(); ce.release();
            gray.release(); edges.release(); closed.release(); gradient.release();
            if (pick != null) {
                if (pick.bestContour != null) pick.bestContour.release();
                if (pick.bestScreen != null) pick.bestScreen.release();
            }
            if (DEBUG) {
                int nzEdges = Core.countNonZero(edges);
                int nzGrad  = Core.countNonZero(gradient);
                Log.d("Analyzer", "edgesNZ=" + nzEdges + " gradNZ=" + nzGrad
                        + " contour=" + (sContour != null));
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
            listener.onNoContour(0,0);
        } finally {
            imageProxy.close();
            busy.set(false);
        }
    }
}
