package com.example.laundryproject.auth;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// This class allows us to call back functions from the FirebaseAuth class that handle loginUser,registerUser.
// This class can also be used to find current user and also log out. Separating Firebase commands with the rest of the code.
public class AuthManager {

    private final FirebaseAuth mAuth;

    public AuthManager() {
        mAuth = FirebaseAuth.getInstance();
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }

    public void loginUser(String email, String password, @NonNull AuthCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        String message = "Authentication failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        callback.onFailure(message);
                    }
                });
    }

    public void registerUser(String email, String password, @NonNull AuthCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        String message = "Registration failed";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        callback.onFailure(message);
                    }
                });
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void signOut() {
        mAuth.signOut();
    }
}