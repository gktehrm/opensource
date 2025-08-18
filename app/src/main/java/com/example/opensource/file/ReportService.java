package com.example.opensource.file;

import com.example.opensource.firebase.FirebaseReceiptLoader;
import com.example.opensource.receipt.entity.Receipt;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReportService {


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
                            reportList.add(report);
                        } catch (Exception e) {
                            callback.onFailure(e);
                            return;
                        }
                    }

                    callback.onSuccess(reportList);
                })
                .addOnFailureListener(callback::onFailure);
    }
}

