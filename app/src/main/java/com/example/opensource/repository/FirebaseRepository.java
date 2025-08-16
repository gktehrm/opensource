package com.example.opensource.repository;
//firestore 파일 가져오기 /저장 담당
import android.content.Context;

import com.example.opensource.firebase.RepositoryManager;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseRepository {

    public static void loadFolders(FirebaseUser user,
                                   final java.util.function.Consumer<List<RepositoryInfo>> onSuccess,
                                   final java.util.function.Consumer<Exception> onFailure) {
        RepositoryManager.loadFolders(user, new RepositoryManager.OnFoldersLoadListener() {
            @Override
            public void onSuccess(List<RepositoryInfo> folders) {
                onSuccess.accept(folders);
            }
            @Override
            public void onFailure(Exception e) {
                onFailure.accept(e);
            }
        });
    }

    public static void saveFolder(Context context, String folderName,
                                  final java.util.function.Consumer<RepositoryInfo> onSuccess,
                                  final java.util.function.Consumer<Exception> onFailure) {
        RepositoryManager.saveFolder(context, folderName, new RepositoryManager.OnFolderSaveListener() {
            @Override
            public void onSuccess(String folderId) {
                String date = new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault()).format(new Date());
                RepositoryInfo newFolder = new RepositoryInfo(folderId,folderName, date);
                onSuccess.accept(newFolder);
            }
            @Override
            public void onFailure(Exception e) {
                onFailure.accept(e);
            }
        });
    }
}
