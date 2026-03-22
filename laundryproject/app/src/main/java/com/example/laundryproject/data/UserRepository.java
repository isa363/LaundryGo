package com.example.laundryproject.data;

import androidx.annotation.NonNull;

import com.example.laundryproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    private final FirebaseFirestore db;
    private final String COLLECTION_NAME = "users";

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface LoadUserCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface ApartmentCheckCallback {
        void onResult(boolean exists);
        void onFailure(String errorMessage);
    }

    public interface LoadUsersCallback {
        void onSuccess(List<UserWithId> users);
        void onFailure(String errorMessage);
    }

    public static class UserWithId {
        public String uid;
        public User user;

        public UserWithId(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }
    }

    public void saveUser(String uid, User user, @NonNull FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getUser(String uid, @NonNull LoadUserCallback callback) {
        db.collection(COLLECTION_NAME)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void checkApartmentExists(String aptNumber, @NonNull ApartmentCheckCallback callback) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("aptNumber", aptNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        callback.onResult(!queryDocumentSnapshots.isEmpty()))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getAllUsers(@NonNull LoadUsersCallback callback) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserWithId> users = new ArrayList<>();

                    queryDocumentSnapshots.getDocuments().forEach(document -> {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            users.add(new UserWithId(document.getId(), user));
                        }
                    });

                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateUserField(String uid, String field, Object value, @NonNull FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .document(uid)
                .update(field, value)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateUser(String uid, User user, @NonNull FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteUserDocument(String uid, @NonNull FirestoreCallback callback) {
        db.collection(COLLECTION_NAME)
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}