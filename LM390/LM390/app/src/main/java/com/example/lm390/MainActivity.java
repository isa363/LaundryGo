package com.example.lm390;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference machineRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Realtime Database
        machineRef = FirebaseDatabase.getInstance()
                .getReference("machines")
                .child("machine_1");

        machineRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    String state = snapshot.child("state").getValue(String.class);

                    if (state != null) {
                        updateUI(state);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    private void updateUI(String state) {

        TextView statusText = findViewById(R.id.statusText);
        View statusCircle = findViewById(R.id.statusCircle);

        Drawable background = statusCircle.getBackground();

        if (!(background instanceof GradientDrawable)) {
            return;
        }

        GradientDrawable drawable = (GradientDrawable) background;

        if (state == null) {
            statusText.setText("UNKNOWN");
            drawable.setColor(Color.GRAY);
            drawable.setStroke(8, Color.DKGRAY);
            return;
        }

        state = state.trim();

        if (state.equalsIgnoreCase("RUNNING")) {

            statusText.setText("RUNNING");
            drawable.setColor(Color.parseColor("#FF9800")); // Orange
            drawable.setStroke(8, Color.parseColor("#F57C00"));

        } else if (state.equalsIgnoreCase("AVAILABLE")) {

            statusText.setText("AVAILABLE");
            drawable.setColor(Color.parseColor("#1B5E20")); // Green
            drawable.setStroke(8, Color.parseColor("#4CAF50"));

        } else if (state.equalsIgnoreCase("OFF")) {

            statusText.setText("OFF");
            drawable.setColor(Color.parseColor("#B71C1C")); // Red
            drawable.setStroke(8, Color.parseColor("#F44336"));

        } else {

            statusText.setText("UNKNOWN");
            drawable.setColor(Color.GRAY);
            drawable.setStroke(8, Color.DKGRAY);
        }
    }
}