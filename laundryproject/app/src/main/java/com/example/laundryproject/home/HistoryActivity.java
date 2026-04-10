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

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(firebaseUser.getUid())
                .child("history")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        historyList.clear();
                        double totalSpent = 0.0;

                        for (DataSnapshot entry : snapshot.getChildren()) {
                            Long epoch      = entry.child("epoch").getValue(Long.class);
                            Double duration = entry.child("durationMin").getValue(Double.class);
                            Double cost     = entry.child("costUSD").getValue(Double.class);
                            String mName    = entry.child("machineName").getValue(String.class);

                            if (epoch == null) continue;

                            double safeDuration = duration != null ? duration : 0.0;
                            double safeCost     = cost != null ? cost : 0.0;
                            String safeName     = (mName != null && !mName.isEmpty()) ? mName : "Machine";

                            historyList.add(new HistoryItem(safeName, epoch, safeDuration, safeCost));
                            totalSpent += safeCost;
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