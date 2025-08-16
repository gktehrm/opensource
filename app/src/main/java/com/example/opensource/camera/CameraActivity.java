// com/example/opensource/camera/CameraActivity.java
package com.example.opensource.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
    private boolean isProcessing = false;   // ⬅ 중복 호출 방지

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        processingLayout = findViewById(R.id.layout_processing);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.camera_fragment_container, new CameraXFragment(), TAG_CAMERA) // ⬅ 태그 부여
                    .commit();
        }
    }

    public void onCameraResult(Mat result){
        if (isProcessing) { // ⬅ 여러 번 들어오지 않게
            result.release();
            return;
        }
        isProcessing = true;

        var frag = getSupportFragmentManager().findFragmentByTag(TAG_CAMERA);
        if (frag instanceof CameraXFragment) {
            ((CameraXFragment) frag).stopCamera(); // ⬅ 아래 3)에서 추가
        }

        findViewById(R.id.camera_fragment_container).setVisibility(View.GONE);
        processingLayout.setVisibility(View.VISIBLE);

        Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmap);
        result.release();

        String prompt =
"""
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
        analyzeBitmapWithGemini(bitmap, prompt);
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
                Intent data = new Intent();
                data.putExtra(EXTRA_ANALYSIS_JSON, analysisResult);
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
}
