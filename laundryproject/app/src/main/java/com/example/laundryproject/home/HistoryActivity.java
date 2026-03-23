package com.example.laundryproject.home; 
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private TextView tvTotalSessions, tvTotalSpent;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        MaterialToolbar toolbar = findViewById(R.id.historyToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Session History");
        }

        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvTotalSpent    = findViewById(R.id.tvTotalSpent);

        RecyclerView recycler = findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recycler.setAdapter(adapter);

        // Read history from all machines
        FirebaseDatabase.getInstance()
                .getReference("machines")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        historyList.clear();
                        double total = 0;

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

                                if (epoch != null) {
                                    double c = cost != null ? cost : 0;
                                    double d = duration != null ? duration : 0;
                                    historyList.add(0,
                                            new HistoryItem(machineName, epoch, d, c));
                                    total += c;
                                }
                            }
                        }

                        tvTotalSessions.setText(String.valueOf(historyList.size()));
                        tvTotalSpent.setText(
                                String.format(Locale.US, "$%.2f CAD", total));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
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
