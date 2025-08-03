package com.example.opensource;

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
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraXFragment extends Fragment implements CameraListener {

    private PreviewView previewView;
    private Button captureButton;
    private ImageCapture imageCapture;

    private ExecutorService cameraExecutor;

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
        captureButton = view.findViewById(R.id.capture_button);

        cameraExecutor = Executors.newSingleThreadExecutor();

        requestPermissionsAndStartCamera();

        captureButton.setOnClickListener(v -> takePhoto());
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

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        getViewLifecycleOwner(),
                        cameraSelector,
                        preview,
                        imageCapture
                );

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // 갤러리용 저장 설정
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
                            Toast.makeText(getContext(), "✅ 저장됨", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onRetryCapture() {
        Toast.makeText(getContext(), "↩ 다시 찍기", Toast.LENGTH_SHORT).show();
        // CameraX는 자동으로 프리뷰 유지됨 → 추가 작업 불필요
    }

    @Override
    public void onConfirmCapture(Uri uri) {
        Toast.makeText(getContext(), "✔ 계속 진행됨: " + uri, Toast.LENGTH_SHORT).show();
        // TODO: 업로드, 다음 화면 이동 등
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}