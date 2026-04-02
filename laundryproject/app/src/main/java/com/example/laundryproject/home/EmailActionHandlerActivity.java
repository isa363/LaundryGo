package com.example.laundryproject.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.auth.LoginActivity;
import com.example.laundryproject.data.UserRepository;
import com.google.firebase.auth.ActionCodeUrl;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailActionHandlerActivity extends AppCompatActivity {

    private final UserRepository userRepository = new UserRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();
        if (data == null) {
            Toast.makeText(this, "Invalid email action link.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ActionCodeUrl actionCodeUrl = ActionCodeUrl.parseLink(data.toString());
        if (actionCodeUrl == null || actionCodeUrl.getCode() == null) {
            Toast.makeText(this, "Missing action code.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String oobCode = actionCodeUrl.getCode();

        FirebaseAuth.getInstance()
                .applyActionCode(oobCode)
                .addOnSuccessListener(unused -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Email change applied, but no user is signed in.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    user.reload().addOnSuccessListener(v -> {
                        FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (refreshedUser == null || refreshedUser.getEmail() == null) {
                            Toast.makeText(this, "Email updated, but refresh failed.", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        String actualEmail = refreshedUser.getEmail();

                        userRepository.updateUserField(
                                refreshedUser.getUid(),
                                "email",
                                actualEmail,
                                new UserRepository.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(
                                                EmailActionHandlerActivity.this,
                                                "Email updated successfully.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        Intent intent = new Intent(
                                                EmailActionHandlerActivity.this,
                                                ProfileActivity.class
                                        );
                                        intent.putExtra("email_updated", true);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                        startActivity(intent);
                                        finish();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(
                                                EmailActionHandlerActivity.this,
                                                "Auth updated, but database update failed: " + errorMessage,
                                                Toast.LENGTH_LONG
                                        ).show();
                                        finish();
                                    }
                                }
                        );
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                    FirebaseAuth.getInstance().signOut();

                    Toast.makeText(
                            EmailActionHandlerActivity.this,
                            "Email updated successfully. Please sign in again with your new email.",
                            Toast.LENGTH_LONG
                    ).show();

                    Intent intent = new Intent(EmailActionHandlerActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not apply email action: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}