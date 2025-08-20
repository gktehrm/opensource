package com.example.opensource.repository;
import android.content.Context;

import com.example.opensource.firebase.RepositoryManager;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Firestore와의 폴더 데이터 통신 담당 클래스
 */
public class FirebaseRepository {

    /** 폴더 불러오기 */
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

     /** 폴더 실시간 리스너 */
    public static void listenFolders(FirebaseUser user,
                                     final Consumer<List<RepositoryInfo>> onSuccess,
                                     final Consumer<Exception> onFailure) {
        RepositoryManager.listenFolders(user, new RepositoryManager.OnFoldersLoadListener() {
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

    /** 폴더 저장 */
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
