package com.example.laundryproject.home;

import com.example.laundryproject.FcmTokenHelper;
import com.example.laundryproject.R;
import com.example.laundryproject.auth.AuthManager;
import com.example.laundryproject.auth.LoginActivity;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerMachines;
    private TextView tvWelcome, tvAvatar;

    private MachineAdapter adapter;
    private List<MachineItem> machineList = new ArrayList<>();
    private Map<String, String> previousStates = new HashMap<>();

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private long lastDataReceivedAt = 0;
    private static final long STALE_TIMEOUT_MS = 120000;
    private static final long CHECK_INTERVAL_MS = 5000;
    private final Handler staleHandler = new Handler(Looper.getMainLooper());

    private AuthManager authManager;
    private UserRepository userRepository;

    private final Runnable staleCheckRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            if (lastDataReceivedAt != 0 && now - lastDataReceivedAt > STALE_TIMEOUT_MS) {
                for (MachineItem item : machineList) {
                    if (!"AVAILABLE".equalsIgnoreCase(item.state)) {
                        item.state = "DISCONNECTED";
                    }
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
            staleHandler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager(); // only used for logout
        userRepository = new UserRepository();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvAvatar = findViewById(R.id.tvAvatar);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null && firebaseUser.getEmail() != null) {
            String username = firebaseUser.getEmail().split("@")[0];
            String initial = String.valueOf(username.charAt(0)).toUpperCase();
            tvWelcome.setText("Welcome, " + username);
            tvAvatar.setText(initial);
        }

        recyclerMachines = findViewById(R.id.recyclerMachines);
        recyclerMachines.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MachineAdapter(machineList, machine -> {
            Intent intent = new Intent(this, MachineDetailActivity.class);
            intent.putExtra("MACHINE_ID", machine.machineId);
            intent.putExtra("MACHINE_NAME", machine.machineName);
            intent.putExtra("MACHINE_EPOCH",
                    machine.epochStart != null ? machine.epochStart : 0L);
            startActivity(intent);
        });

        recyclerMachines.setAdapter(adapter);

        BottomNavHelper.setup(this, R.id.bottomNav, R.id.nav_machines);

        createNotificationChannel();
        FcmTokenHelper.syncCurrentUserToken();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        loadMachines();
    }

    private void loadMachines() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        userRepository.getUser(firebaseUser.getUid(), new UserRepository.LoadUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user == null || user.buildingCode == null) return;

                FirebaseDatabase.getInstance()
                        .getReference("machines")
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                lastDataReceivedAt = System.currentTimeMillis();
                                machineList.clear();

                                for (DataSnapshot child : snapshot.getChildren()) {
                                    String building = child.child("buildingCode").getValue(String.class);
                                    if (building == null || !building.equals(user.buildingCode)) {
                                        continue;
                                    }

                                    String id = child.getKey();
                                    String state = child.child("state").getValue(String.class);
                                    String name = child.child("machineName").getValue(String.class);
                                    Long epoch = child.child("epoch").getValue(Long.class);
                                    String ts = child.child("timestamp").getValue(String.class);
                                    Double price = child.child("price").getValue(Double.class);
                                    String buildingCode = child.child("buildingCode").getValue(String.class);


                                    if (state == null) state = "DISCONNECTED";
                                    if (name == null) name = id;
                                    if (epoch == null) epoch = 0L;

                                    MachineItem item = new MachineItem( id,  name, state, epoch, ts, price != null ? price : 0.0, buildingCode);
                                    item.lastUpdatedAt = System.currentTimeMillis();
                                    machineList.add(item);

                                    String prev = previousStates.containsKey(id)
                                            ? previousStates.get(id)
                                            : "AVAILABLE";

                                    if ("RUNNING".equalsIgnoreCase(prev)
                                            && "AVAILABLE".equalsIgnoreCase(state)) {

                                        SharedPreferences prefs = getSharedPreferences("notif_prefs", MODE_PRIVATE);
                                        boolean subscribed = prefs.getBoolean(id, false);

                                        if (subscribed) {
                                            showNotification(name + " is done!",
                                                    "Your laundry is ready to pick up.");
                                            prefs.edit().putBoolean(id, false).apply();
                                        }
                                    }

                                    previousStates.put(id, state);
                                }

                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(MainActivity.this,
                                        "Failed to load machines.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

                timerRunnable = new Runnable() {
                    @Override
                    public void run() {
                        long nowSec = System.currentTimeMillis() / 1000;
                        for (MachineItem item : machineList) {
                            if ("RUNNING".equalsIgnoreCase(item.state)
                                    && item.epochStart != null) {
                                item.elapsedSeconds = nowSec - item.epochStart;
                            }
                        }
                        adapter.notifyDataSetChanged();
                        timerHandler.postDelayed(this, 1000);
                    }
                };
                timerHandler.post(timerRunnable);

                staleHandler.post(staleCheckRunnable);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FcmTokenHelper.clearCurrentUserToken();
            authManager.signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        staleHandler.removeCallbacks(staleCheckRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "laundry_channel",
                    "Laundry Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String message) {
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "laundry_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}
