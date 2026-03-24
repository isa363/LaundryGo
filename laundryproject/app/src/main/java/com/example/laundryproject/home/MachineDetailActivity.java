package com.example.laundryproject.home;

import static android.app.ProgressDialog.show;

import com.example.laundryproject.R;

import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Locale;

public class MachineDetailActivity  extends AppCompatActivity {

    private String machineId;
    private long   epochStart = 0;
    private Button btnNotify;


    private TextView tvStatePill, tvBigTimer, tvStartedAt,
            tvMachineName, tvMachineId, tvCost, tvLastUpdated;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_detail);

        machineId = getIntent().getStringExtra("MACHINE_ID");
        String machineName = getIntent().getStringExtra("MACHINE_NAME");
        epochStart = getIntent().getLongExtra("MACHINE_EPOCH", 0L);

        // Up navigation to home page
        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(machineName);
        }

        tvStatePill   = findViewById(R.id.tvStatePill);
        tvBigTimer    = findViewById(R.id.tvBigTimer);
        tvStartedAt   = findViewById(R.id.tvStartedAt);
        tvMachineName = findViewById(R.id.tvMachineName);
        tvMachineId   = findViewById(R.id.tvMachineId);
        tvCost        = findViewById(R.id.tvCost);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);

        tvMachineName.setText(machineName);
        tvMachineId.setText(machineId);

        Button btnNotify = findViewById(R.id.btnNotifyMe);
        // Check if user already subscribed to this machine
        SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
        boolean alreadySubscribed = prefs.getBoolean(machineId, false);

        if (alreadySubscribed) {
            btnNotify.setText("You'll be notified");
            btnNotify.setEnabled(false);
        }

       btnNotify.setOnClickListener(v -> {
            // Save subscription for this machine
 prefs.edit().putBoolean(machineId, true).apply();
 btnNotify.setText("You'll be notified");
 btnNotify.setEnabled(false);
 Toast.makeText(this, "You will be notified when " + machineName + " is done", Toast.LENGTH_SHORT).show();
 });

        // Read this machine from Firebase
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .child(machineId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String state = snapshot.child("state")
                                .getValue(String.class);
                        Long   epoch = snapshot.child("epoch")
                                .getValue(Long.class);
                        String ts    = snapshot.child("timestamp")
                                .getValue(String.class);
                        Double cost  = snapshot.child("lastSessionCostCAD")
                                .getValue(Double.class);

                        if (state == null) state = "OFF";
                        if (epoch != null) epochStart = epoch;

                        updateUI(state, ts, cost);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                        updateUI("DISCONNECTED", null, null);
                    }
                });
    }

    private void updateUI(String state, String timestamp, Double cost) {
        SimpleDateFormat fmt =
                new SimpleDateFormat("h:mm a", Locale.getDefault());

        if (timestamp != null) tvLastUpdated.setText(timestamp);

        if ("RUNNING".equalsIgnoreCase(state)) {
            tvStatePill.setText("RUNNING");
            tvStatePill.setBackgroundResource(R.drawable.pill_running);
            tvCost.setText(cost != null
                    ? String.format(Locale.US, "$%.2f CAD", cost)
                    : "$2.50 CAD");
            if (epochStart > 0)
                tvStartedAt.setText("Started at "
                        + fmt.format(new Date(epochStart * 1000L)));
            startTimer();

        } else if ("AVAILABLE".equalsIgnoreCase(state)) {
            tvStatePill.setText("AVAILABLE");
            tvStatePill.setBackgroundResource(R.drawable.pill_available);
            tvBigTimer.setText("Done");
            tvCost.setText(cost != null
                    ? String.format(Locale.US, "$%.2f CAD", cost)
                    : "$2.50 CAD");
            tvStartedAt.setText("Ready to use");
            stopTimer();

            // Reset notification button when machine becomes available
            SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean(machineId, false).apply();
            btnNotify.setText("Notify Me When Done");
            btnNotify.setEnabled(true);


        } else {
            tvStatePill.setText("DISCONNECTED");
            tvStatePill.setBackgroundResource(R.drawable.pill_disconnected);
            tvBigTimer.setText("—");
            tvCost.setText("—");
            tvStartedAt.setText("No signal");
            tvLastUpdated.setText("—");
            stopTimer();
        }
    }

    private void startTimer() {
        stopTimer();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (epochStart == 0) return;
                long elapsed = (System.currentTimeMillis() / 1000) - epochStart;
                long h = elapsed / 3600;
                long m = (elapsed % 3600) / 60;
                long s = elapsed % 60;
                tvBigTimer.setText(
                        String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null)
            handler.removeCallbacks(timerRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
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
