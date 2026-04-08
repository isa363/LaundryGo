package com.example.laundryproject.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private TextView tvTotalRevenue, tvTotalCycles, tvPeakHour;
    private BarChart barChartRevenue;
    private BarChart barChartPeakHours;
    private LineChart lineChartMonthly;

    private AuthManager authManager;
    private UserRepository userRepository;
    private String adminBuilding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        authManager = new AuthManager();
        userRepository = new UserRepository();

        MaterialToolbar toolbar = findViewById(R.id.analyticsToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Analytics Dashboard");
        }

        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvTotalCycles = findViewById(R.id.tvTotalCycles);
        tvPeakHour = findViewById(R.id.tvPeakHour);
        barChartRevenue = findViewById(R.id.barChartRevenue);
        barChartPeakHours = findViewById(R.id.barChartPeakHours);
        lineChartMonthly = findViewById(R.id.lineChartMonthly);

        setupCharts();
        getAdminBuildingThenLoad();
    }

    private void getAdminBuildingThenLoad() {
        if (authManager.getCurrentUser() == null) return;

        userRepository.getUser(authManager.getCurrentUser().getUid(),
                new UserRepository.LoadUserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        if (user == null) return;
                        adminBuilding = user.buildingCode;
                        loadAnalytics();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                    }
                });
    }

    private void loadAnalytics() {
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        double totalRevenue = 0;
                        int totalCycles = 0;

                        Map<String, Double> revenuePerMachine = new TreeMap<>();
                        Map<Integer, Integer> cyclesPerHour = new TreeMap<>();
                        Map<Integer, Double> dailyRevenue = new TreeMap<>();

                        Calendar now = Calendar.getInstance();
                        int currentMonth = now.get(Calendar.MONTH);
                        int currentYear = now.get(Calendar.YEAR);

                        for (DataSnapshot machine : snapshot.getChildren()) {

                            String building = machine.child("buildingCode").getValue(String.class);
                            if (adminBuilding != null
                                    && !adminBuilding.isEmpty()
                                    && (building == null || !building.equals(adminBuilding))) {
                                continue;
                            }

                            String machineName = machine.child("machineName").getValue(String.class);
                            if (machineName == null || machineName.trim().isEmpty()) {
                                machineName = machine.getKey();
                            }

                            Double machinePrice = machine.child("price").getValue(Double.class);
                            double machineRevenue = 0;

                            for (DataSnapshot entry : machine.child("history").getChildren()) {
                                Long epoch = entry.child("epoch").getValue(Long.class);
                                Double storedCost = entry.child("costUSD").getValue(Double.class);

                                if (epoch == null) {
                                    continue;
                                }

                                if (storedCost == null && machinePrice == null) {
                                    continue;
                                }

                                double cost = storedCost != null ? storedCost : machinePrice;

                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(epoch * 1000L);

                                totalRevenue += cost;
                                machineRevenue += cost;
                                totalCycles++;

                                int hour = cal.get(Calendar.HOUR_OF_DAY);
                                cyclesPerHour.put(hour,
                                        cyclesPerHour.getOrDefault(hour, 0) + 1);

                                if (cal.get(Calendar.MONTH) == currentMonth
                                        && cal.get(Calendar.YEAR) == currentYear) {
                                    int day = cal.get(Calendar.DAY_OF_MONTH);
                                    dailyRevenue.put(day,
                                            dailyRevenue.getOrDefault(day, 0.0) + cost);
                                }
                            }

                            revenuePerMachine.put(machineName, machineRevenue);
                        }

                        int peakHour = 0;
                        int maxCycles = 0;
                        for (Map.Entry<Integer, Integer> e : cyclesPerHour.entrySet()) {
                            if (e.getValue() > maxCycles) {
                                maxCycles = e.getValue();
                                peakHour = e.getKey();
                            }
                        }

                        tvTotalRevenue.setText(String.format(Locale.US, "$%.2f CAD", totalRevenue));
                        tvTotalCycles.setText(String.valueOf(totalCycles));
                        tvPeakHour.setText(totalCycles == 0
                                ? "No data"
                                : String.format(Locale.US, "%02d:00 - %02d:00", peakHour, (peakHour + 1) % 24));

                        updateRevenueChart(revenuePerMachine);
                        updatePeakHoursChart(cyclesPerHour);
                        updateMonthlyChart(dailyRevenue);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                    }
                });
    }

    private void setupCharts() {
        setupBarChart(barChartRevenue);
        setupBarChart(barChartPeakHours);

        lineChartMonthly.getDescription().setEnabled(false);
        lineChartMonthly.getAxisRight().setEnabled(false);
        lineChartMonthly.getLegend().setEnabled(false);
        lineChartMonthly.getAxisLeft().setAxisMinimum(0f);
        lineChartMonthly.getAxisLeft().setTextColor(Color.parseColor("#888888"));
        lineChartMonthly.setTouchEnabled(true);

        XAxis xAxis = lineChartMonthly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#888888"));
    }

    private void setupBarChart(BarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisLeft().setTextColor(Color.parseColor("#888888"));
        chart.setTouchEnabled(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#888888"));
    }

    private void updateRevenueChart(Map<String, Double> revenuePerMachine) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int i = 0;
        for (Map.Entry<String, Double> e : revenuePerMachine.entrySet()) {
            entries.add(new BarEntry(i, e.getValue().floatValue()));
            labels.add(e.getKey());
            i++;
        }

        if (entries.isEmpty()) {
            barChartRevenue.clear();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Revenue per Machine");
        dataSet.setColor(Color.parseColor("#1a56a0"));
        dataSet.setValueTextColor(Color.parseColor("#1A1A1A"));
        dataSet.setValueTextSize(10f);

        barChartRevenue.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChartRevenue.setData(new BarData(dataSet));
        barChartRevenue.invalidate();
    }

    private void updatePeakHoursChart(Map<Integer, Integer> cyclesPerHour) {
        List<BarEntry> entries = new ArrayList<>();

        for (int h = 0; h < 24; h++) {
            entries.add(new BarEntry(h, cyclesPerHour.getOrDefault(h, 0)));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Cycles per Hour");
        dataSet.setColor(Color.parseColor("#FF9800"));
        dataSet.setDrawValues(false);

        barChartPeakHours.setData(new BarData(dataSet));
        barChartPeakHours.invalidate();
    }

    private void updateMonthlyChart(Map<Integer, Double> dailyRevenue) {
        List<Entry> entries = new ArrayList<>();

        for (Map.Entry<Integer, Double> e : dailyRevenue.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue().floatValue()));
        }

        if (entries.isEmpty()) {
            lineChartMonthly.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Revenue");
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

        lineChartMonthly.setData(new LineData(dataSet));
        lineChartMonthly.invalidate();
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