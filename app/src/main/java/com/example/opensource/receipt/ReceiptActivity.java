// com/example/opensource/receipt/ReceiptActivity.java
package com.example.opensource.receipt;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.repository.RepositoryActivity;
import com.example.opensource.camera.CameraActivity;
import com.example.opensource.receipt.entity.Receipt;

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

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String analysisJson = result.getData().getStringExtra(CameraActivity.EXTRA_ANALYSIS_JSON);
                    String imageUri = result.getData().getStringExtra(CameraActivity.EXTRA_IMAGE_URI);
                    try {
                        if (analysisJson != null) {
                            JSONObject obj = new JSONObject(analysisJson);
                            Receipt parsed = ReceiptParser.parseFromJson(obj);
                            mergeReceipt(parsed); // OCR 결과를 현재 Receipt에 병합
                            bindReceiptToViews();
                        }
                        if (imageUri != null) {
                            receipt.setImageUri(imageUri);
                            setImagePreview(imageUri);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "분석 결과 처리 중 오류", Toast.LENGTH_SHORT).show();
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

        textFolder.setText("내 영수증 폴더");

        adapter = new ReceiptItemAdapter(receipt.getItemList());
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerOrderItems.setAdapter(adapter);

        bindReceiptToViews();

        btnRetake.setOnClickListener(v -> openCamera());

        btnSave.setOnClickListener(v -> {
            // 폼 → Receipt
            applyViewsToReceipt();
            // 결과 반환
            Intent result = new Intent();
            result.putExtra(RepositoryActivity.EXTRA_RECEIPT, receipt);
            result.putExtra(RepositoryActivity.EXTRA_INDEX, index);
            setResult(RESULT_OK, result);
            finish();
        });

        // 생성 모드이면 즉시 카메라 실행해 OCR로 채움
        if (mode == RepositoryActivity.MODE_CREATE) {
            openCamera();
        }
    }

    private void bindReceiptToViews() {
        editStoreName.setText(nullToEmpty(receipt.getStoreName()));
        editAddress.setText(nullToEmpty(receipt.getAddress()));
        editPhoneNumber.setText(nullToEmpty(receipt.getPhoneNumber()));
        editTimestamp.setText(nullToEmpty(receipt.getTimestamp()));
        editReceiptTotal.setText(String.valueOf(receipt.getReceiptTotal()));
        editPaymentMethod.setText(nullToEmpty(receipt.getPaymentMethod()));
        editUserInformation.setText(nullToEmpty(receipt.getUserInformation()));

        if (receipt.getImageUri() != null) {
            setImagePreview(receipt.getImageUri());
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
        receipt.setPhoneNumber(editPhoneNumber.getText().toString());
        receipt.setTimestamp(editTimestamp.getText().toString());
        try {
            int total = Integer.parseInt(editReceiptTotal.getText().toString());
            receipt.setReceiptTotal(total);
        } catch (NumberFormatException ignored) {}
        receipt.setPaymentMethod(editPaymentMethod.getText().toString());
        receipt.setUserInformation(editUserInformation.getText().toString());
        // item 리스트는 별도 편집 UI를 붙이거나 OCR 병합 로직을 사용
    }

    private void mergeReceipt(Receipt parsed) {
        // 비어있거나 기존 값보다 유의미하면 덮어쓰기 (간단 규칙)
        if (isBetter(parsed.getStoreName(), receipt.getStoreName())) receipt.setStoreName(parsed.getStoreName());
        if (isBetter(parsed.getAddress(), receipt.getAddress())) receipt.setAddress(parsed.getAddress());
        if (isBetter(parsed.getPhoneNumber(), receipt.getPhoneNumber())) receipt.setPhoneNumber(parsed.getPhoneNumber());
        if (isBetter(parsed.getTimestamp(), receipt.getTimestamp())) receipt.setTimestamp(parsed.getTimestamp());
        if (parsed.getReceiptTotal() > 0) receipt.setReceiptTotal(parsed.getReceiptTotal());
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
