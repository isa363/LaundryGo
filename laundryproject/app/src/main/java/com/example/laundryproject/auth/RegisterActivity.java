package com.example.laundryproject.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private static final String ADMIN_KEY = "ADMIN123";

    private EditText emailEditText, passwordEditText, aptNumberEditText, adminKeyEditText, buildingCodeEditText;
    private AutoCompleteTextView buildingSelector;
    private Button registerButton;
    private TextView toLogin;
    private RadioGroup accountTypeGroup;

    private TextInputLayout adminKeyInputLayout;
    private TextInputLayout aptNumberInputLayout;
    private TextInputLayout buildingCodeInputLayout;
    private TextInputLayout buildingSelectorLayout;

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
        buildingCodeEditText = findViewById(R.id.register_buildingCodeEditText);

        buildingSelector = findViewById(R.id.register_buildingSelector);

        adminKeyInputLayout = findViewById(R.id.register_adminKeyInputLayout);
        aptNumberInputLayout = findViewById(R.id.register_aptNumberInputLayout);
        buildingCodeInputLayout = findViewById(R.id.register_buildingCodeInputLayout);
        buildingSelectorLayout = findViewById(R.id.register_buildingSelectorLayout);

        accountTypeGroup = findViewById(R.id.register_accountTypeRadioGroup);
        registerButton = findViewById(R.id.register_registerButton);
        toLogin = findViewById(R.id.loginRedirectText);

        // Default state = Regular user
        adminKeyInputLayout.setVisibility(View.GONE);
        aptNumberInputLayout.setVisibility(View.VISIBLE);
        buildingCodeInputLayout.setVisibility(View.VISIBLE);
        buildingSelectorLayout.setVisibility(View.VISIBLE);

        loadBuildingNames();

        accountTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.register_adminRadioButton) {
                adminKeyInputLayout.setVisibility(View.VISIBLE);
                aptNumberInputLayout.setVisibility(View.GONE);
                buildingCodeInputLayout.setVisibility(View.GONE);

                aptNumberEditText.setText("");
                buildingCodeEditText.setText("");
            } else {
                adminKeyInputLayout.setVisibility(View.GONE);
                adminKeyEditText.setText("");

                aptNumberInputLayout.setVisibility(View.VISIBLE);
                buildingCodeInputLayout.setVisibility(View.VISIBLE);
            }
        });

        toLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        registerButton.setOnClickListener(v -> attemptRegister());
    }

    private void loadBuildingNames() {
        userRepository.getAllBuildings(new UserRepository.BuildingListCallback() {
            @Override
            public void onSuccess(List<String> buildingNames) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        RegisterActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        buildingNames
                );
                buildingSelector.setAdapter(adapter);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(RegisterActivity.this,
                        "Could not load buildings: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptRegister() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String aptNumber = aptNumberEditText.getText().toString().trim();
        String adminKey = adminKeyEditText.getText().toString().trim();
        String buildingCode = buildingCodeEditText.getText().toString().trim();
        String selectedBuilding = buildingSelector.getText().toString().trim();

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

        if (selectedBuilding.isEmpty()) {
            Toast.makeText(this, "Please select a building", Toast.LENGTH_SHORT).show();
            return;
        }

        if (accountType.equals("Regular")) {
            if (!ValidationUtils.isNotEmpty(aptNumber)) {
                Toast.makeText(this, "Apartment number is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isNotEmpty(buildingCode)) {
                Toast.makeText(this, "Building code is required", Toast.LENGTH_SHORT).show();
                return;
            }

            validateBuildingCode(email, password, aptNumber, accountType, selectedBuilding, buildingCode);
            return;
        }

        if (accountType.equals("Admin")) {
            if (!adminKey.equals(ADMIN_KEY)) {
                Toast.makeText(this, "Invalid admin key", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, password, "", accountType, selectedBuilding);
        }
    }

    private void validateBuildingCode(String email, String password, String aptNumber,
                                      String accountType, String buildingName, String enteredCode) {

        userRepository.getBuildingCode(buildingName, new UserRepository.BuildingCodeFetchCallback() {
            @Override
            public void onSuccess(String correctCode) {

                if (!enteredCode.equals(correctCode)) {
                    Toast.makeText(RegisterActivity.this,
                            "Incorrect building code for selected building",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                checkApartmentAndRegister(email, password, aptNumber, accountType, buildingName);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(RegisterActivity.this,
                        "Could not verify building: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkApartmentAndRegister(String email, String password, String aptNumber,
                                           String accountType, String buildingName) {

        userRepository.checkApartmentExists(buildingName, aptNumber, new UserRepository.ApartmentCheckCallback() {
            @Override
            public void onResult(boolean exists) {
                if (exists) {
                    Toast.makeText(RegisterActivity.this,
                            "An account already exists for this apartment in this building",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                registerUser(email, password, aptNumber, accountType, buildingName);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(RegisterActivity.this,
                        "Could not verify apartment number: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(String email, String password, String aptNumber,
                              String accountType, String buildingName) {

        authManager.registerUser(email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser) {

                String defaultUsername = email.contains("@") ? email.split("@")[0] : email;
                User user = new User(email, aptNumber, true, accountType, buildingName, defaultUsername);

                userRepository.saveUser(firebaseUser.getUid(), user, new UserRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess() {

                        if (accountType.equals("Admin")) {
                            Toast.makeText(RegisterActivity.this,
                                    "Admin registration successful.",
                                    Toast.LENGTH_LONG).show();

                            authManager.signOut();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                            finish();
                            return;
                        }

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
