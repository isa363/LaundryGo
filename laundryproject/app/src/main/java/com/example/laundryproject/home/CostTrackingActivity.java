package com.example.laundryproject.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cost_tracking);

        authManager    = new AuthManager();
        userRepository = new UserRepository();

        MaterialToolbar toolbar = findViewById(R.id.costToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Spending");
        }

        tvMonthlyTotal    = findViewById(R.id.tvMonthlyTotal);
        tvSessionCount    = findViewById(R.id.tvSessionCount);
        tvAvgCost         = findViewById(R.id.tvAvgCost);
        lineChart         = findViewById(R.id.lineChart);
        recyclerBreakdown = findViewById(R.id.recyclerBreakdown);

        recyclerBreakdown.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerBreakdown.setAdapter(adapter);

        setupChart();
        loadData();
    }

    private void loadData() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) return;

        userRepository.getUser(currentUser.getUid(),
                new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null) return;
                loadMachineData(user.buildingCode);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(CostTrackingActivity.this,
                        errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMachineData(String userBuilding) {
        FirebaseDatabase.getInstance()
            .getReference("machines")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    historyList.clear();

                    double monthlyTotal = 0;
                    int    sessionCount = 0;

                    Calendar cal = Calendar.getInstance();
                    int currentMonth = cal.get(Calendar.MONTH);
                    int currentYear  = cal.get(Calendar.YEAR);

                    Map<Integer, Double> dailySpending = new TreeMap<>();

                    for (DataSnapshot machine : snapshot.getChildren()) {

                        // Filter by user's building
                        String building = machine.child("buildingCode")
                                .getValue(String.class);
                        if (building == null
                                || !building.equals(userBuilding)) {
                            continue;
                        }

                        // Use machineName
                        String machineName = machine.child("machineName")
                                .getValue(String.class);
                        if (machineName == null) machineName = machine.getKey();

                        // Price from machine node in DB
                        Double machinePrice = machine.child("price")
                                .getValue(Double.class);
                        double price = machinePrice != null ? machinePrice : 2.50;

                        for (DataSnapshot entry :
                                machine.child("history").getChildren()) {
                            Long   epoch    = entry.child("epoch")
                                    .getValue(Long.class);
                            Double duration = entry.child("durationMin")
                                    .getValue(Double.class);
                            // Use stored costUSD if present, else machine price
                            Double storedCost = entry.child("costUSD")
                                    .getValue(Double.class);
                            double cost = storedCost != null
                                    ? storedCost : price;

                            if (epoch == null) continue;

                            double d = duration != null ? duration : 0;

                            Calendar entryCal = Calendar.getInstance();
                            entryCal.setTimeInMillis(epoch * 1000L);

                            if (entryCal.get(Calendar.MONTH) == currentMonth
                                    && entryCal.get(Calendar.YEAR)
                                    == currentYear) {
                                monthlyTotal += cost;
                                sessionCount++;

                                int day = entryCal.get(Calendar.DAY_OF_MONTH);
                                dailySpending.put(day,
                                    dailySpending.getOrDefault(day, 0.0) + cost);

                                historyList.add(
                                    new HistoryItem(machineName, epoch, d, cost));
                            }
                        }
                    }

                    // Sort most recent first
                    Collections.sort(historyList,
                            (a, b) -> Long.compare(b.getEpoch(), a.getEpoch()));

                    double finalTotal = monthlyTotal;
                    int    finalCount = sessionCount;

                    tvMonthlyTotal.setText(String.format(
                            Locale.US, "$%.2f CAD", finalTotal));
                    tvSessionCount.setText(String.valueOf(finalCount));
                    tvAvgCost.setText(finalCount > 0
                        ? String.format(Locale.US, "$%.2f CAD",
                            finalTotal / finalCount)
                        : "$0.00 CAD");

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
        lineChart.getLegend().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#888888"));

        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setTextColor(Color.parseColor("#888888"));
    }

    private void updateChart(Map<Integer, Double> dailySpending) {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : dailySpending.entrySet()) {
            entries.add(new Entry(
                    entry.getKey(), entry.getValue().floatValue()));
        }
        if (entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spending");
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
