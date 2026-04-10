package com.example.laundryproject.home;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.home.views.HourlyUsageChartView;
import com.example.laundryproject.home.views.UsageHeatmapView;
import com.example.laundryproject.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private TextView tvSubtitle;
    private TextView tvRevenueValue;
    private TextView tvCyclesValue;
    private TextView tvPeakHourValue;
    private TextView tvTopMachineValue;
    private TextView tvMachineCountValue;

    private UsageHeatmapView heatmapView;
    private HourlyUsageChartView hourlyChartView;

    private final AuthManager authManager = new AuthManager();
    private final UserRepository userRepository = new UserRepository();

    private String adminBuilding;
    private String currentTimeFilter = UsageInsightCalculator.TIME_WEEK;

    private MaterialButton btnTimeWeek;
    private MaterialButton btnTimeMonth;
    private MaterialButton btnTimeAll;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        setupToolbar();
        bindViews();
        setupTimeFilterButtons();
        loadAdminBuildingAndAnalytics();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.adminAnalyticsToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics Dashboard");
        }
    }

    private void bindViews() {
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvRevenueValue = findViewById(R.id.tvRevenueValue);
        tvCyclesValue = findViewById(R.id.tvCyclesValue);
        tvPeakHourValue = findViewById(R.id.tvPeakHourValue);
        tvTopMachineValue = findViewById(R.id.tvTopMachineValue);
        tvMachineCountValue = findViewById(R.id.tvMachineCountValue);

        heatmapView = findViewById(R.id.adminUsageHeatmapView);
        hourlyChartView = findViewById(R.id.adminHourlyUsageChartView);
        btnTimeWeek  = findViewById(R.id.btnTimeWeek);
        btnTimeMonth = findViewById(R.id.btnTimeMonth);
        btnTimeAll   = findViewById(R.id.btnTimeAll);
    }

    private void loadAdminBuildingAndAnalytics() {
        if (authManager.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRepository.getUser(authManager.getCurrentUser().getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null || user.buildingCode == null || user.buildingCode.trim().isEmpty()) {
                    showEmptyState("No building available");
                    return;
                }

                adminBuilding = user.buildingCode.trim();
                observeAnalytics();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminAnalyticsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                showEmptyState("Could not load analytics");
            }
        });
    }

    private void observeAnalytics() {
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        AdminAnalyticsCalculator.Result result =
                                AdminAnalyticsCalculator.fromSnapshot(snapshot, adminBuilding, currentTimeFilter);

                        if (result.totalCycles > 0) {
                            tvSubtitle.setText("Overview for building " + adminBuilding);
                            tvRevenueValue.setText(String.format(Locale.US, "$%.2f", result.totalRevenue));
                            tvCyclesValue.setText(String.valueOf(result.totalCycles));
                            tvPeakHourValue.setText(AdminAnalyticsCalculator.formatHourRange(result.peakHour));
                            tvTopMachineValue.setText(result.topMachineName + " • " + result.topMachineCycles + " cycles");
                            tvMachineCountValue.setText(String.valueOf(result.activeMachineCount));
                        } else {
                            tvSubtitle.setText("No machine history found yet");
                            tvRevenueValue.setText("$0.00");
                            tvCyclesValue.setText("0");
                            tvPeakHourValue.setText("—");
                            tvTopMachineValue.setText("—");
                            tvMachineCountValue.setText(String.valueOf(result.activeMachineCount));
                        }

                        heatmapView.setData(result.dayHourCounts);
                        hourlyChartView.setData(result.hourlyAverages);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        showEmptyState("Could not load analytics");
                    }
                });
    }

    private void showEmptyState(String subtitle) {
        tvSubtitle.setText(subtitle);
        tvRevenueValue.setText("$0.00");
        tvCyclesValue.setText("0");
        tvPeakHourValue.setText("—");
        tvTopMachineValue.setText("—");
        tvMachineCountValue.setText("0");
        heatmapView.setData(new int[7][24]);
        hourlyChartView.setData(new float[24]);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void setupTimeFilterButtons() {
        btnTimeWeek.setOnClickListener(v -> setTimeFilter(UsageInsightCalculator.TIME_WEEK));
        btnTimeMonth.setOnClickListener(v -> setTimeFilter(UsageInsightCalculator.TIME_MONTH));
        btnTimeAll.setOnClickListener(v -> setTimeFilter(UsageInsightCalculator.TIME_ALL));
        updateTimeFilterButtons();
    }

    private void setTimeFilter(String timeFilter) {
        currentTimeFilter = timeFilter;
        updateTimeFilterButtons();
        observeAnalytics();
    }

    private void updateTimeFilterButtons() {
        styleButton(btnTimeWeek, UsageInsightCalculator.TIME_WEEK.equals(currentTimeFilter));
        styleButton(btnTimeMonth, UsageInsightCalculator.TIME_MONTH.equals(currentTimeFilter));
        styleButton(btnTimeAll, UsageInsightCalculator.TIME_ALL.equals(currentTimeFilter));
    }

    private void styleButton(MaterialButton button, boolean selected) {
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
}