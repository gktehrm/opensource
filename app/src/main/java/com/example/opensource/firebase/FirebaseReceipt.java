package com.example.opensource.firebase;

import android.net.Uri;
import android.util.Log;

import com.example.opensource.receipt.entity.Receipt;
import com.example.opensource.receipt.entity.ReceiptMapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;

import java.util.HashMap;
import java.util.Map;

// 문서: users/{uid}/repositories/{repoId}/receipts/{receiptId}
// 이미지: users/{uid}/repositories/{repoId}/receipts/{receiptId}.jpg
public class FirebaseReceipt {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private String uidOrThrow() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) throw new IllegalStateException("User not logged in");
        return u.getUid();
    }

    /** 영수증 저장/수정 (업서트 방식) */
    public void upsertReceipt(String repoId,
                              Receipt receipt,
                              Uri imageUriOrNull,
                              OnCompleteListener<DocumentReference> listener) {

        String uid = uidOrThrow();

        // 1. 문서 참조 (id 있으면 그대로, 없으면 새로 생성)
        CollectionReference colRef = db.collection("users")
                .document(uid)
                .collection("repositories")
                .document(repoId)
                .collection("receipts");

        DocumentReference docRef;
        if (receipt.getId() == null || receipt.getId().isEmpty()) {
            docRef = colRef.document();                // 새 문서
            receipt.setId(docRef.getId());
        } else {
            docRef = colRef.document(receipt.getId()); // 기존 문서
        }

        // 데이터 변환
        Map<String, Object> data = ReceiptMapper.toMap(receipt);
        data.put("id", receipt.getId());

        // 이미지 없는 경우 → imageUrl 건드리지 않음
        if (imageUriOrNull == null) {
            docRef.set(data, SetOptions.merge())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateRepoLastModified(uid, repoId);
                            Log.d("FirebaseReceipt", "영수증 저장 완료 (이미지 없음)");
                            listener.onComplete(Tasks.forResult(docRef));
                        } else {
                            Log.e("FirebaseReceipt", "영수증 저장 실패", task.getException());
                            listener.onComplete(Tasks.forException(task.getException()));
                        }
                    });
            return;
        }

        // 4. 이미지 있는 경우 → Storage 업로드 후 URL 갱신
        StorageReference imgRef = storage.getReference()
                .child("users/" + uid + "/repositories/" + repoId + "/receipts/" + receipt.getId() + ".jpg");

        imgRef.putFile(imageUriOrNull)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    return imgRef.getDownloadUrl();
                })
                .addOnCompleteListener(urlTask -> {
                    if (!urlTask.isSuccessful()) {
                        listener.onComplete(Tasks.forException(urlTask.getException()));
                        return;
                    }

                    String downloadUrl = urlTask.getResult().toString();
                    receipt.setImageUrl(downloadUrl); // 객체에도 반영
                    data.put("imageUrl", downloadUrl);

                    docRef.set(data, SetOptions.merge())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    updateRepoLastModified(uid, repoId);
                                    Log.d("FirebaseReceipt", "영수증 + 이미지 저장 완료");
                                    listener.onComplete(Tasks.forResult(docRef));
                                } else {
                                    Log.e("FirebaseReceipt", "영수증 + 이미지 저장 실패", task.getException());
                                    listener.onComplete(Tasks.forException(task.getException()));
                                }
                            });
                });
    }

    /** 영수증 목록 불러오기 */
    public void loadReceipts(String repoId,
                             OnCompleteListener<QuerySnapshot> listener) {
        String uid = uidOrThrow();

        db.collection("users")
                .document(uid)
                .collection("repositories")
                .document(repoId)
                .collection("receipts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            Receipt r = doc.toObject(Receipt.class);
                            if (r != null) {
                                r.setId(doc.getId()); // 문서 ID 저장
                            }
                        }
                    }
                    listener.onComplete(task);
                });
    }
    private void updateRepoLastModified(String uid, String repoId) {
        String date = new java.text.SimpleDateFormat("yyyy.MM.dd a h:mm",
                java.util.Locale.getDefault()).format(new java.util.Date());

        Map<String, Object> update = new HashMap<>();
        update.put("lastModified", date);

        db.collection("users")
                .document(uid)
                .collection("repositories")
                .document(repoId)
                .set(update, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("FirebaseReceipt", "lastModified 갱신 완료: " + date);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseReceipt", "lastModified 갱신 실패", e);
                });
    }
}
