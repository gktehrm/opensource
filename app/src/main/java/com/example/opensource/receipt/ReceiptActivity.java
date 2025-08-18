// com/example/opensource/receipt/ReceiptActivity.java
package com.example.opensource.receipt;

import com.bumptech.glide.Glide;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.firebase.FirebaseReceipt;
import com.example.opensource.repository.RepositoryActivity;
import com.example.opensource.camera.CameraActivity;
import com.example.opensource.receipt.entity.Receipt;
import com.example.opensource.receipt.ReceiptParser;
import com.google.firebase.firestore.DocumentReference;


import org.json.JSONObject;

public class ReceiptActivity extends AppCompatActivity {

    private TextView textFolder;
    private ImageView imageReceipt;
    private EditText editStoreName, editAddress, editPhoneNumber, editTimestamp, editReceiptTotal, editPaymentMethod, editUserInformation;
    private RecyclerView recyclerOrderItems;
    private Button btnRetake, btnSave;

    private ReceiptItemAdapter adapter;
    private Receipt receipt;

    private int mode;   // create/edit
    private int index;  // 편집 시 원본 인덱스
    private String repositoryId;
    /** 코드펜스, BOM, 스마트쿼트 제거 등 정리 */
    @Nullable
    private String normalizeAndExtractJsonObject(@Nullable String raw) {
        if (raw == null) return null;
        String s = raw.trim();

        // 1) 코드펜스 제거 ``` 또는 ```json
        s = s.replaceAll("(?is)```json", "")
                .replaceAll("(?is)```", "")
                .trim();

        // 2) 따옴표 정규화(스마트쿼트 → ASCII)
        s = s.replace('“','"').replace('”','"')
                .replace('‘','\'').replace('’','\'');

        // 3) BOM 제거
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);

        // 4) 문자열 안에서 최초 '{'부터 짝이 맞는 '}'까지 추출(설명 문구 제거용)
        int start = s.indexOf('{');
        if (start < 0) return null;

