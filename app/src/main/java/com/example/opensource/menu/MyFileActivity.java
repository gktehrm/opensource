package com.example.opensource.menu;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.opensource.R;

public class MyFileActivity extends AppCompatActivity {

    EditText searchBar;
    ImageButton btnSort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_file); // XML 이름 맞게 변경

        // 1. XML 요소 연결
        searchBar = findViewById(R.id.searchBar);
        btnSort = findViewById(R.id.btnSort);

        // 2. 정렬 버튼 클릭 시 다이얼로그 띄우기
        btnSort.setOnClickListener(v -> showSortDialog());

        // 3. 검색바 이벤트 (엔터 키 입력 시 검색)
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            String keyword = searchBar.getText().toString().trim();
            if (!keyword.isEmpty()) {
                Toast.makeText(this, "검색: " + keyword, Toast.LENGTH_SHORT).show();
            }
            return false;
        });
    }

    // 정렬 다이얼로그
    private void showSortDialog() {
        String[] sortOptions = {"최신순", "오래된순"};

        new AlertDialog.Builder(this)
                .setTitle("정렬 기준 선택")
                .setItems(sortOptions, (dialog, which) -> {
                    if (which == 0) {
                        Toast.makeText(this, "최신순 정렬", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "오래된순 정렬", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
}
