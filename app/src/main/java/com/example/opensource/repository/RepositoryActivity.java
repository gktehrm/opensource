package com.example.opensource.repository;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.R;
import com.example.opensource.file.FileGeneratorActivity;
import com.example.opensource.receipt.ReceiptActivity;
import com.example.opensource.receipt.entity.Receipt;
import com.example.opensource.receipt.entity.ReceiptMapper;
import com.example.opensource.firebase.FirebaseReceipt;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * 저장소 화면 액티비티
 * - 영수증 목록 표시
 * - 영수증 생성/수정
 * - Firestore 연동
 */
public class RepositoryActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_RECEIPT = "receipt";
    public static final String EXTRA_INDEX = "index";
    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private TextView tvNoReceipt;
    private RecyclerView recyclerView;
    private ReceiptAdapter receiptAdapter;
    private List<Receipt> receiptList = new ArrayList<>();
    private String repositoryId;

    private final FirebaseReceipt firebaseReceipt = new FirebaseReceipt();

    /** 영수증 수정 후 결과 처리 */
    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    int idx = data.getIntExtra(EXTRA_INDEX, -1);
                    Receipt updated = (Receipt) data.getSerializableExtra(EXTRA_RECEIPT);
                    String imageUriStr = data.getStringExtra("imageUri");

                    if (idx >= 0 && updated != null) {
                        receiptList.set(idx, updated);
                        updateReceiptListUI();

                        // Firestore에도 반영
                        Uri uploadUri = (imageUriStr != null) ? Uri.parse(imageUriStr) : null;
                        firebaseReceipt.upsertReceipt(repositoryId, updated, uploadUri, task -> {
                            if (!task.isSuccessful()) {
                                Log.e("RepositoryActivity", "영수증 수정 업로드 실패", task.getException());
                            }
                        });
                    }
                }
            });

    /** 영수증 새로 생성 후 결과 처리 */
    private final ActivityResultLauncher<Intent> createLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Receipt created = (Receipt) data.getSerializableExtra(EXTRA_RECEIPT);
                    String imageUriStr = data.getStringExtra("imageUri");

                    if (created != null) {
                        receiptList.add(created);
                        updateReceiptListUI();

                        // Firestore 저장
                        Uri uploadUri = (imageUriStr != null) ? Uri.parse(imageUriStr) : null;
                        firebaseReceipt.upsertReceipt(repositoryId, created, uploadUri, task -> {
                            if (!task.isSuccessful()) {
                                Log.e("RepositoryActivity", "영수증 생성 업로드 실패", task.getException());
                            }
                        });
                    } else {
                        android.widget.Toast.makeText(this, "생성 결과에 영수증 데이터가 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repository);

        TextView tvFileName = findViewById(R.id.tvFileName);
        tvNoReceipt = findViewById(R.id.tvNoReceipt);

        // 🔑 MainActivity에서 넘겨준 repositoryId 받기
        repositoryId = getIntent().getStringExtra("repositoryId");

        recyclerView = findViewById(R.id.receiptRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        receiptAdapter = new ReceiptAdapter(receiptList,
                (position, receipt) -> {
                    // 편집 모드로 상세 화면 열기
                    Intent intent = new Intent(this, ReceiptActivity.class);
                    intent.putExtra(EXTRA_MODE, MODE_EDIT);
                    intent.putExtra(EXTRA_INDEX, position);
                    intent.putExtra(EXTRA_RECEIPT, receipt);
                    intent.putExtra("repositoryId", repositoryId);
                    editLauncher.launch(intent);
                });
        recyclerView.setAdapter(receiptAdapter);

        //  Firestore에서 영수증 불러오기
        loadReceiptsFromFirebase();

        ImageButton btnOcrScan = findViewById(R.id.btnOcrScan);
        btnOcrScan.setOnClickListener(v -> {
            // 생성 모드: 빈 ReceiptActivity 열고 CameraActivity 실행
            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra(EXTRA_MODE, MODE_CREATE);
            intent.putExtra("repositoryId", repositoryId);
            createLauncher.launch(intent);
        });

        ImageButton btnConvert = findViewById(R.id.btnConvertToFile);
        btnConvert.setOnClickListener(v -> {
            Intent intent = new Intent(this, FileGeneratorActivity.class);
            intent.putExtra("repositoryId", repositoryId);  // 🔹 repositoryId 넘기기
            startActivity(intent);
        });


        ImageButton btnSort = findViewById(R.id.btnSort);
        btnSort.setOnClickListener(v -> showSortDialog());

        String fileName = getIntent().getStringExtra("fileName");
        if (fileName != null) tvFileName.setText(fileName);
    }

    /** Firestore에서 해당 repositoryId의 영수증을 불러오기 */
    private void loadReceiptsFromFirebase() {
        firebaseReceipt.loadReceipts(repositoryId, task -> {
            if (task.isSuccessful()) {
                receiptList.clear();
                for (Receipt r : task.getResult()) {
                    if (r == null) continue;
                    receiptList.add(r);
                }
                updateReceiptListUI();
            } else {
                Log.e("RepositoryActivity", "영수증 불러오기 실패", task.getException());
            }
        });
    }

    /** 영수증 정렬 다이얼로그 표시 */
    private void showSortDialog() {
        String[] sortOptions = {"최신순", "오래된순"};
        new AlertDialog.Builder(this)
                .setTitle("정렬 기준 선택")
                .setItems(sortOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            receiptList.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
                            break;
                        case 1:
                            receiptList.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
                            break;
                    }
                    updateReceiptListUI();
                })
                .show();
    }

    /** UI 갱신 */
    private void updateReceiptListUI() {
        receiptAdapter.setReceipts(receiptList);
        if (receiptList == null || receiptList.isEmpty()) {
            tvNoReceipt.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            tvNoReceipt.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }
}
