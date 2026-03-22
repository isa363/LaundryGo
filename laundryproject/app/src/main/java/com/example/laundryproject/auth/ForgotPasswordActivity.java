package com.example.laundryproject.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.util.ValidationUtils;

// This class handles the Forgot Password page.
// User enters email and Firebase sends a reset link.

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button sendButton;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        authManager = new AuthManager();

        emailEditText = findViewById(R.id.forgot_emailEditText);
        sendButton = findViewById(R.id.forgot_sendButton);

        sendButton.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = emailEditText.getText().toString().trim();

        if (!ValidationUtils.isEmailValid(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        authManager.sendPasswordResetEmail(email, new AuthManager.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                finish(); // go back to login after sending
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}