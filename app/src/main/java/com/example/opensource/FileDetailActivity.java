package com.example.opensource;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class FileDetailActivity extends AppCompatActivity {

    TextView tvFileName;

    private void showSortDialog() {
        String[] sortOptions = {"최신순", "오래된순"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("정렬 기준 선택")
                .setItems(sortOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Toast.makeText(this, "최신순 정렬", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            Toast.makeText(this, "오래된순 정렬", Toast.LENGTH_SHORT).show();
                            break;
                    }
                });
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);

        tvFileName = findViewById(R.id.tvFileName);

        ImageButton btnOcrScan = findViewById(R.id.btnOcrScan);
        btnOcrScan.setOnClickListener(v -> {
            // OCR 기능 호출
        });

        ImageButton btnConvert = findViewById(R.id.btnConvertToFile);
        btnConvert.setOnClickListener(v -> {
            // 파일 저장 기능 호출
        });

        ImageButton btnSort = findViewById(R.id.btnSort);
        btnSort.setOnClickListener(v -> showSortDialog());

        // 파일명 받기
        String fileName = getIntent().getStringExtra("fileName");
        if (fileName != null) {
            tvFileName.setText(fileName);
        }

        // 버튼 클릭 리스너는 다른 팀원이 구현 예정이므로 생략
    }
}
