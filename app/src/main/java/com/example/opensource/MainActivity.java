package com.example.opensource;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.auth.LoginActivity;
import com.example.opensource.auth.ProfileManager;
import com.example.opensource.menu.MyPageActivity;
import com.example.opensource.repository.RepositoryInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RepositoryListAdapter adapter;
    private List<RepositoryInfo> fileList;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private String userName;
    private static final String PREFS_NAME = "repository_prefs";
    private static final String KEY_FILE_LIST = "file_list";
    private final ActivityResultLauncher<Intent> myPageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String updatedNickname = result.getData().getStringExtra("updatedNickname");
                    if (updatedNickname != null) {
                        userName = updatedNickname;
                        updateUserName(userName);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 로그인 확인 후 안되어있으면 로그인 화면으로 이동
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        Log.d("MainActivity", "User UID: " + user.getUid());
        Log.d("MainActivity", "User Display Name: " + user.getDisplayName());

        userName = getIntent().getStringExtra("username");
        updateUserName(userName);
        Log.d("MainActivity", "Received username: " + userName);

        init();
        loadRepositoryList();   // <-- 추가
    }

    private void init(){
        ImageButton myPageButton = findViewById(R.id.btnMypage);
        myPageButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            intent.putExtra("nickname", userName);
            myPageLauncher.launch(intent); // 변경
        });

        recyclerView = findViewById(R.id.recyclerView);

        fileList = new ArrayList<>();

        // position 0번에 항상 플러스 카드가 고정되도록 null 추가
        fileList.add(null);

        adapter = new RepositoryListAdapter(MainActivity.this, fileList, this::showAddFolderDialog);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    public void updateUserName(String userName) {
        this.userName = userName;
        ((TextView) findViewById(R.id.main_act_profile_name)).setText(userName);
    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("폴더 이름을 입력하세요");

        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String folderName = input.getText().toString().trim();

            if (!folderName.isEmpty()) {
                String date = new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault()).format(new Date());
                RepositoryInfo newFolder = new RepositoryInfo(folderName, "마지막 수정 " + date);
                fileList.add(newFolder);
                adapter.notifyItemInserted(fileList.size() - 1);
                saveRepositoryList();   // <-- 추가
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    @Override
    protected void onPause() {
        super.onPause();
        saveRepositoryList();   // <-- 추가: 앱 종료/백그라운드 시 저장
    }

    private void saveRepositoryList() {
        JSONArray array = new JSONArray();
        for (int i = 1; i < fileList.size(); i++) {  // i=1부터: position 0은 null(플러스 카드)
            RepositoryInfo info = fileList.get(i);
            if (info != null) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("title", info.getTitle());
                    obj.put("date", info.getDate());
                    array.put(obj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_FILE_LIST, array.toString()).apply();
    }

    private void loadRepositoryList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String json = prefs.getString(KEY_FILE_LIST, null);

        fileList.clear();
        fileList.add(null); // 항상 플러스 카드 유지

        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String title = obj.getString("title");
                    String date = obj.getString("date");
                    fileList.add(new RepositoryInfo(title, date));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
