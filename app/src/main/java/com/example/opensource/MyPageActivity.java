package com.example.opensource;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MyPageActivity extends AppCompatActivity {

    TextView tvNickname;

    // 1. 결과를 받을 ActivityResultLauncher 등록
    private final ActivityResultLauncher<Intent> editInfoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String updatedNickname = result.getData().getStringExtra("updatedNickname");
                    if (updatedNickname != null) {
                        tvNickname.setText(updatedNickname);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        tvNickname = findViewById(R.id.tvNickname);

        // 메인에서 전달된 닉네임 받기
        Intent intent = getIntent();
        String nickname = intent.getStringExtra("nickname");
        if (nickname != null) {
            tvNickname.setText(nickname);
        }

        Button btnEditInfo = findViewById(R.id.btnEditInfo);
        btnEditInfo.setOnClickListener(v -> {
            Intent editIntent = new Intent(MyPageActivity.this, EditInfoActivity.class);
            editInfoLauncher.launch(editIntent); // 새 방식으로 실행
        });

        Button btnMyFiles = findViewById(R.id.btnMyFiles);
        btnMyFiles.setOnClickListener(v -> {
            Intent fileIntent = new Intent(MyPageActivity.this, MyFileActivity.class);
            startActivity(fileIntent);
        });
    }
}
