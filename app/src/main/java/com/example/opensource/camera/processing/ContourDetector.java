package com.example.opensource.camera.processing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class ContourDetector {

    public static class Result {
        public MatOfPoint bestContour; // big/smoothed candidate
        public MatOfPoint bestScreen;  // approx 4pts
    }

    /** Python find_document_contour 포팅(간략화) */
    public static Result findDocumentContour(Mat original, Mat edged) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edged.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        int h = original.rows(), w = original.cols();
        double areaThresh = Math.pow(Math.min(h, w) * 0.1, 2);

        // 작은 컨투어 마스크
        Mat smallMask = Mat.zeros(edged.size(), CvType.CV_8U);
        for (MatOfPoint c : contours) {
            if (Imgproc.contourArea(c) < areaThresh) {
                Imgproc.drawContours(smallMask, Arrays.asList(c), -1, new Scalar(255), -1);
            }
        }

        contours.sort((a, b) -> Double.compare(Imgproc.contourArea(b), Imgproc.contourArea(a)));
        if (contours.size() > 8) contours = contours.subList(0, 8);

        MatOfPoint bestContour = null;
        MatOfPoint bestScreen = null;
        double bestArea = -1;
        double bestSolidity = -1;

        for (MatOfPoint c : contours) {
            Mat docMask = Mat.zeros(edged.size(), CvType.CV_8U);
            Imgproc.drawContours(docMask, Arrays.asList(c), -1, new Scalar(255), -1);
            Mat docClean = new Mat();
            Core.subtract(docMask, smallMask, docClean);

            List<MatOfPoint> clean = new ArrayList<>();
            Imgproc.findContours(docClean, clean, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            if (clean.isEmpty()) continue;

            MatOfPoint smoothed = Collections.max(clean, Comparator.comparingDouble(Imgproc::contourArea));

            double area = Imgproc.contourArea(smoothed);

            // convex hull: 인덱스 → 점 변환
            MatOfInt hullIdx = new MatOfInt();
            Imgproc.convexHull(smoothed, hullIdx);
            Point[] pts = smoothed.toArray();
            int[] idx = hullIdx.toArray();
            Point[] hullPts = new Point[idx.length];
            for (int i = 0; i < idx.length; i++) hullPts[i] = pts[idx[i]];
            MatOfPoint hull = new MatOfPoint(hullPts);

            double hullArea = Imgproc.contourArea(hull);
            if (hullArea == 0) continue;
            double solidity = area / hullArea;

            MatOfPoint2f hull2f = new MatOfPoint2f(hull.toArray());
            MatOfPoint2f approx2f = new MatOfPoint2f();
            double peri = Imgproc.arcLength(hull2f, true);
            Imgproc.approxPolyDP(hull2f, approx2f, 0.03 * peri, true);
            MatOfPoint approx = new MatOfPoint(approx2f.toArray());

            if (approx.total() == 4 && solidity >= 0.9) {
                if (bestContour == null || (area >= bestArea * 0.95 && solidity > bestSolidity)) {
                    bestContour = smoothed;
                    bestScreen = approx;
                    bestArea = area;
                    bestSolidity = solidity;
                }
            }
        }

        if (bestScreen == null) return null;
        Result r = new Result();
        r.bestContour = bestContour;
        r.bestScreen = bestScreen;
        return r;
    }

    /** smooth_contour: 마스크화 → Morph Close → 최대 컨투어 선택 */
    public static MatOfPoint smoothContour(MatOfPoint contour, Size imageSize, Size kernel, int iterations, int pad) {
        int W = (int) imageSize.width, H = (int) imageSize.height;
        Mat mask = Mat.zeros(H + 2 * pad, W + 2 * pad, CvType.CV_8U);

        MatOfPoint padded = addOffset(contour, pad, pad);
        Imgproc.drawContours(mask, Arrays.asList(padded), -1, new Scalar(255), -1);

        Mat k = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernel);
        Mat closed = new Mat();
        Imgproc.morphologyEx(mask, closed, Imgproc.MORPH_CLOSE, k, new Point(-1, -1), iterations);

        List<MatOfPoint> newContours = new ArrayList<>();
        Imgproc.findContours(closed, newContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (newContours.isEmpty()) return contour;

        MatOfPoint best = Collections.max(newContours, Comparator.comparingDouble(Imgproc::contourArea));
        MatOfPoint bestShifted = addOffset(best, -pad, -pad);

        // clip
        Point[] pts = bestShifted.toArray();
        for (Point p : pts) {
            p.x = Math.max(0, Math.min(W - 1, p.x));
            p.y = Math.max(0, Math.min(H - 1, p.y));
        }
        return new MatOfPoint(pts);
    }

    private static MatOfPoint addOffset(MatOfPoint cnt, int dx, int dy) {
        Point[] pts = cnt.toArray();
        for (Point p : pts) { p.x += dx; p.y += dy; }
        return new MatOfPoint(pts);
    }
}
