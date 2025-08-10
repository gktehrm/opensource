package com.example.opensource;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MyPageActivity extends AppCompatActivity {

    TextView tvNickname;

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
    }
}
