package com.example.laundryproject.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.home.AdminActivity;
import com.example.laundryproject.home.HomeActivity;
import com.example.laundryproject.model.User;
import com.example.laundryproject.util.ValidationUtils;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private TextView toRedirect, forgotPasswordTextEdit;
    private Button loginButton;

    private AuthManager authManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        authManager = new AuthManager();
        userRepository = new UserRepository();

        emailEditText = findViewById(R.id.login_emailEditText);
        passwordEditText = findViewById(R.id.login_passwordEditText);
        loginButton = findViewById(R.id.login_loginButton);
        toRedirect = findViewById(R.id.RegisterRedirectText);
        forgotPasswordTextEdit = findViewById(R.id.forgotPasswordTextEdit);

        toRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> attemptLogin());

        forgotPasswordTextEdit.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            handleLoggedInUser(currentUser);
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
                handleLoggedInUser(user);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLoggedInUser(FirebaseUser user) {
        user.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = authManager.getCurrentUser();

            if (refreshedUser == null) {
                Toast.makeText(LoginActivity.this,
                        "Login failed. Please try again.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!refreshedUser.isEmailVerified()) {
                authManager.signOut();
                Toast.makeText(LoginActivity.this,
                        "Please verify your email before logging in.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            userRepository.getUser(refreshedUser.getUid(), new UserRepository.LoadUserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user == null) {
                        authManager.signOut();
                        Toast.makeText(LoginActivity.this,
                                "User profile not found.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!user.enabled) {
                        authManager.signOut();
                        Toast.makeText(LoginActivity.this,
                                "Your account is not enabled yet. Please contact admin.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    String accountType = user.accountType != null
                            ? user.accountType.trim()
                            : "";

                    Intent intent;

                    if (accountType.equalsIgnoreCase("Admin")) {
                        intent = new Intent(LoginActivity.this, AdminActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, HomeActivity.class);
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    authManager.signOut();
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}