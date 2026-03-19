package com.example.laundryproject.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.auth.LoginActivity;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.google.firebase.auth.FirebaseUser;

// This is the main page, it uses UserRepository class to load all user info
// This is where we will add main functionalities using the userRepository functions that we can add as more stuff gets added to the project
public class MainActivity extends AppCompatActivity {

    private AuthManager authManager;
    private UserRepository userRepository;
    private Button logoutButton;

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
                Toast.makeText(MainActivity.this,
                        "Please verify your email before accessing the app.",
                        Toast.LENGTH_LONG).show();
                redirectToLogin();
                return;
            }

            EdgeToEdge.enable(MainActivity.this);
            setContentView(R.layout.main_page);

            logoutButton = findViewById(R.id.main_logout);
            logoutButton.setOnClickListener(v -> {
                authManager.signOut();
                redirectToLogin();
            });

            loadCurrentUserData();
        });
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        String uid = currentUser.getUid();

        userRepository.getUser(uid, new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(MainActivity.this,
                        "Welcome " + user.email + " | Apt: " + user.aptNumber,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}