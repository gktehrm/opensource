package com.example.opensource.repository;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.camera.CameraActivity;
import com.example.opensource.file.FileGeneratorActivity;
import com.example.opensource.receipt.ReceiptAdapter;
import com.example.opensource.receipt.ReceiptManager;
import com.example.opensource.receipt.entity.Receipt;

import java.util.List;

public class RepositoryActivity extends AppCompatActivity {
    private TextView tvFileName;
    private ReceiptManager receiptManager;
    private RecyclerView recyclerView;
    private TextView tvNoReceipt;
    private ReceiptAdapter receiptAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repository);

        tvFileName = findViewById(R.id.tvFileName);
        tvNoReceipt = findViewById(R.id.tvNoReceipt);

        // 영수증 데이터 관리 객체 생성
        receiptManager = new ReceiptManager();

        recyclerView = findViewById(R.id.receiptRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        receiptAdapter = new ReceiptAdapter(receiptManager.getAllReceipts());
        recyclerView.setAdapter(receiptAdapter);

        updateReceiptListUI();

        ImageButton btnOcrScan = findViewById(R.id.btnOcrScan);
        btnOcrScan.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        });

        ImageButton btnConvert = findViewById(R.id.btnConvertToFile);
        btnConvert.setOnClickListener(v -> {
            Intent intent = new Intent(RepositoryActivity.this, FileGeneratorActivity.class);
            startActivity(intent);
        });

        ImageButton btnSort = findViewById(R.id.btnSort);
        btnSort.setOnClickListener(v -> showSortDialog());

        // 파일명 받기
        String fileName = getIntent().getStringExtra("fileName");
        if (fileName != null) {
            tvFileName.setText(fileName);
        }

        // 검색 처리 예시 (EditText 입력 처리)
        findViewById(R.id.searchBar).setOnKeyListener((v, keyCode, event) -> {
            // 키 입력 이벤트 감지 후 (예: 엔터), 검색 결과 갱신
            // String keyword = ((EditText) v).getText().toString();
            // List<Receipt> result = receiptManager.searchByStore(keyword);
            // receiptAdapter.setReceipts(result);
            // return true;
            return false;
        });
    }

    private void showSortDialog() {
        String[] sortOptions = {"최신순", "오래된순"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("정렬 기준 선택")
                .setItems(sortOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            receiptManager.sortByDate(false);
                            break;
                        case 1:
                            receiptManager.sortByDate(true);
                            break;
                    }
                    updateReceiptListUI();
                });
        builder.show();
    }
    // 영수증 리스트가 갱신될 때마다 호출
    private void updateReceiptListUI() {
        List<Receipt> list = receiptManager.getAllReceipts();
        receiptAdapter.setReceipts(list);

        if (list == null || list.isEmpty()) {
            tvNoReceipt.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoReceipt.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
