package com.example.laundryproject.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.LoginActivity;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.cardview.widget.CardView;

import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private final UserRepository userRepository = new UserRepository();

    private TextView tvWelcome;
    private TextView tvAvailableMachines;
    private TextView tvRunningMachines;

    private TextView tvPeakHour;
    private TextView tvLeastBusyHour;;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bindViews();
        setupShortcuts();
        setupBottomNav();
        loadDashboardData();

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
    }

    private void bindViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAvailableMachines = findViewById(R.id.tvAvailableMachines);
        tvRunningMachines = findViewById(R.id.tvRunningMachines);

        tvPeakHour = findViewById(R.id.tvPeakHour);
        tvLeastBusyHour = findViewById(R.id.tvLeastBusyHour);


    }

    private void setupShortcuts() {
        findViewById(R.id.btnOpenMachines).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        CardView cardHistoryShortcut = findViewById(R.id.cardHistoryShortcut);
        CardView cardInsightsShortcut = findViewById(R.id.cardInsightsShortcut);

        cardHistoryShortcut.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        cardInsightsShortcut.setOnClickListener(v ->
                startActivity(new Intent(this, UsageInsightsActivity.class)));
    }
    private void setupBottomNav() {
        BottomNavHelper.setup(this, R.id.bottomNavigationView, R.id.nav_home);
    }

    private void loadDashboardData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepository.getUser(firebaseUser.getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null || user.buildingCode == null || user.buildingCode.trim().isEmpty()) {
                    Toast.makeText(HomeActivity.this, "No building found for this user.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String buildingCode = user.buildingCode.trim();
                tvWelcome.setText("Welcome back");

                FirebaseDatabase.getInstance()
                        .getReference("machines")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                int available = 0;
                                int running = 0;


                                UsageInsightCalculator.Result result =
                                        UsageInsightCalculator.fromSnapshot(snapshot, buildingCode, UsageInsightCalculator.FILTER_ALL, UsageInsightCalculator.TIME_WEEK);

                                for (DataSnapshot machine : snapshot.getChildren()) {
                                    String machineBuilding = machine.child("buildingCode").getValue(String.class);
                                    if (machineBuilding == null || !machineBuilding.equals(buildingCode)) {
                                        continue;
                                    }

                                    String state = machine.child("state").getValue(String.class);
                                    if (state == null) continue;

                                    if ("AVAILABLE".equalsIgnoreCase(state)) {
                                        available++;
                                    } else if ("RUNNING".equalsIgnoreCase(state)) {
                                        running++;
                                    }
                                }



                                tvAvailableMachines.setText(String.valueOf(available));
                                tvRunningMachines.setText(String.valueOf(running));

                                tvPeakHour.setText(result.totalSessions > 0 ? result.getPeakLabel() : "—");
                                tvLeastBusyHour.setText(result.totalSessions > 0 ? result.getQuietLabel() : "—");
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(HomeActivity.this, "Failed to load dashboard.", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}