package com.example.opensource.camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import org.opencv.core.Mat;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraXFragment extends Fragment implements CameraListener, ContourResultListener {

    private PreviewView previewView;
    private OverlayView overlay;
    private Button captureButton;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FrameAnalyzer frameAnalyzer;

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
        captureButton = view.findViewById(R.id.capture_button);

        cameraExecutor = Executors.newSingleThreadExecutor();
        frameAnalyzer = new FrameAnalyzer(this); // 분석 결과 콜백: 이 Fragment

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
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(requireActivity().getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetResolution(new android.util.Size(1280, 720)) // android.util.Size(완전수식)
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
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String name = "photo_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/InsectApp");

        Uri savedUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                requireContext().getContentResolver(),
                savedUri,
                contentValues
        ).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri uri = outputFileResults.getSavedUri();
                        if (uri != null) {
                            // 1) 최근 quad 가져오기
                            float[] qx = frameAnalyzer.getLastQuadXs();
                            float[] qy = frameAnalyzer.getLastQuadYs();

                            if (qx != null && qy != null && qx.length == 4 && qy.length == 4) {
                                // 2) Uri → Mat(BGR)
                                Mat original = com.example.opensource.camera.util.OpenCvImageUtils.uriToBgr(requireContext(), uri);
                                if (original != null && !original.empty()) {
                                    // 3) 원근 보정
                                    Mat warped = com.example.opensource.camera.processing.RectifyUtils
                                            .rectifyWithQuad(original, qx, qy);

                                    // 4) 같은 Uri에 덮어쓰기 (JPEG 95)
                                    boolean ok = com.example.opensource.camera.util.OpenCvImageUtils
                                            .saveMatToUri(requireContext(), uri, warped, 95);

                                    original.release(); warped.release();
                                    if (!ok) {
                                        Toast.makeText(getContext(), "원근변환 저장 실패(원본 표시)", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "이미지 로드 실패(원본 표시)", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "문서 외곽 4점을 찾지 못해 원본을 표시합니다", Toast.LENGTH_SHORT).show();
                            }

                            // 5) 다이얼로그 표시 (uri는 보정본 또는 원본)
                            Toast.makeText(getContext(), "✅ 저장됨(원근 보정 적용)", Toast.LENGTH_SHORT).show();
                            PhotoDialogFragment dialog = PhotoDialogFragment.newInstance(uri.toString());
                            dialog.show(getParentFragmentManager(), "PhotoDialog");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(getContext(), "❌ 저장 실패", Toast.LENGTH_SHORT).show();
                        exception.printStackTrace();
                    }
                }
        );
    }

    // ====== CameraListener ======
    @Override
    public void onRetryCapture() {
        Toast.makeText(getContext(), "↩ 다시 찍기", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfirmCapture(Uri uri) {
        Toast.makeText(getContext(), "✔ 계속 진행됨: " + uri, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onContourResult(float[] xs, float[] ys, int imgW, int imgH) {
        if (overlay == null) return;
        int vw = overlay.getWidth(), vh = overlay.getHeight();
        if (vw == 0 || vh == 0) return;

        // imgW/imgH는 이제 "원본 버퍼" 크기 (위 1번에서 보정)
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
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }


}
