//package com.example.opensource.firebase;
//import android.net.Uri;
//import android.util.Log;
//// 파이어베이스에 영수증 정보 + 사진 저장
////Firestore: 텍스트 데이터 (가맹점명, 날짜, 금액, 이미지 URL)
////Storage: 실제 이미지 파일
////둘을 연결하는 고리는 imageUrl (Storage 다운로드 URL)
//
//import androidx.annotation.NonNull;
//
//import com.example.opensource.receipt.entity.Receipt;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class FirebaseReceipt {
//    private final FirebaseFirestore db;
//    private final FirebaseStorage storage;
//
//    public FirebaseReceipt() {
//        db = FirebaseFirestore.getInstance();
//        storage = FirebaseStorage.getInstance();
//    }
//
//    /**
//     * 영수증 + 사진 저장
//     *
//     * @param folderId  저장할 폴더의 Firestore 문서 ID
//     * @param receipt   저장할 영수증 객체
//     * @param imageUri  로컬 영수증 사진 Uri (없으면 null)
//     */
//    public void saveReceipt(String folderId, Receipt receipt, Uri imageUri) {
//        // receipts 하위 컬렉션 안에 새 문서 생성
//        String receiptId = db.collection("repositories")
//                .document(folderId)
//                .collection("receipts")
//                .document()
//                .getId();
//
//        Map<String, Object> data = new HashMap<>();
//        data.put("storeName", receipt.getStoreName());
//        data.put("timestamp", receipt.getTimestamp());
//        data.put("receiptTotal", receipt.getReceiptTotal());
//
//        if (imageUri != null) {
//            // Storage 경로: repositories/{folderId}/receipts/{receiptId}.jpg
//            StorageReference ref = storage.getReference()
//                    .child("repositories/" + folderId + "/receipts/" + receiptId + ".jpg");
//
//            ref.putFile(imageUri)
//                    .addOnSuccessListener(task ->
//                            ref.getDownloadUrl().addOnSuccessListener(uri -> {
//                                data.put("imageUrl", uri.toString());
//
//                                db.collection("repositories")
//                                        .document(folderId)
//                                        .collection("receipts")
//                                        .document(receiptId)
//                                        .set(data)
//                                        .addOnSuccessListener(aVoid ->
//                                                Log.d("FirebaseReceipt", "영수증 저장 성공"))
//                                        .addOnFailureListener(e ->
//                                                Log.e("FirebaseReceipt", "영수증 저장 실패", e));
//                            })
//                    )
//                    .addOnFailureListener(e ->
//                            Log.e("FirebaseReceipt", "이미지 업로드 실패", e));
//
//        } else {
//            // 이미지 없이 바로 저장
//            db.collection("repositories")
//                    .document(folderId)
//                    .collection("receipts")
//                    .document(receiptId)
//                    .set(data)
//                    .addOnSuccessListener(aVoid ->
//                            Log.d("FirebaseReceipt", "영수증 저장 성공"))
//                    .addOnFailureListener(e ->
//                            Log.e("FirebaseReceipt", "영수증 저장 실패", e));
//        }
//    }
//
//    /**
//     * 폴더 안 모든 영수증 불러오기
//     *
//     * @param folderId  불러올 폴더 ID
//     * @param listener  불러오기 완료 후 콜백
//     */
//    public void loadReceipts(String folderId, OnReceiptsLoadedListener listener) {
//        db.collection("repositories")
//                .document(folderId)
//                .collection("receipts")
//                .get()
//                .addOnSuccessListener(query -> {
//                    List<Receipt> receipts = new ArrayList<>();
//                    for (DocumentSnapshot doc : query.getDocuments()) {
//                        Receipt r = new Receipt();
//                        r.setStoreName(doc.getString("storeName"));
//                        r.setTimestamp(doc.getString("timestamp"));
//
//                        if (doc.getLong("receiptTotal") != null) {
//                            r.setReceiptTotal(doc.getLong("receiptTotal").intValue());
//                        }
//
//                        if (doc.contains("imageUrl")) {
//                            r.setImageUrl(doc.getString("imageUrl"));
//                        }
//
//                        receipts.add(r);
//                    }
//                    listener.onLoaded(receipts);
//                })
//                .addOnFailureListener(e -> {
//                    Log.e("FirebaseReceipt", "영수증 불러오기 실패", e);
//                    listener.onLoaded(new ArrayList<>()); // 실패 시 빈 리스트 반환
//                });
//    }
//
//    // 불러오기 콜백 인터페이스
//    public interface OnReceiptsLoadedListener {
//        void onLoaded(List<Receipt> receipts);
//    }
//}
