package com.example.laundryproject.home;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.home.views.HourlyUsageChartView;
import com.example.laundryproject.home.views.UsageHeatmapView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
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

    private String currentFilter = UsageInsightCalculator.FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_insights);

        MaterialToolbar toolbar = findViewById(R.id.insightsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Usage Insights");
        }

        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvPeakValue = findViewById(R.id.tvPeakValue);
        tvQuietValue = findViewById(R.id.tvQuietValue);
        tvSessionsValue = findViewById(R.id.tvSessionsValue);

        btnAll = findViewById(R.id.btnAll);
        btnWashers = findViewById(R.id.btnWashers);
        btnDryers = findViewById(R.id.btnDryers);

        heatmapView = findViewById(R.id.usageHeatmapView);
        hourlyChartView = findViewById(R.id.hourlyUsageChartView);

        btnAll.setOnClickListener(v -> setFilter(UsageInsightCalculator.FILTER_ALL));
        btnWashers.setOnClickListener(v -> setFilter(UsageInsightCalculator.FILTER_WASHERS));
        btnDryers.setOnClickListener(v -> setFilter(UsageInsightCalculator.FILTER_DRYERS));

        updateFilterButtons();
        observeInsights();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        observeInsights();
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

    private void observeInsights() {
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        UsageInsightCalculator.Result result =
                                UsageInsightCalculator.fromSnapshot(snapshot, currentFilter);

                        tvSubtitle.setText(result.totalSessions > 0
                                ? "Based on " + result.totalSessions + " recorded sessions"
                                : "No history found yet for this category");

                        tvPeakValue.setText(result.totalSessions > 0
                                ? result.getPeakLabel()
                                : "—");

                        tvQuietValue.setText(result.totalSessions > 0
                                ? result.getQuietLabel()
                                : "—");

                        tvSessionsValue.setText(String.valueOf(result.totalSessions));

                        heatmapView.setData(result.dayHourCounts);
                        hourlyChartView.setData(result.hourlyAverages);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvSubtitle.setText("Could not load insights");
                        tvPeakValue.setText("—");
                        tvQuietValue.setText("—");
                        tvSessionsValue.setText("0");
                        heatmapView.setData(new int[7][24]);
                        hourlyChartView.setData(new float[24]);
                    }
                });
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