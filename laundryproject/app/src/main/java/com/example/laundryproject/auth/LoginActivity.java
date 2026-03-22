package com.example.laundryproject.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.home.MainActivity;
import com.example.laundryproject.util.ValidationUtils;
import com.google.firebase.auth.FirebaseUser;

// This class handles the Login page.
// Handles verifying if user's email is verified and redirecting to main page.
// Also allows navigation to register page and forgot password page.

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private TextView toRedirect, forgotPasswordText;
    private Button loginButton;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        authManager = new AuthManager();

        emailEditText = findViewById(R.id.login_emailEditText);
        passwordEditText = findViewById(R.id.login_passwordEditText);
        loginButton = findViewById(R.id.login_loginButton);
        toRedirect = findViewById(R.id.RegisterRedirectText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        toRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPasswordText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                FirebaseUser refreshedUser = authManager.getCurrentUser();
                if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            });
        }
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!ValidationUtils.isEmailValid(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isPasswordValid(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        authManager.loginUser(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                user.reload().addOnCompleteListener(task -> {
                    FirebaseUser refreshedUser = authManager.getCurrentUser();

                    if (refreshedUser != null && refreshedUser.isEmailVerified()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        authManager.signOut();
                        Toast.makeText(LoginActivity.this,
                                "Please verify your email before logging in.",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}