package com.example.laundryproject.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class CostTrackingActivity extends AppCompatActivity {

    private TextView tvMonthlyTotal, tvSessionCount, tvAvgCost;
    private LineChart lineChart;
    private RecyclerView recyclerBreakdown;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList = new ArrayList<>();

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost_tracking);

        authManager = new AuthManager();

        MaterialToolbar toolbar = findViewById(R.id.costToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Spending");
        }

        tvMonthlyTotal  = findViewById(R.id.tvMonthlyTotal);
        tvSessionCount  = findViewById(R.id.tvSessionCount);
        tvAvgCost       = findViewById(R.id.tvAvgCost);
        lineChart       = findViewById(R.id.lineChart);
        recyclerBreakdown = findViewById(R.id.recyclerBreakdown);

        recyclerBreakdown.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerBreakdown.setAdapter(adapter);

        setupChart();
        loadData();
    }

    private void loadData() {
        FirebaseDatabase.getInstance()
            .getReference("machines")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    historyList.clear();

                    double monthlyTotal = 0;
                    int sessionCount    = 0;

                    // Get current month and year
                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH);
                    int currentYear  = cal.get(Calendar.YEAR);

                    // Daily spending map for chart — key = day of month
                    Map<Integer, Double> dailySpending = new TreeMap<>();

                    for (DataSnapshot machine : snapshot.getChildren()) {
                        String machineId   = machine.getKey();
                        String machineName = machine.child("machineName")
                                .getValue(String.class);
                        if (machineName == null) machineName = machineId;

                        for (DataSnapshot entry :
                                machine.child("history").getChildren()) {
                            Long   epoch    = entry.child("epoch")
                                    .getValue(Long.class);
                            Double duration = entry.child("durationMin")
                                    .getValue(Double.class);
                            Double cost     = entry.child("costUSD")
                                    .getValue(Double.class);

                            if (epoch == null) continue;

                            double c = cost != null ? cost : 0;
                            double d = duration != null ? duration : 0;

                            // Filter by current month only
                            Calendar entryCal = Calendar.getInstance();
                            entryCal.setTimeInMillis(epoch * 1000L);
                            int entryMonth = entryCal.get(Calendar.MONTH);
                            int entryYear  = entryCal.get(Calendar.YEAR);

                            if (entryMonth == currentMonth
                                    && entryYear == currentYear) {
                                monthlyTotal += c;
                                sessionCount++;

                                // Add to daily map
                                int day = entryCal.get(Calendar.DAY_OF_MONTH);
                                dailySpending.put(day,
                                    dailySpending.getOrDefault(day, 0.0) + c);

                                historyList.add(0, new HistoryItem(machineName, epoch, d, c));
                            }
                        }
                    }

                    // Update summary cards
                    double finalMonthlyTotal = monthlyTotal;
                    int finalSessionCount    = sessionCount;

                    tvMonthlyTotal.setText(String.format(
                            Locale.US, "$%.2f CAD", finalMonthlyTotal));
                    tvSessionCount.setText(String.valueOf(finalSessionCount));
                    tvAvgCost.setText(finalSessionCount > 0
                        ? String.format(Locale.US, "$%.2f CAD",
                            finalMonthlyTotal / finalSessionCount)
                        : "$0.00 CAD");

                    // Update chart
                    updateChart(dailySpending);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError error) {}
            });
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getXAxis().setTextColor(Color.parseColor("#888888"));
        lineChart.getAxisLeft().setTextColor(Color.parseColor("#888888"));
        lineChart.getLegend().setEnabled(false);
    }

    private void updateChart(Map<Integer, Double> dailySpending) {
        List<.Entry> entries = new ArrayList<>();

        for (Map.Entry<Integer, Double> entry : dailySpending.entrySet()) {
            entries.add(new Entry(entry.getKey(), entry.getValue().floatValue()));
        }

        if (entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spending");

        // Stock chart style
        dataSet.setColor(Color.parseColor("#1a56a0"));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#1a56a0"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#1a56a0"));
        dataSet.setFillAlpha(30);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
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
