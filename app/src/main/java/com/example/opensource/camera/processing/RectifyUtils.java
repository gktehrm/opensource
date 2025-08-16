package com.example.opensource.camera.processing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class RectifyUtils {

    /** quad(4점: tl,tr,br,bl) 순서로 정렬 */
    // RectifyUtils.java
    private static Point[] orderQuad(Point[] ptsIn) {
        if (ptsIn == null || ptsIn.length < 4)
            throw new IllegalArgumentException("Need four points");

        // 1) 먼저 x로 정렬 → 좌측 2개, 우측 2개로 분리
        Point[] pts = ptsIn.clone();
        java.util.Arrays.sort(pts, (a,b)-> Double.compare(a.x, b.x));
        Point[] left  = new Point[]{ pts[0], pts[1] };
        Point[] right = new Point[]{ pts[2], pts[3] };

        // 2) 좌측 2점 중 y 작은 게 TL, 큰 게 BL
        Point tl = (left[0].y <= left[1].y) ? left[0] : left[1];
        Point bl = (left[0].y <= left[1].y) ? left[1] : left[0];

        // 3) 우측 2점 중 y 작은 게 TR, 큰 게 BR
        Point tr = (right[0].y <= right[1].y) ? right[0] : right[1];
        Point br = (right[0].y <= right[1].y) ? right[1] : right[0];

        return new Point[]{ tl, tr, br, bl }; // TL,TR,BR,BL
    }


    /** 대상 사각형 크기 계산 */
    private static Size rectSize(Point[] q) {
        Point tl = q[0], tr = q[1], br = q[2], bl = q[3];
        double w1 = Math.hypot(br.x - bl.x, br.y - bl.y);
        double w2 = Math.hypot(tr.x - tl.x, tr.y - tl.y);
        double h1 = Math.hypot(tr.x - br.x, tr.y - br.y);
        double h2 = Math.hypot(tl.x - bl.x, tl.y - bl.y);
        int W = Math.max(20, (int)Math.round(Math.max(w1, w2)));
        int H = Math.max(20, (int)Math.round(Math.max(h1, h2)));
        return new Size(W, H);
    }

    /**
     * 원근 보정: original(BGR)과 quad(원본버퍼 기준 4점), quad가 추정된 기준 크기(quadW,H) → warped 반환
     * - 만약 quadW/H가 원본 이미지와 다르면 quad 좌표를 원본 크기로 리스케일 후 사용
     */
    public static Mat rectifyWithQuad(Mat originalBgr, float[] quadXs, float[] quadYs) {
        // 4개 점 확인
        if (quadXs == null || quadYs == null || quadXs.length != 4 || quadYs.length != 4) {
            return originalBgr.clone(); // 안전하게 원본 복제
        }

        Point[] raw = new Point[]{
                new Point(quadXs[0], quadYs[0]),
                new Point(quadXs[1], quadYs[1]),
                new Point(quadXs[2], quadYs[2]),
                new Point(quadXs[3], quadYs[3])
        };

        Point[] quad = orderQuad(raw); // ← 여기서 강제 정렬

        // 목표 사각형 크기
        Size target = rectSize(quad);
        MatOfPoint2f src = new MatOfPoint2f(quad);
        MatOfPoint2f dst = new MatOfPoint2f(
                new Point(0,0),
                new Point(target.width-1, 0),
                new Point(target.width-1, target.height-1),
                new Point(0, target.height-1)
        );

        // Homography & warp
        Mat H = Imgproc.getPerspectiveTransform(src, dst);
        Mat warped = new Mat((int)target.height, (int)target.width, originalBgr.type());
        Imgproc.warpPerspective(originalBgr, warped, H, warped.size(), Imgproc.INTER_LINEAR);

        H.release(); src.release(); dst.release();
        return warped;
    }
}
