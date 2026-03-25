package com.example.laundryproject.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.example.laundryproject.util.ValidationUtils;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private static final String ADMIN_KEY = "ADMIN123";

    private EditText emailEditText, passwordEditText, aptNumberEditText, adminKeyEditText;
    private Button registerButton;
    private TextView toLogin;
    private RadioGroup accountTypeGroup;
    private TextInputLayout adminKeyInputLayout;
    private TextInputLayout aptNumberInputLayout;

    private AuthManager authManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        authManager = new AuthManager();
        userRepository = new UserRepository();

        emailEditText = findViewById(R.id.register_emailEditText);
        passwordEditText = findViewById(R.id.register_passwordEditText);
        aptNumberEditText = findViewById(R.id.register_aptNumberEditText);
        adminKeyEditText = findViewById(R.id.register_adminKeyEditText);

        adminKeyInputLayout = findViewById(R.id.register_adminKeyInputLayout);
        aptNumberInputLayout = findViewById(R.id.register_aptNumberInputLayout);

        accountTypeGroup = findViewById(R.id.register_accountTypeRadioGroup);
        registerButton = findViewById(R.id.register_registerButton);
        toLogin = findViewById(R.id.loginRedirectText);

        // Default state = Regular selected
        adminKeyInputLayout.setVisibility(View.GONE);
        aptNumberInputLayout.setVisibility(View.VISIBLE);

        accountTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.register_adminRadioButton) {
                adminKeyInputLayout.setVisibility(View.VISIBLE);
                aptNumberInputLayout.setVisibility(View.GONE);
                aptNumberEditText.setText("");
            } else {
                adminKeyInputLayout.setVisibility(View.GONE);
                adminKeyEditText.setText("");
                aptNumberInputLayout.setVisibility(View.VISIBLE);
            }
        });

        toLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        registerButton.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String aptNumber = aptNumberEditText.getText().toString().trim();
        String adminKey = adminKeyEditText.getText().toString().trim();

        int selectedId = accountTypeGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedId);

        String accountType = selectedRadioButton != null
                ? selectedRadioButton.getText().toString()
                : "Regular";

        if (!ValidationUtils.isEmailValid(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isPasswordValid(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (accountType.equals("Regular") && !ValidationUtils.isNotEmpty(aptNumber)) {
            Toast.makeText(this, "Apartment number is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (accountType.equals("Admin") && !adminKey.equals(ADMIN_KEY)) {
            Toast.makeText(this, "Invalid admin key", Toast.LENGTH_SHORT).show();
            return;
        }

        if (accountType.equals("Admin")) {
            registerUser(email, password, "", accountType);
        } else {
            checkApartmentAndRegister(email, password, aptNumber, accountType);
        }
    }

    private void checkApartmentAndRegister(String email, String password, String aptNumber, String accountType) {
        userRepository.checkApartmentExists(aptNumber, new UserRepository.ApartmentCheckCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    Toast.makeText(RegisterActivity.this,
                            "An account already exists for this apartment number",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                registerUser(email, password, aptNumber, accountType);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(RegisterActivity.this,
                        "Could not verify apartment number: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String email, String password, String aptNumber, String accountType) {
        authManager.registerUser(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {
                User user = new User(email, aptNumber, true, accountType);

                userRepository.saveUser(firebaseUser.getUid(), user, new UserRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this,
                                                "Registration successful. Verification email sent.",
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(RegisterActivity.this,
                                                "Registration successful, but verification email could not be sent.",
                                                Toast.LENGTH_LONG).show();
                                    }

                                    authManager.signOut();
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                    finish();
                                });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(RegisterActivity.this,
                                "Firestore error: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}