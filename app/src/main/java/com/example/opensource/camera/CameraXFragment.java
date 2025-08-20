package com.example.opensource.camera;

import static com.example.opensource.camera.util.OpenCvImageUtils.getExifRotation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.opensource.R;
import com.example.opensource.camera.analyzer.ContourResultListener;
import com.example.opensource.camera.analyzer.FrameAnalyzer;
import com.example.opensource.camera.util.OpenCvImageUtils;
import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraXFragment extends Fragment implements CameraListener, ContourResultListener {

    private PreviewView previewView;
    private OverlayView overlay;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FrameAnalyzer frameAnalyzer;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis analysis;
    private boolean isStopped = false;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                if (cameraGranted != null && cameraGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        previewView = view.findViewById(R.id.previewView);
        overlay = view.findViewById(R.id.overlay);
        Button captureButton = view.findViewById(R.id.capture_button);

        cameraExecutor = Executors.newSingleThreadExecutor();
        frameAnalyzer = new FrameAnalyzer(this);

        requestPermissionsAndStartCamera();
        captureButton.setOnClickListener(v -> takePhoto());
        overlay.post(() -> overlay.debugDrawTestBox());
    }

    private void requestPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(requireActivity().getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetResolution(new android.util.Size(1280, 720))
                        .build();

                analysis.setAnalyzer(cameraExecutor, frameAnalyzer);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture,
                        analysis
                );

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed" + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
    }

    public void stopCamera() {
        if (isStopped) return;
        isStopped = true;
        try {
            if (analysis != null) {
                analysis.clearAnalyzer();
                analysis = null;
            }
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        } catch (Exception ignored) {}
        // 실행 스레드도 종료
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        imageCapture.takePicture(
                cameraExecutor,
                new ImageCapture.OnImageCapturedCallback() {
                    @ExperimentalGetImage
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bitmap;
                        try (image) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);

                            int exifRotation = getExifRotation(bytes);

                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                            if (exifRotation != 0 && bitmap != null) {
                                Matrix matrix = new Matrix();
                                matrix.postRotate(exifRotation);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            }

                            Mat original = new Mat();
                            Utils.bitmapToMat(bitmap, original);

                            // 화면 표시용 원본 비트맵(회전 적용된 상태)
                            Bitmap originalBitmapForDialog = bitmap;

                            float[] qx = frameAnalyzer.getLastQuadXs();
                            float[] qy = frameAnalyzer.getLastQuadYs();

                            Bitmap warpedBitmapForDialog = null;
                            try {
                                Mat warped = null;
                                if (qx != null && qy != null && qx.length == 4 && qy.length == 4) {
                                    warped = com.example.opensource.camera.processing.RectifyUtils.rectifyWithQuad(original, qx, qy);
                                }
                                if (warped != null && !warped.empty()) {
                                    warpedBitmapForDialog = Bitmap.createBitmap(warped.cols(), warped.rows(), Bitmap.Config.ARGB_8888);
                                    Utils.matToBitmap(warped, warpedBitmapForDialog);
                                    warped.release();
                                }
                            } finally {
                                original.release();
                            }

                            Bitmap finalOriginal = originalBitmapForDialog;
                            Bitmap finalWarped = warpedBitmapForDialog;

                            requireActivity().runOnUiThread(() -> {
                                PhotoPreviewDialogFragment dialog =
                                        PhotoPreviewDialogFragment.newInstance(finalOriginal, finalWarped);
                                dialog.setTargetFragment(CameraXFragment.this, 0);
                                dialog.show(getParentFragmentManager(), "PhotoPreview");
                            });

                        } catch (Exception e) {
                            Log.e("Capture", "사진 처리 중 오류" + e.getMessage());
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "사진 처리 중 오류", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
        );
    }

    @Override
    public void onRetryCapture() {
        Toast.makeText(getContext(), "다시 찍기", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfirmCapture(Bitmap bitmap) {
        CameraActivity activity = (CameraActivity) requireActivity();
        activity.onCameraResult(OpenCvImageUtils.bitmapToMat(bitmap));
    }


    @Override
    public void onContourResult(float[] xs, float[] ys, int imgW, int imgH) {
        if (overlay == null) return;
        int vw = overlay.getWidth(), vh = overlay.getHeight();
        if (vw == 0 || vh == 0) return;

        float[] mapped = OpenCvImageUtils.mapToOverlayFill(xs, ys, imgW, imgH, vw, vh);

        int n = mapped.length / 2;
        float[] mx = new float[n], my = new float[n];
        for (int i = 0, j = 0; i < mapped.length; i += 2, j++) {
            mx[j] = mapped[i];
            my[j] = mapped[i + 1];
        }
        requireActivity().runOnUiThread(() -> overlay.setMappedContour(mx, my));
    }

    @Override
    public void onNoContour(int imgW, int imgH) {
        if (overlay == null || !isAdded()) return;

        requireActivity().runOnUiThread(() -> overlay.setMappedContour(null, null));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCamera();
    }


}
