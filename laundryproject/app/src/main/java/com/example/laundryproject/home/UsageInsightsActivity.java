package com.example.laundryproject.home;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.home.views.HourlyUsageChartView;
import com.example.laundryproject.home.views.UsageHeatmapView;
import com.example.laundryproject.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UsageInsightsActivity extends AppCompatActivity {

    private TextView tvSubtitle;
    private TextView tvPeakValue;
    private TextView tvQuietValue;
    private TextView tvSessionsValue;

    private MaterialButton btnAll;
    private MaterialButton btnWashers;
    private MaterialButton btnDryers;

    private UsageHeatmapView heatmapView;
    private HourlyUsageChartView hourlyChartView;

    private final UserRepository userRepository = new UserRepository();

    private String currentFilter = UsageInsightCalculator.FILTER_ALL;
    private String currentBuildingCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_insights);

        BottomNavHelper.setup(this, R.id.bottomNav, R.id.nav_history);

        setupToolbar();
        bindViews();
        setupFilterButtons();
        loadCurrentUserAndInsights();
        setupBottomNav();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.insightsToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Usage Insights");
        }
    }

    private void bindViews() {
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvPeakValue = findViewById(R.id.tvPeakValue);
        tvQuietValue = findViewById(R.id.tvQuietValue);
        tvSessionsValue = findViewById(R.id.tvSessionsValue);

        btnAll = findViewById(R.id.btnAll);
        btnWashers = findViewById(R.id.btnWashers);
        btnDryers = findViewById(R.id.btnDryers);

        heatmapView = findViewById(R.id.usageHeatmapView);
        hourlyChartView = findViewById(R.id.hourlyUsageChartView);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> setFilter(UsageInsightCalculator.FILTER_ALL));
        btnWashers.setOnClickListener(v -> setFilter(UsageInsightCalculator.FILTER_WASHERS));
        btnDryers.setOnClickListener(v -> setFilter(UsageInsightCalculator.FILTER_DRYERS));
        updateFilterButtons();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();

        if (currentBuildingCode != null && !currentBuildingCode.trim().isEmpty()) {
            observeInsights(currentBuildingCode);
        }
    }

    private void updateFilterButtons() {
        styleFilterButton(btnAll, UsageInsightCalculator.FILTER_ALL.equals(currentFilter));
        styleFilterButton(btnWashers, UsageInsightCalculator.FILTER_WASHERS.equals(currentFilter));
        styleFilterButton(btnDryers, UsageInsightCalculator.FILTER_DRYERS.equals(currentFilter));
    }

    private void styleFilterButton(MaterialButton button, boolean selected) {
        button.setChecked(selected);

        if (selected) {
            button.setBackgroundTintList(getColorStateList(R.color.insight_chip_selected_bg));
            button.setTextColor(getColor(R.color.insight_chip_selected_text));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(getColorStateList(R.color.insight_chip_bg));
            button.setTextColor(getColor(R.color.insight_chip_text));
            button.setStrokeWidth(2);
            button.setStrokeColor(getColorStateList(R.color.insight_chip_stroke));
        }
    }

    private void loadCurrentUserAndInsights() {
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
                    Toast.makeText(UsageInsightsActivity.this,
                            "Could not determine your building.", Toast.LENGTH_SHORT).show();
                    showEmptyState("No building available");
                    return;
                }

                currentBuildingCode = user.buildingCode.trim();
                observeInsights(currentBuildingCode);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(UsageInsightsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                showEmptyState("Could not load insights");
            }
        });
    }

    private void observeInsights(String buildingCode) {
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        UsageInsightCalculator.Result result =
                                UsageInsightCalculator.fromSnapshot(snapshot, buildingCode, currentFilter);

                        if (result.totalSessions > 0) {
                            tvSubtitle.setText("Based on " + result.totalSessions + " recorded sessions");
                            tvPeakValue.setText(result.getPeakLabel());
                            tvQuietValue.setText(result.getQuietLabel());
                            tvSessionsValue.setText(String.valueOf(result.totalSessions));
                        } else {
                            tvSubtitle.setText("No history found yet for this selection");
                            tvPeakValue.setText("—");
                            tvQuietValue.setText("—");
                            tvSessionsValue.setText("0");
                        }

                        heatmapView.setData(result.dayHourCounts);
                        hourlyChartView.setData(result.hourlyAverages);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showEmptyState("Could not load insights");
                    }
                });
    }

    private void showEmptyState(String subtitle) {
        tvSubtitle.setText(subtitle);
        tvPeakValue.setText("—");
        tvQuietValue.setText("—");
        tvSessionsValue.setText("0");
        heatmapView.setData(new int[7][24]);
        hourlyChartView.setData(new float[24]);
    }

    private void setupBottomNav() {
        BottomNavHelper.setup(this, R.id.bottomNav, R.id.nav_history);
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