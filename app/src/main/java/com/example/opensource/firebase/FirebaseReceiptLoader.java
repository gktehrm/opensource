package com.example.opensource.firebase;

import com.example.opensource.receipt.entity.Receipt;
import com.example.opensource.receipt.entity.ReceiptMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FirebaseReceiptLoader {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CompletableFuture<List<Receipt>> loadReceipts(String repoId) {
        CompletableFuture<List<Receipt>> future = new CompletableFuture<>();

        db.collection("users")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("repositories")
                .document(repoId)
                .collection("receipts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Receipt> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Receipt r = ReceiptMapper.fromMap(doc.getData());
                        if (r != null) {
                            r.setId(doc.getId());
                            list.add(r);
                        }
                    }
                    future.complete(list);
                })
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }
}
