package com.example.opensource.file;

import com.example.opensource.receipt.entity.Receipt;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private static final String TAG = "ReportService";

    public void loadReportData(String repositoryId, ReportDataCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("repositories")
                .document(repositoryId)
                .collection("receipts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ReportData> reportList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        try {
                            Receipt receipt = doc.toObject(Receipt.class);
                            ReportData report = ReportDataMapper.map(receipt);
                            if (report != null) {
                                reportList.add(report);
                            }
                        } catch (Exception e) {
                            // 일부 변환 실패는 무시하고 로그만 출력
                            Log.w(TAG, "Receipt 변환 실패: " + doc.getId(), e);
                        }
                    }
                    callback.onSuccess(reportList);
                })
                .addOnFailureListener(callback::onFailure);
    }
}
