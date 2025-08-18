package com.example.opensource.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.opensource.MainActivity;
import com.example.opensource.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;


public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private CheckBox rememberCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        com.google.firebase.FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();

        SharedPreferences prefs = getSharedPreferences("opensource", MODE_PRIVATE);

        if (prefs.getBoolean("autoLogin", false) && auth.getCurrentUser() != null) {
            startMainActivity();
            return;
        }

        Button loginButton = findViewById(R.id.button);
        loginButton.setOnClickListener(v -> loginUser());

        EditText editText = findViewById(R.id.Password);
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                loginUser();
                return true;
            }
            return false;
        });

        Button signupButton = findViewById(R.id.buttonSignup);
        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        rememberCheckBox = findViewById(R.id.checkbox_remember);
    }

    private void loginUser() {
        String email = ((EditText) findViewById(R.id.EmailAddress)).getText().toString();
        String password = ((EditText) findViewById(R.id.Password)).getText().toString();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences("opensource", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("autoLogin", rememberCheckBox.isChecked());
                        editor.apply();

                        Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        startMainActivity();
                    } else {
                        Log.e("LoginActivity", "로그인 실패", task.getException());
                        Toast.makeText(LoginActivity.this, "로그인 실패: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        ProfileManager.getUserName(_username -> {
            if (_username != null) {
                intent.putExtra("username", (_username));
                startActivity(intent);
                finish();
            }
        });
    }
}