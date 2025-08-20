// com/example/opensource/camera/CameraActivity.java
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

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    // 🔹 앨범에서 이미지 선택 런처 (READ 권한 없이도 사용 가능한 GetContent)
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

                    String prompt = getReceiptPrompt();
                    analyzeBitmapWithGemini(bitmap, prompt);
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

        String prompt = getReceiptPrompt();
        analyzeBitmapWithGemini(bitmap, prompt);
    }

    // 🔹 프롬프트 생성 분리 (카메라/앨범 공통 사용)
    private String getReceiptPrompt() {
        return """
보낸 이미지는 영수증입니다. \
다음 JSON 스키마로만 응답하세요. \
{"storeName":"","address":"","phoneNumber":"","timestamp":"yyyy-MM-dd HH:mm:ss",\
"paymentMethod":"","userInformation":"","itemList":[{"productName":"","quantity":1,"unitPrice":1000}],"receiptTotal":0}\
각각은 다음을 의미합니다.\s
storeName: 상호명(가게 이름)
address: 주소(실제 주소)
phoneNumber: 연락처
timestamp: 거래 일시(yyyy-MM-dd HH:mm:ss)
paymentMethod: 결제 정보(카드 결제, 현금 결제 등)
userInformation: 사용 정보(음식점, 물품 구매, 여가 생활, 카페, 기타 등 이 부분은 자동으로 작성)
itemList: 주문 내역
productName: 상품명
quantity: 상품 개수
unitPrice: 단가
receiptTotal: 금액 소계
""";
    }

    private void analyzeBitmapWithGemini(Bitmap bitmapToAnalyze, String prompt) {
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", getString(R.string.gemini_api_key));
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addImage(bitmapToAnalyze)
                .addText(prompt)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(GenerateContentResponse res) {
                String analysisResult = res.getText();
                Log.d("Analysis Result", Objects.requireNonNull(analysisResult));

                // Bitmap → Uri 변환
                Uri imageUri = saveBitmapAndGetUri(lastCapturedBitmap);
                // Intent에 JSON + 이미지 Uri 전달
                Intent data = new Intent();
                data.putExtra(EXTRA_ANALYSIS_JSON, analysisResult);
                if (imageUri != null) {
                    data.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
                }
                setResult(RESULT_OK, data);
                finish();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e("Analysis Error", "Analysis failed: " + t.getMessage());
                setResult(RESULT_CANCELED);
                finish();
            }
        }, executor);
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