        int depth = 0;
        boolean inStr = false;
        char prev = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);

            // 문자열 내부 처리(따옴표 이스케이프 고려)
            if (c == '"' && prev != '\\') {
                inStr = !inStr;
            }

            if (!inStr) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return s.substring(start, i + 1).trim();
                    }
                }
            }
            prev = c;
        }
        return null; // 짝이 안 맞으면 null
    }


    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String analysisJson = result.getData().getStringExtra(CameraActivity.EXTRA_ANALYSIS_JSON);
                    String imageUri = result.getData().getStringExtra(CameraActivity.EXTRA_IMAGE_URI);
                    try {
                        if (analysisJson != null) {
                            String jsonStr = normalizeAndExtractJsonObject(analysisJson);
                            if (jsonStr == null) {
                                throw new org.json.JSONException("No JSON object found in response");
                            }
                            JSONObject obj = new JSONObject(jsonStr);
                            Receipt parsed = ReceiptParser.parseFromJson(obj);
                            mergeReceipt(parsed); // OCR 결과 병합
                            bindReceiptToViews();
                        }
                        if (imageUri != null) {
                            receipt.setImageUri(imageUri);
                            setImagePreview(imageUri);
                        }
                    } catch (Exception e) {
                        Log.e("분석결과처리오류", String.valueOf(e));
                        Toast.makeText(this, "분석 결과 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_detail);

        textFolder = findViewById(R.id.textFolder);
        imageReceipt = findViewById(R.id.imageReceipt);
        editStoreName = findViewById(R.id.editStoreName);
        editAddress = findViewById(R.id.editAddress);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editTimestamp = findViewById(R.id.editTimestamp);
        editReceiptTotal = findViewById(R.id.editReceiptTotal);
        editPaymentMethod = findViewById(R.id.editPaymentMethod);
        editUserInformation = findViewById(R.id.editUserInformation);

        recyclerOrderItems = findViewById(R.id.recyclerOrderItems);
        btnRetake = findViewById(R.id.btnRetake);
        btnSave = findViewById(R.id.btnSave);

        mode = getIntent().getIntExtra(RepositoryActivity.EXTRA_MODE, RepositoryActivity.MODE_EDIT);
        index = getIntent().getIntExtra(RepositoryActivity.EXTRA_INDEX, -1);
        receipt = (Receipt) getIntent().getSerializableExtra(RepositoryActivity.EXTRA_RECEIPT);
        if (receipt == null) receipt = new Receipt();

        // 저장소 ID 받아오기
        repositoryId = getIntent().getStringExtra("repositoryId");
        if (repositoryId == null) {
            Toast.makeText(this, "저장소 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        textFolder.setText("내 영수증 폴더");

        adapter = new ReceiptItemAdapter(receipt.getItemList());
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderItems.setAdapter(adapter);

        bindReceiptToViews();

        btnRetake.setOnClickListener(v -> openCamera());

        btnSave.setOnClickListener(v -> {
            applyViewsToReceipt();

            FirebaseReceipt firebase = new FirebaseReceipt();
            Uri uploadUri = (receipt.getImageUri() != null) ? Uri.parse(receipt.getImageUri()) : null;

            firebase.upsertReceipt(repositoryId, receipt, uploadUri, task -> {
                if (task.isSuccessful()) {
                    DocumentReference docRef = task.getResult();
                    Log.d("ReceiptActivity", "저장 성공: " + docRef.getId());

                    // 저장된 URL 다시 UI 반영
                    bindReceiptToViews();

                    // 결과 전달
                    Intent result = new Intent();
                    result.putExtra(RepositoryActivity.EXTRA_RECEIPT, receipt);
                    result.putExtra(RepositoryActivity.EXTRA_INDEX, index);
                    setResult(RESULT_OK, result);

                    finish();
                } else {
                    Log.e("ReceiptActivity", "저장 실패", task.getException());
                    Toast.makeText(this, "저장 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        if (mode == RepositoryActivity.MODE_CREATE) {
            openCamera();
        }
    }

    private void bindReceiptToViews() {
        editStoreName.setText(nullToEmpty(receipt.getStoreName()));
        editAddress.setText(nullToEmpty(receipt.getAddress()));
        editTimestamp.setText(nullToEmpty(receipt.getTimestamp()));
        editReceiptTotal.setText(String.valueOf(receipt.getAmount()));
        editPaymentMethod.setText(nullToEmpty(receipt.getPaymentMethod()));
        editUserInformation.setText(nullToEmpty(receipt.getUserInformation()));

        // 🔹 이미지 표시 (우선순위: Firebase imageUrl → 로컬 imageUri → placeholder)
        if (receipt.getImageUrl() != null && !receipt.getImageUrl().isEmpty()) {
            // Firebase 저장된 URL 불러오기
            Glide.with(this)
                    .load(receipt.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(imageReceipt);
        } else if (receipt.getImageUri() != null && !receipt.getImageUri().isEmpty()) {
            // 로컬에서 찍은 직후 미리보기
            imageReceipt.setImageURI(Uri.parse(receipt.getImageUri()));
        } else {
            // 기본 이미지
            imageReceipt.setImageResource(R.drawable.ic_image_placeholder);
        }

        adapter.notifyDataSetChanged();
    }

    private void setImagePreview(String uriStr) {
        try {
            imageReceipt.setImageURI(Uri.parse(uriStr));
        } catch (Exception e) {
            imageReceipt.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_image_placeholder));
        }
    }

    private void applyViewsToReceipt() {
        receipt.setStoreName(editStoreName.getText().toString());
        receipt.setAddress(editAddress.getText().toString());
        receipt.setTimestamp(editTimestamp.getText().toString());
        try {
            int total = Integer.parseInt(editReceiptTotal.getText().toString());
            receipt.setAmount(total);
        } catch (NumberFormatException ignored) {}
        receipt.setPaymentMethod(editPaymentMethod.getText().toString());
        receipt.setUserInformation(editUserInformation.getText().toString());
        // item 리스트는 별도 편집 UI를 붙이거나 OCR 병합 로직을 사용
    }

    private void mergeReceipt(Receipt parsed) {
        // 비어있거나 기존 값보다 유의미하면 덮어쓰기 (간단 규칙)
        if (isBetter(parsed.getStoreName(), receipt.getStoreName())) receipt.setStoreName(parsed.getStoreName());
        if (isBetter(parsed.getAddress(), receipt.getAddress())) receipt.setAddress(parsed.getAddress());
        if (isBetter(parsed.getTimestamp(), receipt.getTimestamp())) receipt.setTimestamp(parsed.getTimestamp());
        if (parsed.getAmount() > 0) receipt.setAmount(parsed.getAmount());
        if (isBetter(parsed.getPaymentMethod(), receipt.getPaymentMethod())) receipt.setPaymentMethod(parsed.getPaymentMethod());
        if (isBetter(parsed.getUserInformation(), receipt.getUserInformation())) receipt.setUserInformation(parsed.getUserInformation());

        // 아이템: 간단히 전부 교체. (원하면 merge 규칙 세분화)
        if (parsed.getItemList() != null && !parsed.getItemList().isEmpty()) {
            receipt.getItemList().clear();
            for (var it : parsed.getItemList()) receipt.addItem(it);
        }
        // 이미지 Uri는 cameraLauncher에서 별도 세팅
    }

    private boolean isBetter(String newVal, String oldVal) {
        return newVal != null && !newVal.isEmpty() && (oldVal == null || oldVal.isEmpty());
    }
    private String nullToEmpty(String s) { return s == null ? "" : s; }

    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        cameraLauncher.launch(intent);
    }
}