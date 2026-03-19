package com.example.laundryproject.data;

import androidx.annotation.NonNull;

import com.example.laundryproject.model.User;
import com.google.firebase.firestore.FirebaseFirestore;


// This class is important since is allows to retreive user info from the firestore db, the user info is a document with the info we provide in the model class.
// gives us functions that make it easy to interact with the databse
public class UserRepository {

    private final FirebaseFirestore db;

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

    public void saveUser(String userId, User user, @NonNull FirestoreCallback callback) {
        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to save user"
                ));
    }

    public void getUser(String userId, @NonNull LoadUserCallback callback) {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("User data is empty");
                        }
                    } else {
                        callback.onFailure("User document not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to load user"
                ));
    }

    public void checkApartmentExists(String aptNumber, @NonNull ApartmentCheckCallback callback) {
        db.collection("users")
                .whereEqualTo("aptNumber", aptNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onResult(!querySnapshot.isEmpty()))
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to check apartment number"
                ));
    }
}