package com.example.opensource;

import android.content.Intent;
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
import com.example.opensource.folder.FolderController;
import com.example.opensource.folder.SearchHelper;
import com.example.opensource.menu.MyPageActivity;
import com.example.opensource.folder.folderInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RepositoryListAdapter adapter;
    private List<folderInfo> fileList;   // Firestore 원본 데이터 저장

    private FirebaseAuth auth;
    private FirebaseUser user;
    private String userName;
    private EditText searchBar;  // 전역으로 선언

    private SearchHelper searchHelper;
    private FolderController folderController;

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

        // 로그인 확인
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
    }

    private void init() {
        ImageButton myPageButton = findViewById(R.id.btnMypage);
        myPageButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, MyPageActivity.class);
            intent.putExtra("nickname", userName);
            myPageLauncher.launch(intent);
        });

        recyclerView = findViewById(R.id.recyclerView);
        searchBar = findViewById(R.id.searchBar);

        fileList = new ArrayList<>();

        adapter = new RepositoryListAdapter(new RepositoryListAdapter.FolderActionListener() {
            @Override
            public void onAddFolder() {
                showAddFolderDialog();
            }

            @Override
            public void onDeleteFolder(folderInfo file) {
                deleteFolder(file);
            }

            @Override
            public void onRenameFolder(folderInfo file, String newName) {
                // 필요하다면 이름 변경 처리
            }
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // 검색창 연결
        SearchHelper.setupSearch(searchBar, fileList, adapter);

        // 폴더 불러오기
        FolderController.loadFolders(user, fileList, adapter);

        SearchHelper.setupSearch(searchBar, fileList, adapter);

        // Firestore에서 폴더 로드
        FolderController.loadFolders(user, fileList, adapter);
    }

    public void updateUserName(String userName) {
        this.userName = userName;
        ((TextView) findViewById(R.id.main_act_profile_name)).setText(userName);
    }

    private void showAddFolderDialog() {
        FolderController.showAddFolderDialog(this, fileList, adapter);
    }

    private void deleteFolder(folderInfo file) {
        FolderController.deleteFolder(this, file, fileList, adapter);

    }

    @Override
    public void onBackPressed() {
        if (!SearchHelper.handleBackPress(searchBar, fileList, adapter)) {
            super.onBackPressed();
        }
    }
}
