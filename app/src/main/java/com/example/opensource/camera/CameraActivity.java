// com/example/opensource/camera/CameraActivity.java
package com.example.opensource.camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java4");
        Log.d("OpenCV", "OpenCV native lib loaded");
    }

    public static final String EXTRA_ANALYSIS_JSON = "analysisJson";
    public static final String EXTRA_IMAGE_URI = "imageUri";

    private String lastCapturedImageUri;
    private View processingLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        processingLayout = findViewById(R.id.layout_processing);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.camera_fragment_container, new CameraXFragment())
                    .commit();
        }
    }

    public void onCameraResult(Mat result, @Nullable String imageUriString){
        this.lastCapturedImageUri = imageUriString;

        // 카메라 UI 숨기고 "처리중" 표시
        findViewById(R.id.camera_fragment_container).setVisibility(View.GONE);
        processingLayout.setVisibility(View.VISIBLE);

        Bitmap bitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bitmap);
        result.release();

        String prompt = "보낸 이미지는 영수증입니다. " +
                "다음 JSON 스키마로만 응답하세요. " +
                "{\"storeName\":\"\",\"address\":\"\",\"phoneNumber\":\"\",\"timestamp\":\"yyyy-MM-dd HH:mm:ss\"," +
                "\"paymentMethod\":\"\",\"userInformation\":\"\",\"itemList\":[{\"productName\":\"\",\"quantity\":1,\"unitPrice\":1000}],\"receiptTotal\":0}";

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
                Log.d("Analysis Result", analysisResult);
                Intent data = new Intent();
                data.putExtra(EXTRA_ANALYSIS_JSON, analysisResult);
                if (lastCapturedImageUri != null) data.putExtra(EXTRA_IMAGE_URI, lastCapturedImageUri);
                setResult(RESULT_OK, data);
                finish(); // ReceiptActivity로 복귀
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                setResult(RESULT_CANCELED);
                finish();
            }
        }, executor);
    }
}
