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
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private AuthManager authManager;
    private UserRepository userRepository;

    private Button logoutButton;

    private Button createBuildingCodeButton;
    private Button editBuildingCodeButton;
    private Button toggleBuildingCodeButton;
    private Button deleteBuildingCodeButton;

    private Button viewUsersButton;
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
        viewUsersButton = findViewById(R.id.admin_viewUsersButton);

        createBuildingCodeButton = findViewById(R.id.admin_createBuildingCodeButton);
        editBuildingCodeButton = findViewById(R.id.admin_editBuildingCodeButton);
        toggleBuildingCodeButton = findViewById(R.id.admin_toggleBuildingCodeButton);
        deleteBuildingCodeButton = findViewById(R.id.admin_deleteBuildingCodeButton);

        buildingCodesTextView = findViewById(R.id.admin_buildingCodesTextView);

        logoutButton.setOnClickListener(v -> {
            authManager.signOut();
            Toast.makeText(AdminActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });

        viewUsersButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminViewUsersActivity.class);
            startActivity(intent);
        });

        createBuildingCodeButton.setOnClickListener(v -> showCreateBuildingCodeDialog());
        editBuildingCodeButton.setOnClickListener(v -> showEditBuildingCodeDialog());
        toggleBuildingCodeButton.setOnClickListener(v -> showToggleBuildingCodeDialog());
        deleteBuildingCodeButton.setOnClickListener(v -> showDeleteBuildingCodeDialog());

        loadAllBuildingCodes();
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
    private void redirectToLogin() {
        startActivity(new Intent(AdminActivity.this, LoginActivity.class));
        finish();
    }
}