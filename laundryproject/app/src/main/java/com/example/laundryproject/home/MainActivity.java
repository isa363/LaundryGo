package com.example.laundryproject.home;

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
    private final List<MachineItem> machineList = new ArrayList<>();
    private final Map<String, String> previousStates = new HashMap<>();

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private AuthManager authManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authManager = new AuthManager();
        userRepository = new UserRepository();

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvAvatar = findViewById(R.id.tvAvatar);

        recyclerMachines = findViewById(R.id.recyclerMachines);
        recyclerMachines.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MachineAdapter(machineList, machine -> {
            Intent intent = new Intent(this, MachineDetailActivity.class);
            intent.putExtra("MACHINE_ID", machine.machineId);
            intent.putExtra("MACHINE_NAME", machine.machineName);
            startActivity(intent);
        });

        recyclerMachines.setAdapter(adapter);

        BottomNavHelper.setup(this, R.id.bottomNav, R.id.nav_machines);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            String username = user.getEmail().split("@")[0];
            tvWelcome.setText("Welcome, " + username);
            tvAvatar.setText(username.substring(0, 1).toUpperCase());
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
                                machineList.clear();

                                for (DataSnapshot child : snapshot.getChildren()) {
                                    String building = child.child("buildingCode").getValue(String.class);

                                    if (building == null || !building.equals(user.buildingCode)) {
                                        continue;
                                    }

                                    String id = child.getKey();
                                    String state = child.child("state").getValue(String.class);
                                    String name = child.child("machineName").getValue(String.class);

                                    if (state == null) state = "DISCONNECTED";
                                    if (name == null) name = id;

                                    machineList.add(new MachineItem(id, name, state, 0L, ""));
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
            authManager.signOut();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}