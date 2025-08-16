// com/example/opensource/repository/RepositoryActivity.java
package com.example.opensource.repository;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.receipt.ReceiptActivity;
import com.example.opensource.receipt.ReceiptAdapter;
import com.example.opensource.receipt.ReceiptManager;
import com.example.opensource.receipt.entity.Receipt;

import java.util.List;

public class RepositoryActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_RECEIPT = "receipt";
    public static final String EXTRA_INDEX = "index";
    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private TextView tvFileName, tvNoReceipt;
    private ReceiptManager receiptManager;
    private RecyclerView recyclerView;
    private ReceiptAdapter receiptAdapter;

    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int idx = data.getIntExtra(EXTRA_INDEX, -1);
                    Receipt updated = (Receipt) data.getSerializableExtra(EXTRA_RECEIPT);
                    if (idx >= 0 && updated != null) {
                        receiptManager.updateReceipt(idx, updated);
                        updateReceiptListUI();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> createLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Receipt created = (Receipt) result.getData().getSerializableExtra(EXTRA_RECEIPT);
                    if (created != null) {
                        receiptManager.addReceipt(created);
                        updateReceiptListUI();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repository);

        tvFileName = findViewById(R.id.tvFileName);
        tvNoReceipt = findViewById(R.id.tvNoReceipt);

        receiptManager = new ReceiptManager();

        recyclerView = findViewById(R.id.receiptRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        receiptAdapter = new ReceiptAdapter(receiptManager.getAllReceipts(),
                (position, receipt) -> {
                    // 편집 모드로 상세 화면 열기
                    Intent intent = new Intent(this, ReceiptActivity.class);
                    intent.putExtra(EXTRA_MODE, MODE_EDIT);
                    intent.putExtra(EXTRA_INDEX, position);
                    intent.putExtra(EXTRA_RECEIPT, receipt);
                    editLauncher.launch(intent);
                });
        recyclerView.setAdapter(receiptAdapter);

        updateReceiptListUI();

        ImageButton btnOcrScan = findViewById(R.id.btnOcrScan);
        btnOcrScan.setOnClickListener(v -> {
            // 생성 모드: 빈 ReceiptActivity 열고, 여기서 즉시 CameraActivity 실행
            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra(EXTRA_MODE, MODE_CREATE);
            createLauncher.launch(intent);
        });

        ImageButton btnConvert = findViewById(R.id.btnConvertToFile);
        btnConvert.setOnClickListener(v -> {
            // 기존 코드 유지
            // startActivity(new Intent(this, FileGeneratorActivity.class));
        });

        ImageButton btnSort = findViewById(R.id.btnSort);
        btnSort.setOnClickListener(v -> showSortDialog());

        String fileName = getIntent().getStringExtra("fileName");
        if (fileName != null) tvFileName.setText(fileName);

        // 검색바 키 리스너 필요 시 기존 코드 사용
    }

    private void showSortDialog() {
        String[] sortOptions = {"최신순", "오래된순"};
        new AlertDialog.Builder(this)
                .setTitle("정렬 기준 선택")
                .setItems(sortOptions, (dialog, which) -> {
                    switch (which) {
                        case 0: receiptManager.sortByDate(false); break;
                        case 1: receiptManager.sortByDate(true); break;
                    }
                    updateReceiptListUI();
                })
                .show();
    }

    private void updateReceiptListUI() {
        List<Receipt> list = receiptManager.getAllReceipts();
        receiptAdapter.setReceipts(list);
        if (list == null || list.isEmpty()) {
            tvNoReceipt.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            tvNoReceipt.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }
}
