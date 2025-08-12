package com.example.opensource;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FileStorage {

    public void uploadDocument(String filePath) {
        // 로그인 확인
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("Firebase", "로그인 필요: currentUser == null");
            return;
        }
        String uid = user.getUid();

        // Storage 참조
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // 업로드 경로: users/{uid}/documents/{파일명}
        File file = new File(filePath);
        Uri fileUri = Uri.fromFile(file);
        String fileName = file.getName();
        StorageReference docRef =
                storageRef.child("users/" + uid + "/documents/" + fileName);

        // 파일 업로드
        docRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 업로드 성공 → 다운로드 URL
                    docRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String downloadUrl = uri.toString();

                                // Firestore에 메타데이터 저장
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                Map<String, Object> docInfo = new HashMap<>();
                                docInfo.put("fileName", fileName);
                                docInfo.put("url", downloadUrl);
                                docInfo.put("uploadedAt", new Date());

                                db.collection("users")
                                        .document(uid)
                                        .collection("documents")
                                        .add(docInfo)
                                        .addOnSuccessListener(ref ->
                                                Log.d("Firebase", "Firestore 저장 완료: " + ref.getId()))
                                        .addOnFailureListener(e ->
                                                Log.e("Firebase", "Firestore 저장 실패", e));
                            })
                            .addOnFailureListener(e ->
                                    Log.e("Firebase", "다운로드 URL 획득 실패", e));
                })
                .addOnFailureListener(e ->
                        Log.e("Firebase", "업로드 실패", e));
    }
}
