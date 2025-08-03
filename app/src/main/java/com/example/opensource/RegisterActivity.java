package com.example.opensource;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        Button registerButton = findViewById(R.id.buttonRegister);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String username = ((EditText) findViewById(R.id.editTextUsername)).getText().toString();
        String email = ((EditText) findViewById(R.id.editTextEmail)).getText().toString();
        String password = ((EditText) findViewById(R.id.editTextPassword)).getText().toString();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserData(username, email);
                        Toast.makeText(RegisterActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    } else {
                        Toast.makeText(RegisterActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserData(String username, String email) {
        String uid = auth.getCurrentUser().getUid();  // 로그인된 사용자의 UID

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("createdBy", uid);

        firestore.collection("users")
                .document(uid)  // 직접 UID로 문서 ID 지정!
                .set(user)
                .addOnSuccessListener(aVoid ->
                        Log.d("RegisterActivity", "Firestore에 사용자 정보 저장 성공 (UID: " + uid + ")")
                )
                .addOnFailureListener(e ->
                        Log.e("RegisterActivity", "Firestore 저장 실패", e)
                );
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();

        //private void navigateToLoginActivity() {
          //  Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            //startActivity(intent);
            //finish();  // 회원가입 화면 종료
    }
}
