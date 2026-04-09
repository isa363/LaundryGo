package com.example.laundryproject.home;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AdminViewMachinesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminMachineAdapter adapter;

    private DatabaseReference machinesRef;

    private List<MachineItem> machineList = new ArrayList<>();

    private String adminBuilding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_machines);

        adminBuilding = getIntent().getStringExtra("adminBuilding");

        MaterialToolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationIconTint(getResources().getColor(android.R.color.white));

        FloatingActionButton fab = findViewById(R.id.fabAddMachine);
        fab.setOnClickListener(v -> showAddMachineDialog());

        machinesRef = FirebaseDatabase.getInstance().getReference("machines");

        recyclerView = findViewById(R.id.rvMachines);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AdminMachineAdapter(machineList, this::showMachineActionsDialog);
        recyclerView.setAdapter(adapter);

        loadMachines();
    }

    private void loadMachines() {
        machinesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                machineList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {

                    if (!child.hasChild("buildingCode") && !child.hasChild("machineName")) {
                        continue;
                    }

                    String id = child.child("machineId").getValue(String.class);
                    if (id == null) {
                        id = child.child("machineID").getValue(String.class);
                    }
                    if (id == null) {
                        id = child.getKey();
                    }

                    String name = child.child("machineName").getValue(String.class);
                    String state = child.child("state").getValue(String.class);
                    String building = child.child("buildingCode").getValue(String.class);
                    String timestamp = child.child("timestamp").getValue(String.class);

                    Long epoch = child.child("epochStart").getValue(Long.class);
                    if (epoch == null) {
                        epoch = child.child("epoch").getValue(Long.class);
                    }
                    if (epoch == null) {
                        epoch = 0L;
                    }

                    Double price = child.child("price").getValue(Double.class);
                    if (price == null) {
                        price = 0.0;
                    }

                    if (building == null || !building.equals(adminBuilding)) {
                        continue;
                    }

                    MachineItem m = new MachineItem(id, name, state, epoch, timestamp, price, building);
                    machineList.add(m);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AdminViewMachinesActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMachineActionsDialog(MachineItem m) {

        String message =
                "Machine Name: " + m.machineName + "\n" +
                        "State: " + (m.state != null ? m.state : "N/A") + "\n" +
                        "Price: $" + m.price;


        new AlertDialog.Builder(this)
                .setTitle("Machine Options")
                .setMessage(message)
                .setNegativeButton("Edit Machine", (d, w) -> showEditMachineDialog(m))
                .setPositiveButton("Delete Machine", (d, w) -> deleteMachine(m.machineId))
                .setNeutralButton("Close", null)
                .show();
    }

    private void showEditMachineDialog(MachineItem m) {

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Machine Name");
        nameInput.setText(m.machineName);

        final EditText priceInput = new EditText(this);
        priceInput.setHint("Price per cycle");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        priceInput.setText(String.valueOf(m.price));

        // NEW: In Service switch
        final Switch serviceSwitch = new Switch(this);
        serviceSwitch.setText("In Service");

        String state = m.state != null ? m.state.toUpperCase() : "DISCONNECTED";
        boolean isInService =
                state.equals("AVAILABLE") ||
                        state.equals("RUNNING");

        serviceSwitch.setChecked(isInService);

        // Apply tint styling
        serviceSwitch.getThumbDrawable().setTint(isInService ? 0xFF1A56A0 : 0xFFBDBDBD);
        serviceSwitch.getTrackDrawable().setTint(isInService ? 0x801A56A0 : 0x80BDBDBD);

        serviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            serviceSwitch.getThumbDrawable().setTint(isChecked ? 0xFF1A56A0 : 0xFFBDBDBD);
            serviceSwitch.getTrackDrawable().setTint(isChecked ? 0x801A56A0 : 0x80BDBDBD);
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(nameInput);
        layout.addView(priceInput);
        layout.addView(serviceSwitch);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Machine")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                String newName = nameInput.getText().toString().trim();
                String priceText = priceInput.getText().toString().trim();

                if (newName.isEmpty() || priceText.isEmpty()) {
                    Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
                    return;
                }

                double newPrice = Double.parseDouble(priceText);

                boolean inService = serviceSwitch.isChecked();
                String newState = inService ? "AVAILABLE" : "DISCONNECTED";

                // Update Firebase
                machinesRef.child(m.machineId).child("machineName").setValue(newName);
                machinesRef.child(m.machineId).child("price").setValue(newPrice);
                machinesRef.child(m.machineId).child("state").setValue(newState);

                Toast.makeText(this, "Machine updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void deleteMachine(String id) {
        machinesRef.child(id).removeValue()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Machine deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showAddMachineDialog() {

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Machine Name");

        final EditText priceInput = new EditText(this);
        priceInput.setHint("Price per cycle");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);
        layout.addView(nameInput);
        layout.addView(priceInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Machine")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {

                    String name = nameInput.getText().toString().trim();
                    double price = Double.parseDouble(priceInput.getText().toString().trim());

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Machine name required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    machinesRef.get().addOnSuccessListener(snapshot -> {
                        long count = snapshot.getChildrenCount();
                        String newId = "machine_" + (count + 1);

                        MachineItem m = new MachineItem(
                                newId,
                                name,
                                "DISCONNECTED",
                                0L,
                                "",
                                price,
                                adminBuilding
                        );

                        machinesRef.child(newId).setValue(m)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Machine added", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                    });

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
