package com.example.opensource.menu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.opensource.R;
import com.example.opensource.auth.LoginActivity;  // 🔹 LoginActivity import 추가
import com.google.firebase.auth.FirebaseAuth;

public class MyPageActivity extends AppCompatActivity {

    TextView tvNickname;
    private String nickname;

    private final ActivityResultLauncher<Intent> editInfoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String updatedNickname = result.getData().getStringExtra("updatedNickname");
                    if (updatedNickname != null) {
                        tvNickname.setText(updatedNickname);
                        nickname = updatedNickname;

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("updatedNickname", updatedNickname);
                        setResult(RESULT_OK, returnIntent);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        tvNickname = findViewById(R.id.tvNickname);

        Intent intent = getIntent();
        nickname = intent.getStringExtra("nickname");
        if (nickname != null) {
            tvNickname.setText(nickname);
        }

        Button btnEditInfo = findViewById(R.id.btnEditInfo);
        btnEditInfo.setOnClickListener(v -> {
            Intent editIntent = new Intent(MyPageActivity.this, EditMyInfoActivity.class);
            editInfoLauncher.launch(editIntent);
        });

//        Button btnMyFiles = findViewById(R.id.btnMyFiles);
//        btnMyFiles.setOnClickListener(v -> {
//            Intent fileIntent = new Intent(MyPageActivity.this, MyFileActivity.class);
//            intent.putExtra("nickname", nickname);
//            startActivity(fileIntent);
//        });

        // 🔹 로그아웃 버튼
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            // Firebase 로그아웃
            FirebaseAuth.getInstance().signOut();

            // 로그인 화면으로 이동
            Intent loginIntent = new Intent(MyPageActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);

            finish(); // 현재 Activity 종료
        });
    }
}




