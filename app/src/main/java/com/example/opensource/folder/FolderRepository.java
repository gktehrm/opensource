package com.example.opensource.folder;
//firestore 로드/저장 담당
import android.content.Context;

import com.example.opensource.firebase.FolderManager;
import com.example.opensource.repository.RepositoryInfo;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FolderRepository {

    public interface LoadCallback {
        void onSuccess(List<RepositoryInfo> folders);
        void onFailure(Exception e);
    }

    public interface SaveCallback {
        void onSuccess(RepositoryInfo newFolder);
        void onFailure(Exception e);
    }

    public static void loadFolders(FirebaseUser user,
                                   final java.util.function.Consumer<List<RepositoryInfo>> onSuccess,
                                   final java.util.function.Consumer<Exception> onFailure) {
        FolderManager.loadFolders(user, new FolderManager.OnFoldersLoadListener() {
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
        FolderManager.saveFolder(context, folderName, new FolderManager.OnFolderSaveListener() {
            @Override
            public void onSuccess(String folderId) {
                String date = new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault()).format(new Date());
                RepositoryInfo newFolder = new RepositoryInfo(folderName, date);
                onSuccess.accept(newFolder);
            }
            @Override
            public void onFailure(Exception e) {
                onFailure.accept(e);
            }
        });
    }
}
