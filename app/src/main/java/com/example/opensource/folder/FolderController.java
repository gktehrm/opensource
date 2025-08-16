package com.example.opensource.folder;

import android.content.Context;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import android.text.InputType;

import com.example.opensource.RepositoryListAdapter;
import com.example.opensource.firebase.FolderManager;
import com.example.opensource.repository.RepositoryInfo;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * 폴더 추가/삭제/불러오기 컨트롤러
 */
public class FolderController {

    // 🔹 폴더 추가
    public static void showAddFolderDialog(Context context, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("폴더 이름을 입력하세요");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String folderName = input.getText().toString().trim();

            if (!folderName.isEmpty()) {
                FolderRepository.saveFolder(context, folderName, newFolder -> {
                    fileList.add(newFolder);
                    adapter.submitList(FolderFilter.filter(fileList, "")); // 전체 목록 갱신
                }, e -> Log.e("FolderController", "폴더 저장 실패", e));
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // 🔹 폴더 삭제
    public static void deleteFolder(Context context, RepositoryInfo file, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        FolderManager.deleteFolder(file.getId(), new FolderManager.OnFolderActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(context, "삭제 성공", Toast.LENGTH_SHORT).show();
                fileList.removeIf(f -> f.getId().equals(file.getId()));
                adapter.submitList(FolderFilter.filter(fileList, ""));
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 폴더 불러오기 (Firestore → fileList에 저장 후 adapter 갱신)
    public static void loadFolders(FirebaseUser user, List<RepositoryInfo> fileList, RepositoryListAdapter adapter) {
        FolderRepository.loadFolders(user, folders -> {
            fileList.clear();
            fileList.addAll(folders);
            adapter.submitList(FolderFilter.filter(fileList, "")); // 전체 목록 표시
        }, e -> Log.e("FolderController", "폴더 불러오기 실패", e));
    }
}
