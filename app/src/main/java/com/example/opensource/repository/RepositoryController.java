package com.example.opensource.repository;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import android.text.InputType;

import com.example.opensource.RepositoryListAdapter;
import com.example.opensource.firebase.RepositoryManager;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

// 폴더 추가/삭제/불러오기 컨트롤러
public class RepositoryController {

    // ✅ 폴더 추가: Firestore에만 작성, 로컬 리스트/어댑터는 건드리지 않음
    public static void showAddFolderDialog(Context context, FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("폴더 이름을 입력하세요");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!folderName.isEmpty()) {
                FirebaseRepository.saveFolder(context, folderName, newFolder -> {
                    Toast.makeText(context, "폴더가 생성되었습니다.", Toast.LENGTH_SHORT).show();
                    // 화면 갱신은 listenFolders가 처리
                }, e -> {
                    Log.e("RepositoryController", "폴더 저장 실패", e);
                    Toast.makeText(context, "저장 실패", Toast.LENGTH_SHORT).show();
                });
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ✅ 폴더 삭제: Firestore에서만 삭제, 로컬 리스트/어댑터는 건드리지 않음
    public static void deleteFolder(Context context, RepositoryInfo file) {
        RepositoryManager.deleteFolder(file.getId(), new RepositoryManager.OnFolderActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(context, "삭제 성공", Toast.LENGTH_SHORT).show();
                // 화면 갱신은 listenFolders가 처리
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 폴더 불러오기 (단발성) - 필요 시 사용 가능
    public static void loadFolders(FirebaseUser user, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        FirebaseRepository.loadFolders(user, folders -> {
            fileList.clear();
            fileList.addAll(folders);
            submitFolderList(fileList, adapter);
        }, e -> Log.e("RepositoryController", "폴더 불러오기 실패", e));
    }

    // ✅ 폴더 실시간 리스너 (자동 UI 갱신 전담)
    public static void listenFolders(FirebaseUser user, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        if (user == null) return;

        FirebaseRepository.listenFolders(user, folders -> {
            fileList.clear();
            fileList.addAll(folders);
            submitFolderList(fileList, adapter);
        }, e -> Log.e("RepositoryController", "폴더 실시간 불러오기 실패", e));
    }

    // 공통: RepositoryInfo 리스트 → FolderListItem 리스트로 변환 후 submit
    private static void submitFolderList(List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        List<RepositoryListAdapter.FolderListItem> newList = new ArrayList<>();
        // 항상 Add 버튼 먼저
        newList.add(new RepositoryListAdapter.FolderListItem.Add());
        // 나머지 폴더들 추가
        for (RepositoryInfo repo : fileList) {
            newList.add(new RepositoryListAdapter.FolderListItem.Row(repo));
        }
        adapter.submitList(newList);
    }
}
