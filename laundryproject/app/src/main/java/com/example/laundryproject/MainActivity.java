package com.example.laundryproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button logoutButton, ToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        // Check if user is signed in
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.main_page);

        logoutButton = findViewById(R.id.main_logout);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            redirectToLogin();
        });

      ToRegister = findViewById(R.id.ToRegister_main);
        ToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

    }


    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}


