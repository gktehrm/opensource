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
import com.example.opensource.repository.RepositoryController;
import com.example.opensource.repository.RepositorySearchHelper;
import com.example.opensource.repository.RepositoryInfo;
import com.example.opensource.menu.MyPageActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

/**
 * 애플리케이션의 메인 화면을 관리하는 액티비티입니다.
 * 이 클래스는 사용자 인증 상태를 확인하고, 저장소 목록을 표시하며,
 * 사용자 프로필 정보 업데이트 및 UI 상호작용을 처리합니다.
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RepositoryListAdapter adapter;
    private List<RepositoryInfo> folderList;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String userName;
    private EditText searchBar;

    /**
     * MyPageActivity로부터 결과를 받아 처리하는 ActivityResultLauncher입니다.
     * 사용자의 닉네임이 업데이트되면, 이를 현재 화면에 반영합니다.
     */
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

    /**
     * 액티비티가 생성될 때 호출됩니다.
     * 사용자 인증 상태를 확인하고, UI를 초기화하며, 필요한 데이터를 로드합니다.
     * @param savedInstanceState 이전에 저장된 액티비티 상태가 있다면 포함됩니다.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    /**
     * 액티비티의 주요 UI 컴포넌트를 초기화하고 이벤트 리스너를 설정합니다.
     * 마이페이지 버튼, 리사이클러뷰, 검색창 등을 설정하고,
     * 저장소 데이터를 불러와 화면에 표시합니다.
     */
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
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        List<RepositoryListAdapter.FolderListItem> initList = new ArrayList<>();
        initList.add(new RepositoryListAdapter.FolderListItem.Add());
        adapter.submitList(initList);

        RepositorySearchHelper.setupSearch(searchBar, folderList, adapter);

        RepositoryController.listenFolders(user, folderList, adapter);
    }

    /**
     * 사용자의 이름을 UI에 업데이트합니다.
     * @param userName 업데이트할 사용자의 새 이름입니다.
     */
    public void updateUserName(String userName) {
        this.userName = userName;
        ((TextView) findViewById(R.id.main_act_profile_name)).setText(userName);
    }

    /**
     * 새 폴더를 추가하는 다이얼로그를 표시합니다.
     */
    private void showAddFolderDialog() {
        RepositoryController.showAddFolderDialog(this, user);
    }

    /**
     * 지정된 폴더를 삭제합니다.
     * @param file 삭제할 폴더의 정보를 담고 있는 RepositoryInfo 객체입니다.
     */
    private void deleteFolder(RepositoryInfo file) {
        RepositoryController.deleteFolder(this, file);
    }

    /**
     * 뒤로 가기 버튼이 눌렸을 때의 동작을 처리합니다.
     * 검색창이 활성화되어 있으면 검색 상태를 초기화하고,
     * 그렇지 않으면 기본 뒤로 가기 동작을 수행합니다.
     */
    @Override
    public void onBackPressed() {
        if (!RepositorySearchHelper.handleBackPress(searchBar, folderList, adapter)) {
            super.onBackPressed();
        }
    }
}
