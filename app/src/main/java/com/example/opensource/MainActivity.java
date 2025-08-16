package com.example.opensource;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.opensource.auth.LoginActivity;
import com.example.opensource.repository.RepositoryController;
import com.example.opensource.repository.RepositorySearchHelper;
import com.example.opensource.repository.RepositoryInfo;
import com.example.opensource.menu.MyPageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RepositoryListAdapter adapter;
    private List<RepositoryInfo> fileList;
    private List<RepositoryInfo> folderList;

    private FirebaseAuth auth;
    private FirebaseUser user;
    private String userName;
    private EditText searchBar;  // 전역으로 선언


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
            return;
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
        searchBar = findViewById(R.id.searchBar);
        fileList = new ArrayList<>();
        folderList = new ArrayList<>();

        // position 0번에 항상 플러스 카드가 고정되도록 null 추가
        fileList.add(null);

        adapter = new RepositoryListAdapter(new RepositoryListAdapter.FolderActionListener() {
            @Override
            public void onAddFolder() {
                showAddFolderDialog();
            }

            @Override
            public void onDeleteFolder(RepositoryInfo file) {
                deleteFolder(file);
            }

            @Override
            public void onRenameFolder(RepositoryInfo file, String newName) {
                // 필요하다면 이름 변경 처리
            }
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        // 검색창 연결
        RepositorySearchHelper.setupSearch(searchBar, folderList, adapter);

        // 폴더 불러오기
        RepositoryController.loadFolders(user, folderList, adapter);

        RepositorySearchHelper.setupSearch(searchBar, folderList, adapter);

        // Firestore에서 폴더 로드
        RepositoryController.loadFolders(user, folderList, adapter);
    }

    public void updateUserName(String userName) {
        this.userName = userName;
        ((TextView) findViewById(R.id.main_act_profile_name)).setText(userName);
    }

    private void showAddFolderDialog() {
        RepositoryController.showAddFolderDialog(this, folderList, adapter);
    }

    private void deleteFolder(RepositoryInfo file) {
        RepositoryController.deleteFolder(this, file, folderList, adapter);

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
                    obj.put("title", info.getname());
                    obj.put("date", info.getlastModified());
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
    @Override
    public void onBackPressed() {
        // 뒤로가기 눌렀을 때: 검색창이 열려있으면 닫기, 아니면 기본 동작
        if (!RepositorySearchHelper.handleBackPress(searchBar, folderList, adapter)) {
            super.onBackPressed();
        }
    }
}
