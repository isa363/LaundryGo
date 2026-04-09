package com.example.laundryproject.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.auth.LoginActivity;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.dialogs.EmailDialog;
import com.example.laundryproject.dialogs.PassDialog;
import com.example.laundryproject.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity
        implements EmailDialog.EmailUpdateListener, PassDialog.PasswordUpdateListener {

    private MaterialToolbar profileToolbar;
    private Button closedTicketsBtn;
    private EditText editUsername;
    private TextView viewEmail;
    private TextView apt;
    private TextView bldgcodeView;
    private Button saveBtn;
    private Button submitTicketBtn;
    private Button btnInsights;
    private Button myTicketsBtn;

    private LinearLayout emailRow;
    private LinearLayout passwordRow;

    private AuthManager authManager;
    private UserRepository userRepository;
    private FirebaseUser currentUser;

    private User loadedUser;
    private String pendingEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = new AuthManager();
        userRepository = new UserRepository();
        currentUser = authManager.getCurrentUser();

        initViews();
        setupToolbar();
        loadProfileData();
        setupListeners();
        setupBottomNav();

        if (getIntent().getBooleanExtra("email_updated", false)) {
            currentUser = authManager.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                viewEmail.setText(currentUser.getEmail());
            }
            loadProfileData();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        currentUser.reload().addOnCompleteListener(task -> {
            FirebaseUser refreshedUser = authManager.getCurrentUser();
            if (refreshedUser != null && refreshedUser.getEmail() != null) {
                String actualEmail = refreshedUser.getEmail();
                viewEmail.setText(actualEmail);

                if (pendingEmail != null && pendingEmail.equalsIgnoreCase(actualEmail)) {
                    userRepository.updateUserField(
                            refreshedUser.getUid(),
                            "email",
                            actualEmail,
                            new UserRepository.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    pendingEmail = null;
                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Email verified and profile updated.",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }

                                @Override
                                public void onFailure(String errorMessage) {
                                    Toast.makeText(
                                            ProfileActivity.this,
                                            "Auth email changed, but Firestore update failed: " + errorMessage,
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            }
                    );
                }
            }
        });
    }

    private void initViews() {
        profileToolbar = findViewById(R.id.profileToolbar);

        editUsername = findViewById(R.id.editUsername);
        viewEmail = findViewById(R.id.viewEmail);
        apt = findViewById(R.id.apt);
        bldgcodeView = findViewById(R.id.bldgcodeView);
        saveBtn = findViewById(R.id.saveBtn);
        btnInsights = findViewById(R.id.btnInsights);
        submitTicketBtn = findViewById(R.id.submitTicketBtn);
        myTicketsBtn = findViewById(R.id.myTicketsBtn);
        closedTicketsBtn = findViewById(R.id.closedTicketsBtn);

        emailRow = findViewById(R.id.emailRow);
        passwordRow = findViewById(R.id.passwordRow);
    }

    private void setupToolbar() {
        profileToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupBottomNav() {
        BottomNavHelper.setup(this, R.id.bottomNav, R.id.nav_account);
    }
    private void loadProfileData() {
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        viewEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email");

        userRepository.getUser(currentUser.getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                loadedUser = user;

                if (user == null) {
                    Toast.makeText(ProfileActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!TextUtils.isEmpty(user.username)) {
                    editUsername.setText(user.username);
                } else if (currentUser.getEmail() != null && currentUser.getEmail().contains("@")) {
                    editUsername.setText(currentUser.getEmail().split("@")[0]);
                }

                apt.setText(!TextUtils.isEmpty(user.aptNumber) ? user.aptNumber : "N/A");
                bldgcodeView.setText(!TextUtils.isEmpty(user.buildingCode) ? user.buildingCode : "N/A");

                if (!TextUtils.isEmpty(user.email)) {
                    viewEmail.setText(user.email);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        saveBtn.setOnClickListener(v -> saveUsername());

        closedTicketsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ClosedTicketsActivity.class);
            startActivity(intent);
        });

        submitTicketBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SubmitTicketActivity.class);
            startActivity(intent);
        });

        myTicketsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MyTicketsActivity.class);
            startActivity(intent);
        });

        btnInsights.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, CostTrackingActivity.class);
            startActivity(intent);
        });

        emailRow.setOnClickListener(v -> {
            EmailDialog dialog = new EmailDialog();
            dialog.setEmailUpdateListener(this);
            dialog.show(getSupportFragmentManager(), "EmailDialog");
        });

        passwordRow.setOnClickListener(v -> {
            PassDialog dialog = new PassDialog();
            dialog.setPasswordUpdateListener(this);
            dialog.show(getSupportFragmentManager(), "PasswordDialog");
        });
    }

    private void saveUsername() {
        if (currentUser == null) {
            Toast.makeText(this, "No signed-in user found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newUsername = editUsername.getText().toString().trim();

        if (newUsername.isEmpty()) {
            editUsername.setError("Username cannot be empty");
            editUsername.requestFocus();
            return;
        }

        userRepository.updateUserField(
                currentUser.getUid(),
                "username",
                newUsername,
                new UserRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        if (loadedUser != null) {
                            loadedUser.username = newUsername;
                        }
                        Toast.makeText(
                                ProfileActivity.this,
                                "Username updated successfully.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(
                                ProfileActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    @Override
    public void onEmailVerificationSent(String pendingEmail) {
        this.pendingEmail = pendingEmail;
        Toast.makeText(
                this,
                "Complete verification from your inbox, then return to the app.",
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    public void onPasswordUpdated() {
        Toast.makeText(this, "Password change completed.", Toast.LENGTH_SHORT).show();
    }
}
