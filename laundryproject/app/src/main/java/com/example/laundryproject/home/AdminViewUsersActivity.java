package com.example.laundryproject.home;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminViewUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UsersAdapter adapter;
    private UserRepository userRepository;

    private List<UserRepository.UserWithId> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_users);

        MaterialToolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);

        // Enable back arrow
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // Handle back arrow click
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationIconTint(getResources().getColor(android.R.color.white));

        userRepository = new UserRepository();

        recyclerView = findViewById(R.id.rvUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new UsersAdapter(userList, this::showUserActionsDialog);
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        userRepository.getAllUsers(new UserRepository.LoadUsersCallback() {
            @Override
            public void onSuccess(List<UserRepository.UserWithId> users) {

                // Sort users by apartment number
                Collections.sort(users, (u1, u2) -> {
                    try {
                        int apt1 = Integer.parseInt(u1.user.aptNumber != null ? u1.user.aptNumber : "0");
                        int apt2 = Integer.parseInt(u2.user.aptNumber != null ? u2.user.aptNumber : "0");
                        return Integer.compare(apt1, apt2);
                    } catch (Exception e) {
                        return 0; // fallback if parsing fails
                    }
                });

                userList.clear();
                userList.addAll(users);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminViewUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showUserActionsDialog(UserRepository.UserWithId item) {
        User user = item.user;

        String message =
                "Email: " + user.email + "\n" +
                        "User Name: " + user.username + "\n" +
                        "Apartment: " + user.aptNumber + "\n" +
                        "Is User Allowed: " + user.enabled;

        new AlertDialog.Builder(this)
                .setTitle("User Options")
                .setMessage(message)
                .setNegativeButton("Edit User", (d, w) -> showEditUserDialog(item))
                .setPositiveButton("Delete User", (d, w) -> deleteUser(item.uid))
                .setNeutralButton("Close", null)
                .show();
    }

    private void showEditUserDialog(UserRepository.UserWithId item) {
        User user = item.user;

        // Username input
        final EditText usernameInput = new EditText(this);
        usernameInput.setHint("Username");
        usernameInput.setText(user.username != null ? user.username : "");

        // Apartment number input
        final EditText aptInput = new EditText(this);
        aptInput.setHint("Apartment Number");
        aptInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        aptInput.setText(user.aptNumber != null ? user.aptNumber : "");

        // Enabled toggle
        final Switch enabledSwitch = new Switch(this);
        enabledSwitch.setText("Enabled");
        enabledSwitch.setChecked(user.enabled);

        // Apply initial tint
        enabledSwitch.getThumbDrawable().setTint(enabledSwitch.isChecked() ? 0xFF1A56A0 : 0xFFBDBDBD);
        enabledSwitch.getTrackDrawable().setTint(enabledSwitch.isChecked() ? 0x801A56A0 : 0x80BDBDBD);

        // Update tint when toggled
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enabledSwitch.getThumbDrawable().setTint(isChecked ? 0xFF1A56A0 : 0xFFBDBDBD);
            enabledSwitch.getTrackDrawable().setTint(isChecked ? 0x801A56A0 : 0x80BDBDBD);
        });

        // Layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(usernameInput);
        layout.addView(aptInput);
        layout.addView(enabledSwitch);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit User")
                .setView(layout)
                .setPositiveButton("Save", null) // override later
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                String newUsername = usernameInput.getText().toString().trim();
                String aptText = aptInput.getText().toString().trim();
                boolean newEnabled = enabledSwitch.isChecked();

                // VALIDATION 1 — Required fields
                if (newUsername.isEmpty() || aptText.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return; // keep dialog open
                }

                // VALIDATION 2 — Username length
                if (newUsername.length() < 1 || newUsername.length() > 25) {
                    Toast.makeText(this, "Username must be 1–25 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // VALIDATION 3 — Apartment must be integer
                int aptNumber;
                try {
                    aptNumber = Integer.parseInt(aptText);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Apartment must be a number", Toast.LENGTH_SHORT).show();
                    return;
                }

                // VALIDATION 4 — Apartment range
                if (aptNumber < 0 || aptNumber > 1000) {
                    Toast.makeText(this, "Apartment must be between 0 and 1000", Toast.LENGTH_SHORT).show();
                    return;
                }

                // UPDATE USERNAME
                userRepository.updateUserField(item.uid, "username", newUsername, new UserRepository.FirestoreCallback() {
                    @Override
                    public void onSuccess() {

                        // UPDATE APARTMENT
                        userRepository.updateUserField(item.uid, "aptNumber", String.valueOf(aptNumber), new UserRepository.FirestoreCallback() {
                            @Override
                            public void onSuccess() {

                                // UPDATE ENABLED
                                userRepository.updateUserField(item.uid, "enabled", newEnabled, new UserRepository.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(AdminViewUsersActivity.this, "User updated", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        loadUsers();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(AdminViewUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(AdminViewUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(AdminViewUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }


    private void deleteUser(String uid) {
        userRepository.deleteUserDocument(uid, new UserRepository.FirestoreCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(AdminViewUsersActivity.this, "User deleted.", Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminViewUsersActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
