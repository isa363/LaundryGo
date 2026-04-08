package com.example.laundryproject;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FcmTokenHelper {

    private static final String TAG = "FcmTokenHelper";

    public static void syncCurrentUserToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(FcmTokenHelper::saveTokenForCurrentUser)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to fetch FCM token", e));
    }

    public static void saveTokenForCurrentUser(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || token == null || token.trim().isEmpty()) {
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);

        db.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "FCM token saved successfully"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to save FCM token", e));
    }

    public static void clearCurrentUserToken() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .update("fcmToken", null);
    }
}