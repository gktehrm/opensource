package com.example.opensource.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.opensource.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class CameraActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java4");
        Log.d("OpenCV", "OpenCV native lib loaded");
    }

    public static final String EXTRA_ANALYSIS_JSON = "analysisJson";
    public static final String EXTRA_IMAGE_URI = "imageUri";

    private View processingLayout;

    public static final String TAG_CAMERA = "TAG_CAMERA";
    private boolean isProcessing = false;   // 중복 호출 방지
    private Bitmap lastCapturedBitmap;      // Uri 저장용

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    // 사용자가 취소한 경우: 그냥 종료하거나 다시 선택 UI를 보여도 됨
                    setResult(RESULT_CANCELED);
                    finish();
                    return;
                }
                if (isProcessing) return;
                isProcessing = true;

                // 카메라 UI 숨기고 처리 UI 표시
                View cameraContainer = findViewById(R.id.camera_fragment_container);
                if (cameraContainer != null) cameraContainer.setVisibility(View.GONE);
                processingLayout.setVisibility(View.VISIBLE);

                try {
                    Bitmap bitmap;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                        bitmap = ImageDecoder.decodeBitmap(source);
                    } else {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    }

                    lastCapturedBitmap = bitmap;

                    uploadReceiptToServer(bitmap);
                } catch (Exception e) {
                    Log.e("ImagePick", "Failed to decode image: " + e.getMessage());
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        processingLayout = findViewById(R.id.layout_processing);

        // 🔹 기존: 진입 즉시 CameraXFragment 부착 → 변경: 먼저 선택 다이얼로그 표시
        showSourceChooser();
    }

    // 🔹 카메라/앨범 선택 다이얼로그
    private void showSourceChooser() {
        // 혹시 이전에 붙어있던 프래그먼트가 있다면 제거 (회전/재진입 대비)
        var prev = getSupportFragmentManager().findFragmentByTag(TAG_CAMERA);
        if (prev != null) {
            getSupportFragmentManager().beginTransaction().remove(prev).commitNowAllowingStateLoss();
        }

        new AlertDialog.Builder(this)
                .setTitle("이미지 소스 선택")
                .setItems(new CharSequence[]{"카메라로 촬영", "앨범에서 불러오기", "직접 입력하기"}, (dialog, which) -> {
                    if (which == 0) {
                        // 카메라
                        startCameraFragment();
                    } else  if(which == 1) {
                        // 앨범
                        pickImageLauncher.launch("image/*");
                    }else{
                        finish();
                    }
                })
                .setOnCancelListener(d -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .show();
    }

    // 🔹 선택 시에만 CameraXFragment 시작
    private void startCameraFragment() {
        findViewById(R.id.camera_fragment_container).setVisibility(View.VISIBLE);
        processingLayout.setVisibility(View.GONE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.camera_fragment_container, new CameraXFragment(), TAG_CAMERA)
                .commit();
    }

    /**
     * CameraXFragment가 촬영/처리 완료 시 호출해주는 콜백 (기존 로직 유지)
     */
    public void onCameraResult(Mat result) {
        if (isProcessing) { // 여러 번 들어오지 않게
            result.release();
            return;
        }
        isProcessing = true;

        var frag = getSupportFragmentManager().findFragmentByTag(TAG_CAMERA);
        if (frag instanceof CameraXFragment) {
            ((CameraXFragment) frag).stopCamera();
        }

        findViewById(R.id.camera_fragment_container).setVisibility(View.GONE);
        processingLayout.setVisibility(View.VISIBLE);

        Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmap);
        result.release();

        lastCapturedBitmap = bitmap; // 저장해 둠

        uploadReceiptToServer(bitmap);
    }

    private void uploadReceiptToServer(Bitmap bitmap) {
        // Bitmap → File 변환
        File file = new File(getCacheDir(), "receipt.jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Retrofit or OkHttp 사용 예시 (여기서는 OkHttp)
        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url("http://opensource.jabcho.org.com:8800/detect")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Upload Error", "Detect API failed: " + e.getMessage());
                runOnUiThread(() -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("Upload Error", "Detect API response not successful");
                    runOnUiThread(() -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
                    return;
                }

                String body = response.body().string();
                try {
                    JSONObject obj = new JSONObject(body);
                    String session = obj.getString("session");
                    pollProcessResult(session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void pollProcessResult(String session) {
        OkHttpClient client = new OkHttpClient();
        Handler handler = new Handler(Looper.getMainLooper());

        final Runnable[] pollTask = new Runnable[1];

        pollTask[0] = new Runnable() {
            @Override
            public void run() {
                Request request = new Request.Builder()
                        .url("http://opensource.jabcho.org.com:8800/process?session=" + session)
                        .get()
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("Process Error", "Process API failed: " + e.getMessage());
                        handler.postDelayed(pollTask[0], 2000); // ✅ 자기 자신 재호출
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            handler.postDelayed(pollTask[0], 2000);
                            return;
                        }

                        String body = response.body().string();
                        try {
                            JSONObject obj = new JSONObject(body);
                            if (obj.has("status") && "processing".equals(obj.getString("status"))) {
                                handler.postDelayed(pollTask[0], 2000); // ✅ 다시 poll
                            } else {
                                // 결과 도착
                                String analysisResult = obj.toString();
                                Log.d("Analysis Result", analysisResult);

                                Uri imageUri = saveBitmapAndGetUri(lastCapturedBitmap);
                                Intent data = new Intent();
                                data.putExtra(EXTRA_ANALYSIS_JSON, analysisResult);
                                if (imageUri != null) {
                                    data.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
                                }
                                setResult(RESULT_OK, data);
                                finish();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            handler.postDelayed(pollTask[0], 2000);
                        }
                    }
                });
            }
        };

        handler.post(pollTask[0]); // 최초 실행
    }

    // Bitmap을 캐시 폴더에 저장하고 FileProvider Uri 리턴
    private Uri saveBitmapAndGetUri(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "receipt_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

            return FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
