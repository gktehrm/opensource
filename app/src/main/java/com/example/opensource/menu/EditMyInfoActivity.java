package com.example.opensource.menu;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.opensource.R;
import com.example.opensource.auth.ProfileManager;

public class EditMyInfoActivity extends AppCompatActivity {

    EditText etNickname;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        Intent intent = getIntent();
        String nickname = intent.getStringExtra("nickname");
        if (nickname != null) {
            etNickname.setText(nickname);
        }

        etNickname = findViewById(R.id.etNickname);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String newNickname = etNickname.getText().toString().trim();

            if (newNickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else {
                ProfileManager.updateUserName(newNickname, username -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updatedNickname", newNickname);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }
        });
    }
}
