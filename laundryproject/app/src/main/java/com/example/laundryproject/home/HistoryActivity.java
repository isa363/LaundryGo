package com.example.laundryproject.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private TextView tvTotalSessions;
    private TextView tvTotalSpent;
    private HistoryAdapter adapter;
    private final List<HistoryItem> historyList = new ArrayList<>();

    private final UserRepository userRepository = new UserRepository();
    private String currentBuildingCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        setupToolbar();
        setupViews();
        setupRecycler();
        setupUsageInsightsButton();
        loadCurrentUserAndHistory();
        setupBottomNav();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.historyToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Session History");
        }
    }

    private void setupViews() {
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
    }

    private void setupRecycler() {
        RecyclerView recycler = findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recycler.setAdapter(adapter);
    }

    private void setupUsageInsightsButton() {
        MaterialButton btnUsageInsights = findViewById(R.id.btnUsageInsights);
        if (btnUsageInsights != null) {
            btnUsageInsights.setOnClickListener(v ->
                    startActivity(new Intent(HistoryActivity.this, UsageInsightsActivity.class)));
        }
    }

    private void setupBottomNav() {
        BottomNavHelper.setup(this, R.id.bottomNav, R.id.nav_history);
    }

    private void loadCurrentUserAndHistory() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository.getUser(firebaseUser.getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null || user.buildingCode == null || user.buildingCode.trim().isEmpty()) {
                    Toast.makeText(HistoryActivity.this,
                            "Could not determine your building.", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                    return;
                }

                currentBuildingCode = user.buildingCode.trim();
                loadHistoryForBuilding(currentBuildingCode);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(HistoryActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void loadHistoryForBuilding(String buildingCode) {
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        historyList.clear();
                        double totalSpent = 0.0;

                        for (DataSnapshot machine : snapshot.getChildren()) {
                            String machineBuilding = machine.child("buildingCode").getValue(String.class);
                            if (machineBuilding == null || !machineBuilding.equals(buildingCode)) {
                                continue;
                            }

                            String machineId = machine.getKey();
                            String machineName = machine.child("machineName").getValue(String.class);

                            if (machineName == null || machineName.trim().isEmpty()) {
                                machineName = machineId != null ? machineId : "Unknown Machine";
                            }

                            Double machinePrice = machine.child("price").getValue(Double.class);

                            DataSnapshot historySnapshot = machine.child("history");
                            for (DataSnapshot entry : historySnapshot.getChildren()) {
                                Long epoch = entry.child("epoch").getValue(Long.class);
                                Double duration = entry.child("durationMin").getValue(Double.class);
                                Double storedCost = entry.child("costUSD").getValue(Double.class);

                                if (epoch == null) {
                                    continue;
                                }

                                double safeDuration = duration != null ? duration : 0.0;
                                double safeCost = storedCost != null
                                        ? storedCost
                                        : (machinePrice != null ? machinePrice : 0.0);

                                historyList.add(new HistoryItem(machineName, epoch, safeDuration, safeCost));
                                totalSpent += safeCost;
                            }
                        }

                        historyList.sort((a, b) -> Long.compare(b.getEpoch(), a.getEpoch()));

                        tvTotalSessions.setText(String.valueOf(historyList.size()));
                        tvTotalSpent.setText(String.format(Locale.US, "$%.2f CAD", totalSpent));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(HistoryActivity.this,
                                "Failed to load history.", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    }
                });
    }

    private void showEmptyState() {
        historyList.clear();
        tvTotalSessions.setText("0");
        tvTotalSpent.setText("$0.00 CAD");
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}