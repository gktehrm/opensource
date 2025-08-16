package com.example.opensource.firebase;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.opensource.repository.RepositoryInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FolderManager {

    private static final String TAG = "FolderManager";

    //폴더 저장
    public static void saveFolder(Context context, String folderName, OnFolderSaveListener callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            Log.e(TAG, "로그인한 사용자가 없습니다.");
            Toast.makeText(context, "로그인 후 이용해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault()).format(new Date());

        Map<String, Object> folderData = new HashMap<>();
        folderData.put("name", folderName);
        folderData.put("lastModified", date);
        folderData.put("createdAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("repositories")
                .add(folderData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "폴더 저장 성공: " + documentReference.getId());
                    Toast.makeText(context, "폴더가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "폴더 저장 실패", e);
                    Toast.makeText(context, "폴더 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onFailure(e);
                });
    }

     //폴더 불러오기
    public static void loadFolders(FirebaseUser user, OnFoldersLoadListener callback) {
        if (user == null) {
            if (callback != null) callback.onFailure(new Exception("로그인 필요"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("repositories")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RepositoryInfo> folders = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String lastModified = doc.getString("lastModified");

                        // 🔹 Firestore 문서 ID도 저장해야 수정/삭제 가능
                        RepositoryInfo info = new RepositoryInfo(name, lastModified);
                        info.setId(doc.getId());
                        folders.add(info);
                    }
                    if (callback != null) callback.onSuccess(folders);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e);
                });
    }

    //폴더 이름 변경
    public static void renameFolder(String folderId, String newName, OnFolderActionListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onFailure(new Exception("로그인 필요"));
            return;
        }

        String date = new SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.getDefault()).format(new Date());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("repositories")
                .document(folderId)
                .update("name", newName, "lastModified", date)
                .addOnSuccessListener(aVoid -> {


                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e);
                });
    }

    // 폴더 삭제
    public static void deleteFolder(String folderId, OnFolderActionListener listener) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onFailure(new Exception("로그인 필요"));
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("repositories")
                .document(folderId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e);
                });
    }

    /**
     * 저장 콜백
     */
    public interface OnFolderSaveListener {
        void onSuccess(String folderId);
        void onFailure(Exception e);
    }

    /**
     * 불러오기 콜백
     */
    public interface OnFoldersLoadListener {
        void onSuccess(List<RepositoryInfo> folders);
        void onFailure(Exception e);
    }

    /**
     * 수정/삭제 콜백
     */
    public interface OnFolderActionListener {
        void onSuccess();
        void onFailure(Exception e);
    }
}