package com.example.laundryproject.home;

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

import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MachineDetailActivity extends AppCompatActivity {

    private String machineId;
    private String machineName;
    private long epochStart = 0;
    private Button btnNotify;

    private TextView tvStatePill, tvBigTimer, tvStartedAt,
            tvMachineName, tvMachineId, tvCost, tvLastUpdated;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private AuthManager authManager;
    private UserRepository userRepository;

    private double machinePrice = -1.0;
    private String currentState = "DISCONNECTED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machine_detail);

        authManager = new AuthManager();
        userRepository = new UserRepository();

        machineId = getIntent().getStringExtra("MACHINE_ID");
        machineName = getIntent().getStringExtra("MACHINE_NAME");
        epochStart = getIntent().getLongExtra("MACHINE_EPOCH", 0L);

        if (machineName == null) machineName = machineId;
        if (machineId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(machineName);
        }

        tvStatePill = findViewById(R.id.tvStatePill);
        tvBigTimer = findViewById(R.id.tvBigTimer);
        tvStartedAt = findViewById(R.id.tvStartedAt);
        tvMachineName = findViewById(R.id.tvMachineName);
        tvMachineId = findViewById(R.id.tvMachineId);
        tvCost = findViewById(R.id.tvCost);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);

        tvMachineName.setText(machineName);
        tvMachineId.setText(machineId);

        btnNotify = findViewById(R.id.btnNotifyMe);

        SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
        boolean alreadySubscribed = prefs.getBoolean(machineId, false);
        if (alreadySubscribed) {
            btnNotify.setText("You'll be notified");
            btnNotify.setEnabled(false);
        }

        btnNotify.setOnClickListener(v -> {
            writeCurrentSessionForMachine(new SessionWriteCallback() {
                @Override
                public void onSuccess() {
                    prefs.edit().putBoolean(machineId, true).apply();
                    btnNotify.setText("You'll be notified");
                    btnNotify.setEnabled(false);
                    Toast.makeText(
                            MachineDetailActivity.this,
                            "You will be notified when " + machineName + " is done",
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(
                            MachineDetailActivity.this,
                            errorMessage,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });

        FirebaseDatabase.getInstance()
                .getReference("machines")
                .child(machineId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {
                            String state = snapshot.child("state").getValue(String.class);
                            Long epoch = snapshot.child("epoch").getValue(Long.class);
                            String ts = snapshot.child("timestamp").getValue(String.class);
                            Double price = snapshot.child("price").getValue(Double.class);

                            if (state == null) state = "DISCONNECTED";
                            if (epoch != null) epochStart = epoch;
                            if (price != null) machinePrice = price;

                            currentState = state;
                            updateUI(state, ts, machinePrice);
                        } catch (Exception e) {
                            android.util.Log.e("DETAIL_ERROR",
                                    e.getMessage() != null ? e.getMessage() : "Unknown detail error");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        updateUI("DISCONNECTED", null, null);
                    }
                });
    }

    private void writeCurrentSessionForMachine(SessionWriteCallback callback) {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("No user logged in");
            return;
        }

        userRepository.getUser(currentUser.getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null) {
                    callback.onFailure("User not found");
                    return;
                }

                long now = System.currentTimeMillis() / 1000L;

                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("uid", currentUser.getUid());
                sessionData.put("aptNumber", user.aptNumber);
                sessionData.put("buildingCode", user.buildingCode);
                sessionData.put("username", user.username != null ? user.username : "");
                sessionData.put("machineId", machineId);
                sessionData.put("machineName", machineName);
                sessionData.put("price", machinePrice >= 0 ? machinePrice : null);
                sessionData.put("startEpoch", now);

                FirebaseDatabase.getInstance()
                        .getReference("machines")
                        .child(machineId)
                        .child("currentSession")
                        .setValue(sessionData)
                        .addOnSuccessListener(unused -> callback.onSuccess())
                        .addOnFailureListener(e -> callback.onFailure(
                                e.getMessage() != null
                                        ? e.getMessage()
                                        : "Failed to save machine session"
                        ));
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    private void updateUI(String state, String timestamp, Double costOrPrice) {
        SimpleDateFormat fmt = new SimpleDateFormat("h:mm a", Locale.getDefault());

        if (timestamp != null) {
            tvLastUpdated.setText(timestamp);
        }

        if ("RUNNING".equalsIgnoreCase(state)) {
            tvStatePill.setText("RUNNING");
            tvStatePill.setBackgroundResource(R.drawable.pill_running);
            tvCost.setText(costOrPrice != null
                    ? String.format(Locale.US, "$%.2f CAD", costOrPrice)
                    : "—");

            if (epochStart > 0) {
                tvStartedAt.setText("Started at " + fmt.format(new Date(epochStart * 1000L)));
            } else {
                tvStartedAt.setText("Running");
            }

            startTimer();

        } else if ("AVAILABLE".equalsIgnoreCase(state)) {
            tvStatePill.setText("AVAILABLE");
            tvStatePill.setBackgroundResource(R.drawable.pill_available);
            tvBigTimer.setText("Done");
            tvCost.setText(costOrPrice != null
                    ? String.format(Locale.US, "$%.2f CAD", costOrPrice)
                    : "—");
            tvStartedAt.setText("Ready to use");
            stopTimer();

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

                long elapsed = (System.currentTimeMillis() / 1000L) - epochStart;
                long h = elapsed / 3600;
                long m = (elapsed % 3600) / 60;
                long s = elapsed % 60;

                tvBigTimer.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));
                handler.postDelayed(this, 1000);
            }
        };

        handler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
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

    private interface SessionWriteCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}