package com.example.opensource.auth;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileManager {
    public static void getUserName(UsernameCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String username = document.getString("username");
                    callback.onUsernameReceived(username);
                } else {
                    Log.d("LoginActivity", "No such document");
                    callback.onUsernameReceived(null); // 없을 때는 null 반환
                }
            } else {
                Log.d("LoginActivity", "get failed with ", task.getException());
                callback.onUsernameReceived(null); // 실패 시에도 null 반환
            }
        });
    }

    public interface UsernameCallback {
        void onUsernameReceived(String username);
    }

    public static void createUserProfile(String username, String email) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("createdBy", uid);

        firestore.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(aVoid ->
                        Log.d("RegisterActivity", "Firestore에 사용자 정보 저장 성공 (UID: " + uid + ")")
                )
                .addOnFailureListener(e ->
                        Log.e("RegisterActivity", "Firestore 저장 실패", e)
                );
    }

    public static void updateUserName(String newUsername, UpdateUsernameCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);

        firestore.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                            Log.d("RegisterActivity", "Firestore에 사용자 정보 업데이트 성공 (UID: " + uid + ")");
                            callback.onUsernameUpdated(newUsername);
                        }
                )
                .addOnFailureListener(e -> {
                            Log.e("RegisterActivity", "Firestore 업데이트 실패", e);
                            callback.onUsernameUpdated(null);
                        }
                );
    }


    public interface UpdateUsernameCallback {
        void onUsernameUpdated(String username);
    }

}
