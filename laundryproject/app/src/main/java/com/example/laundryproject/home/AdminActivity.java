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

public class AdminActivity extends AppCompatActivity {

    private AuthManager authManager;
    private UserRepository userRepository;

    private Button logoutButton;
    private Button viewUsersButton;
    private Button viewMachinesButton;

    private Button editBuildingCodeButton;
    private Button analyticsButton;

    private TextView buildingCodesTextView;

    private String adminBuildingName; // The building assigned to this admin

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

                if (!"Admin".equalsIgnoreCase(user.accountType)) {
                    Toast.makeText(AdminActivity.this, "Access denied.", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                    return;
                }

                adminBuildingName = user.buildingCode; // buildingCode now stores buildingName
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
        viewMachinesButton = findViewById(R.id.admin_viewMachinesButton);

        editBuildingCodeButton = findViewById(R.id.admin_editBuildingCodeButton);
        analyticsButton       = findViewById(R.id.admin_analyticsButton);

        buildingCodesTextView = findViewById(R.id.admin_buildingCodesTextView);


        logoutButton.setOnClickListener(v -> {
            authManager.signOut();
            Toast.makeText(AdminActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        });

        viewUsersButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminViewUsersActivity.class);
            intent.putExtra("adminBuilding", adminBuildingName);
            startActivity(intent);
        });

        viewMachinesButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AdminViewMachinesActivity.class);
            intent.putExtra("adminBuilding", adminBuildingName);
            startActivity(intent);
        });

        editBuildingCodeButton.setOnClickListener(v -> showEditBuildingCodeDialog());

        analyticsButton.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, AdminAnalyticsActivity.class)));

        loadAdminBuilding();
    }

    private void loadAdminBuilding() {
        if (adminBuildingName == null || adminBuildingName.isEmpty()) {
            buildingCodesTextView.setText("No building assigned to this admin.");
            return;
        }

        userRepository.getBuildingCode(adminBuildingName, new UserRepository.BuildingCodeFetchCallback() {
            @Override
            public void onSuccess(String code) {
                String text = "Building: " + adminBuildingName + "\n"
                        + "Code: " + code + "\n"
                        + "-----------------------------\n";

                buildingCodesTextView.setText(text);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditBuildingCodeDialog() {
        if (adminBuildingName == null) {
            Toast.makeText(this, "No building assigned.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText codeInput = new EditText(this);
        codeInput.setHint("Enter new building code");
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(codeInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Code for " + adminBuildingName)
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCode = codeInput.getText().toString().trim();

                    if (newCode.isEmpty()) {
                        Toast.makeText(this, "Code cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRepository.editBuilding(adminBuildingName, newCode, true, new UserRepository.FirestoreCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AdminActivity.this, "Code updated.", Toast.LENGTH_SHORT).show();
                            loadAdminBuilding();
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

    private void redirectToLogin() {
        startActivity(new Intent(AdminActivity.this, LoginActivity.class));
        finish();
    }
}
