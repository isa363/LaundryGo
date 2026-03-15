package com.example.laundryproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private EditText email, password, aptNumber;
    private RadioGroup accountType;
    private Button registerButton;
    private TextView toLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_page);

        // Init Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Init UI components
        email = findViewById(R.id.register_usernameEditText);
        password = findViewById(R.id.register_passwordEditText);
        aptNumber = findViewById(R.id.register_aptNumberEditText);
        accountType = findViewById(R.id.register_accountTypeRadioGroup);
        registerButton = findViewById(R.id.register_registerButton);
        toLogin = findViewById(R.id.loginRedirectText);

        toLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        registerButton.setOnClickListener(v -> {
            String em = email.getText().toString().trim();
            String pw = password.getText().toString().trim();
            String apt = aptNumber.getText().toString().trim();

            int selectedId = accountType.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = findViewById(selectedId);
            String type = (selectedRadioButton != null) ? selectedRadioButton.getText().toString() : "Regular";

            if (em.isEmpty() || pw.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(em, pw, apt, type);
        });
    }

    private void registerUser(String email, String password, String aptNumber, String accountType) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            writeNewUser(user.getUid(), email, password, aptNumber, accountType);
                        }
                    }
                    else {
                        Toast.makeText(RegisterActivity.this, "Auth Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void writeNewUser(String userId, String email, String password, String aptNumber, String accountType) {
        User user = new User(email, password, aptNumber, false, accountType);

        // write info to db
        db.collection("users").document(userId).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                })

                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
