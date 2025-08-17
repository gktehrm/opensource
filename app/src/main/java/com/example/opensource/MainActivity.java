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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RepositoryListAdapter adapter;
    private List<RepositoryInfo> folderList;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String userName;
    private EditText searchBar;  // 전역으로 선언

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
        folderList = new ArrayList<>();

        // position 0번에 항상 플러스 카드가 고정되도록 null 추가
        folderList.add(null);

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
        //  초기 화면에 "Add 버튼"만 세팅
        List<RepositoryListAdapter.FolderListItem> initList = new ArrayList<>();
        initList.add(new RepositoryListAdapter.FolderListItem.Add());
        adapter.submitList(initList);

        // 검색창 연결
        RepositorySearchHelper.setupSearch(searchBar, folderList, adapter);

        // Firestore에서 폴더 불러오기
        RepositoryController.listenFolders(user, folderList, adapter);
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
    public void onBackPressed() {
        // 뒤로가기 눌렀을 때: 검색창이 열려있으면 닫기, 아니면 기본 동작
        if (!RepositorySearchHelper.handleBackPress(searchBar, folderList, adapter)) {
            super.onBackPressed();
        }
    }
}
