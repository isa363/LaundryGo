package com.example.laundryproject.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.auth.LoginActivity;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.example.laundryproject.util.ValidationUtils;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private AuthManager authManager;
    private UserRepository userRepository;

    private Button logoutButton;
    private Button refreshButton;
    private Button changeAptButton;
    private Button toggleEnabledButton;
    private Button changeEmailButton;
    private Button deleteUserButton;
    private Button changeAccountTypeButton;
    private Button changeBuildingCodeButton;

    private Button createBuildingCodeButton;
    private Button editBuildingCodeButton;
    private Button toggleBuildingCodeButton;
    private Button deleteBuildingCodeButton;

    private TextView usersTextView;
    private TextView buildingCodesTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        authManager = new AuthManager();
        userRepository = new UserRepository();

        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = authManager.getCurrentUser();

            if (refreshedUser == null || !refreshedUser.isEmailVerified()) {
                authManager.signOut();
                Toast.makeText(AdminActivity.this,
                        "Please verify your email before accessing the app.",
                        Toast.LENGTH_LONG).show();
                redirectToLogin();
                return;
            }

            checkAdminAccess();
        });
    }

    private void checkAdminAccess() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        userRepository.getUser(currentUser.getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null) {
                    Toast.makeText(AdminActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    authManager.signOut();
                    redirectToLogin();
                    return;
                }

                String accountType = user.accountType != null ? user.accountType.trim() : "";

                if (!accountType.equalsIgnoreCase("Admin")) {
                    Toast.makeText(AdminActivity.this, "Access denied.", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                    return;
                }

                setupAdminPage();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        });
    }

    private void setupAdminPage() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        logoutButton = findViewById(R.id.admin_logoutButton);
        refreshButton = findViewById(R.id.admin_refreshButton);
        changeAptButton = findViewById(R.id.admin_changeAptButton);
        toggleEnabledButton = findViewById(R.id.admin_toggleEnabledButton);
        changeEmailButton = findViewById(R.id.admin_changeEmailButton);
        deleteUserButton = findViewById(R.id.admin_deleteUserButton);
        changeAccountTypeButton = findViewById(R.id.admin_changeAccountTypeButton);
        changeBuildingCodeButton = findViewById(R.id.admin_changeBuildingCodeButton);

        createBuildingCodeButton = findViewById(R.id.admin_createBuildingCodeButton);
        editBuildingCodeButton = findViewById(R.id.admin_editBuildingCodeButton);
        toggleBuildingCodeButton = findViewById(R.id.admin_toggleBuildingCodeButton);
        deleteBuildingCodeButton = findViewById(R.id.admin_deleteBuildingCodeButton);

        usersTextView = findViewById(R.id.admin_usersTextView);
        buildingCodesTextView = findViewById(R.id.admin_buildingCodesTextView);

        logoutButton.setOnClickListener(v -> {
            authManager.signOut();
            Toast.makeText(AdminActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });

        refreshButton.setOnClickListener(v -> {
            loadAllUsers();
            loadAllBuildingCodes();
        });

        changeAptButton.setOnClickListener(v -> showChangeAptDialog());
        toggleEnabledButton.setOnClickListener(v -> showToggleEnabledDialog());
        changeEmailButton.setOnClickListener(v -> showChangeEmailDialog());
        deleteUserButton.setOnClickListener(v -> showDeleteUserDialog());
        changeAccountTypeButton.setOnClickListener(v -> showChangeAccountTypeDialog());
        changeBuildingCodeButton.setOnClickListener(v -> showChangeBuildingCodeDialog());

        createBuildingCodeButton.setOnClickListener(v -> showCreateBuildingCodeDialog());
        editBuildingCodeButton.setOnClickListener(v -> showEditBuildingCodeDialog());
        toggleBuildingCodeButton.setOnClickListener(v -> showToggleBuildingCodeDialog());
        deleteBuildingCodeButton.setOnClickListener(v -> showDeleteBuildingCodeDialog());

        loadAllUsers();
        loadAllBuildingCodes();
    }

    private void loadAllUsers() {
        userRepository.getAllUsers(new UserRepository.LoadUsersCallback() {
            @Override
            public void onSuccess(List<UserRepository.UserWithId> users) {
                if (users.isEmpty()) {
                    usersTextView.setText("No users found.");
                    return;
                }

                StringBuilder builder = new StringBuilder();

                for (UserRepository.UserWithId item : users) {
                    User user = item.user;

                    builder.append("UID: ").append(item.uid).append("\n");
                    builder.append("Email: ").append(user.email != null ? user.email : "N/A").append("\n");
                    builder.append("Apt: ").append(user.aptNumber != null ? user.aptNumber : "N/A").append("\n");
                    builder.append("Building Code: ").append(user.buildingCode != null ? user.buildingCode : "N/A").append("\n");
                    builder.append("Type: ").append(user.accountType != null ? user.accountType : "N/A").append("\n");
                    builder.append("Enabled: ").append(user.enabled).append("\n");
                    builder.append("-----------------------------\n");
                }

                usersTextView.setText(builder.toString());
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAllBuildingCodes() {
        userRepository.getAllBuildingCodes(new UserRepository.LoadBuildingCodesCallback() {
            @Override
            public void onSuccess(List<UserRepository.BuildingCodeItem> buildingCodes) {
                if (buildingCodes.isEmpty()) {
                    buildingCodesTextView.setText("No building codes found.");
                    return;
                }

                StringBuilder builder = new StringBuilder();

                for (UserRepository.BuildingCodeItem item : buildingCodes) {
                    builder.append("Code: ").append(item.code).append("\n");
                    builder.append("Building Name: ").append(item.buildingName != null ? item.buildingName : "N/A").append("\n");
                    builder.append("Enabled: ").append(item.enabled).append("\n");
                    builder.append("-----------------------------\n");
                }

                buildingCodesTextView.setText(builder.toString());
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateBuildingCodeDialog() {
        final EditText codeInput = new EditText(this);
        codeInput.setHint("Enter building code");

        final EditText buildingNameInput = new EditText(this);
        buildingNameInput.setHint("Enter building name");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(codeInput);
        layout.addView(buildingNameInput);

        new AlertDialog.Builder(this)
                .setTitle("Create Building Code")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim().toUpperCase(Locale.ROOT);
                    String buildingName = buildingNameInput.getText().toString().trim();

                    if (code.isEmpty()) {
                        Toast.makeText(this, "Building code is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (buildingName.isEmpty()) {
                        Toast.makeText(this, "Building name is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.createBuildingCode(code, buildingName, true, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Building code saved.", Toast.LENGTH_SHORT).show();
                            loadAllBuildingCodes();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditBuildingCodeDialog() {
        final EditText codeInput = new EditText(this);
        codeInput.setHint("Enter building code to edit");

        final EditText buildingNameInput = new EditText(this);
        buildingNameInput.setHint("Enter new building name");

        final EditText enabledInput = new EditText(this);
        enabledInput.setHint("Enter true or false");
        enabledInput.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(codeInput);
        layout.addView(buildingNameInput);
        layout.addView(enabledInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Building Code")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim().toUpperCase(Locale.ROOT);
                    String buildingName = buildingNameInput.getText().toString().trim();
                    String enabledText = enabledInput.getText().toString().trim();

                    if (code.isEmpty()) {
                        Toast.makeText(this, "Building code is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (buildingName.isEmpty()) {
                        Toast.makeText(this, "Building name is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!enabledText.equalsIgnoreCase("true") && !enabledText.equalsIgnoreCase("false")) {
                        Toast.makeText(this, "Enabled must be true or false.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean enabled = Boolean.parseBoolean(enabledText);

                    userRepository.editBuildingCode(code, buildingName, enabled, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Building code updated.", Toast.LENGTH_SHORT).show();
                            loadAllBuildingCodes();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToggleBuildingCodeDialog() {
        final EditText codeInput = new EditText(this);
        codeInput.setHint("Enter building code");

        final EditText enabledInput = new EditText(this);
        enabledInput.setHint("Enter true or false");
        enabledInput.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(codeInput);
        layout.addView(enabledInput);

        new AlertDialog.Builder(this)
                .setTitle("Toggle Building Code")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim().toUpperCase(Locale.ROOT);
                    String enabledText = enabledInput.getText().toString().trim();

                    if (code.isEmpty()) {
                        Toast.makeText(this, "Building code is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!enabledText.equalsIgnoreCase("true") && !enabledText.equalsIgnoreCase("false")) {
                        Toast.makeText(this, "Enter true or false only.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean enabled = Boolean.parseBoolean(enabledText);

                    userRepository.updateBuildingCodeEnabled(code, enabled, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Building code updated.", Toast.LENGTH_SHORT).show();
                            loadAllBuildingCodes();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteBuildingCodeDialog() {
        final EditText codeInput = new EditText(this);
        codeInput.setHint("Enter building code");

        new AlertDialog.Builder(this)
                .setTitle("Delete Building Code")
                .setView(codeInput)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim().toUpperCase(Locale.ROOT);

                    if (code.isEmpty()) {
                        Toast.makeText(this, "Building code is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.deleteBuildingCode(code, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Building code deleted.", Toast.LENGTH_SHORT).show();
                            loadAllBuildingCodes();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeAptDialog() {
        final EditText uidInput = new EditText(this);
        uidInput.setHint("Enter user UID");

        final EditText aptInput = new EditText(this);
        aptInput.setHint("Enter new apartment number");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(uidInput);
        layout.addView(aptInput);

        new AlertDialog.Builder(this)
                .setTitle("Change Apartment Number")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String uid = uidInput.getText().toString().trim();
                    String aptNumber = aptInput.getText().toString().trim();

                    if (uid.isEmpty() || aptNumber.isEmpty()) {
                        Toast.makeText(this, "UID and apartment number are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.updateUserField(uid, "aptNumber", aptNumber, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Apartment number updated.", Toast.LENGTH_SHORT).show();
                            loadAllUsers();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToggleEnabledDialog() {
        final EditText uidInput = new EditText(this);
        uidInput.setHint("Enter user UID");

        final EditText enabledInput = new EditText(this);
        enabledInput.setHint("Enter true or false");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(uidInput);
        layout.addView(enabledInput);

        new AlertDialog.Builder(this)
                .setTitle("Change Enabled Status")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String uid = uidInput.getText().toString().trim();
                    String enabledText = enabledInput.getText().toString().trim();

                    if (uid.isEmpty()) {
                        Toast.makeText(this, "UID is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!enabledText.equalsIgnoreCase("true") && !enabledText.equalsIgnoreCase("false")) {
                        Toast.makeText(this, "Enter true or false only.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean enabled = Boolean.parseBoolean(enabledText);

                    userRepository.updateUserField(uid, "enabled", enabled, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Enabled status updated.", Toast.LENGTH_SHORT).show();
                            loadAllUsers();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeEmailDialog() {
        final EditText uidInput = new EditText(this);
        uidInput.setHint("Enter user UID");

        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter new email");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(uidInput);
        layout.addView(emailInput);

        new AlertDialog.Builder(this)
                .setTitle("Change Email")
                .setMessage("This only changes the Firestore email field, not Firebase Authentication email.")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String uid = uidInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();

                    if (uid.isEmpty()) {
                        Toast.makeText(this, "UID is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!ValidationUtils.isEmailValid(email)) {
                        Toast.makeText(this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.updateUserField(uid, "email", email, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this,
                                    "Firestore email updated. Firebase Auth email was not changed.",
                                    Toast.LENGTH_LONG).show();
                            loadAllUsers();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeBuildingCodeDialog() {
        final EditText uidInput = new EditText(this);
        uidInput.setHint("Enter user UID");

        final EditText buildingCodeInput = new EditText(this);
        buildingCodeInput.setHint("Enter new building code");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(uidInput);
        layout.addView(buildingCodeInput);

        new AlertDialog.Builder(this)
                .setTitle("Change User Building Code")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String uid = uidInput.getText().toString().trim();
                    String buildingCode = buildingCodeInput.getText().toString().trim().toUpperCase(Locale.ROOT);

                    if (uid.isEmpty()) {
                        Toast.makeText(this, "UID is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (buildingCode.isEmpty()) {
                        Toast.makeText(this, "Building code is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.checkBuildingCodeExists(buildingCode, new UserRepository.BuildingCodeCheckCallback() {
                        @Override
                        public void onResult(boolean exists) {
                            if (!exists) {
                                Toast.makeText(AdminActivity.this,
                                        "Invalid or disabled building code.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            userRepository.updateUserField(uid, "buildingCode", buildingCode, new UserRepository.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(AdminActivity.this,
                                            "User building code updated.",
                                            Toast.LENGTH_SHORT).show();
                                    loadAllUsers();
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteUserDialog() {
        final EditText uidInput = new EditText(this);
        uidInput.setHint("Enter user UID");

        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("This only deletes the Firestore user document, not the Firebase Auth account.")
                .setView(uidInput)
                .setPositiveButton("Delete", (dialog, which) -> {
                    String uid = uidInput.getText().toString().trim();

                    if (uid.isEmpty()) {
                        Toast.makeText(this, "UID is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.deleteUserDocument(uid, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this,
                                    "User document deleted. Firebase Auth account still exists.",
                                    Toast.LENGTH_LONG).show();
                            loadAllUsers();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeAccountTypeDialog() {
        final EditText uidInput = new EditText(this);
        uidInput.setHint("Enter user UID");

        final EditText typeInput = new EditText(this);
        typeInput.setHint("Enter new account type (Regular/Admin)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(uidInput);
        layout.addView(typeInput);

        new AlertDialog.Builder(this)
                .setTitle("Change Account Type")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String uid = uidInput.getText().toString().trim();
                    String accountType = typeInput.getText().toString().trim();

                    if (uid.isEmpty() || accountType.isEmpty()) {
                        Toast.makeText(this, "UID and account type are required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.updateUserField(uid, "accountType", accountType, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Account type updated.", Toast.LENGTH_SHORT).show();
                            loadAllUsers();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void redirectToLogin() {
        startActivity(new Intent(AdminActivity.this, LoginActivity.class));
        finish();
    }
}