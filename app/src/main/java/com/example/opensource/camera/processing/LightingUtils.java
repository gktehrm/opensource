package com.example.opensource.camera.processing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class LightingUtils {

    /** Homomorphic filter 기반 조명 보정 (Python 포팅) */
    public static Mat reduceLighting(Mat bgr, double sigma, double g1, double g2) {
        Mat yuv = new Mat();
        Imgproc.cvtColor(bgr, yuv, Imgproc.COLOR_BGR2YUV);
        List<Mat> ch = new ArrayList<>(3);
        Core.split(yuv, ch);
        Mat Y = ch.get(0);

        Y.convertTo(Y, CvType.CV_32F);
        Core.add(Y, Scalar.all(1.0), Y);
        Core.divide(Y, Scalar.all(255.0), Y);
        Core.log(Y, Y); // ln(1 + x/255)

        int rows = Y.rows(), cols = Y.cols();
        int M = 2 * rows + 1, N = 2 * cols + 1;

        Mat lpf = gaussianLPF(M, N, sigma);
        Mat hpf = new Mat();
        Core.subtract(Mat.ones(lpf.size(), lpf.type()), lpf, hpf);

        Mat Ypad = new Mat(new Size(N, M), CvType.CV_32F);
        Ypad.setTo(Scalar.all(0));
        Y.copyTo(Ypad.submat(0, rows, 0, cols));

        List<Mat> planes = Arrays.asList(Ypad, Mat.zeros(Ypad.size(), CvType.CV_32F));
        Mat complex = new Mat();
        Core.merge(planes, complex);
        Core.dft(complex, complex);

        Mat lpfShift = ifftShift(lpf);
        Mat hpfShift = ifftShift(hpf);

        Mat lf = mulAndIdft(complex, lpfShift);
        Mat hf = mulAndIdft(complex, hpfShift);

        Mat comb = new Mat();
        Core.addWeighted(lf.submat(0, rows, 0, cols), g1, hf.submat(0, rows, 0, cols), g2, 0.0, comb);

        Core.exp(comb, comb);
        Core.subtract(comb, Scalar.all(1.0), comb);
        Core.normalize(comb, comb, 0, 255, Core.NORM_MINMAX);
        comb.convertTo(comb, CvType.CV_8U);

        ch.set(0, comb);
        Core.merge(ch, yuv);
        Mat out = new Mat();
        Imgproc.cvtColor(yuv, out, Imgproc.COLOR_YUV2BGR);

        // release
        lpf.release(); hpf.release(); Ypad.release(); complex.release(); lf.release(); hf.release(); yuv.release();
        return out;
    }

    private static Mat gaussianLPF(int M, int N, double sigma) {
        Mat lpf = new Mat(M, N, CvType.CV_32F);
        float cx = (float) Math.ceil(N / 2.0), cy = (float) Math.ceil(M / 2.0);
        for (int y = 0; y < M; y++) {
            for (int x = 0; x < N; x++) {
                float dx = x - cx, dy = y - cy;
                float v = (float) Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
                lpf.put(y, x, v);
            }
        }
        return lpf;
    }

    private static Mat ifftShift(Mat m) {
        Mat out = m.clone();
        int cx = m.cols() / 2; int cy = m.rows() / 2;
        Mat q0 = new Mat(m, new Rect(0, 0, cx, cy));
        Mat q1 = new Mat(m, new Rect(cx, 0, m.cols() - cx, cy));
        Mat q2 = new Mat(m, new Rect(0, cy, cx, m.rows() - cy));
        Mat q3 = new Mat(m, new Rect(cx, cy, m.cols() - cx, m.rows() - cy));
        Mat tmp = new Mat();

        q0.copyTo(tmp); q3.copyTo(new Mat(out, new Rect(0,0,cx,cy))); tmp.copyTo(new Mat(out, new Rect(cx,cy,q3.cols(),q3.rows())));
        q1.copyTo(tmp); q2.copyTo(new Mat(out, new Rect(cx,0,q2.cols(),q2.rows()))); tmp.copyTo(new Mat(out, new Rect(0,cy,q1.cols(),q1.rows())));
        tmp.release();
        return out;
    }

    private static Mat mulAndIdft(Mat complex, Mat filter) {
        List<Mat> comps = new ArrayList<>(2);
        Core.split(complex, comps); // [Re, Im]
        Mat re = comps.get(0), im = comps.get(1);
        Core.multiply(re, filter, re);
        Core.multiply(im, filter, im);
        Core.merge(Arrays.asList(re, im), complex);

        Mat inv = new Mat();
        Core.idft(complex, inv, Core.DFT_SCALE | Core.DFT_INVERSE, 0);
        List<Mat> out = new ArrayList<>(2);
        Core.split(inv, out);
        return out.get(0);
    }
}
