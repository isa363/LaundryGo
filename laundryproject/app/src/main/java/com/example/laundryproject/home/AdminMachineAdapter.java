package com.example.laundryproject.home;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;

import java.util.List;

public class AdminMachineAdapter extends RecyclerView.Adapter<AdminMachineAdapter.ViewHolder> {

    public interface OnMachineClickListener {
        void onClick(MachineItem machine);
    }

    private List<MachineItem> machines;
    private OnMachineClickListener listener;

    public AdminMachineAdapter(List<MachineItem> machines, OnMachineClickListener listener) {
        this.machines = machines;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_machine_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        MachineItem m = machines.get(pos);

        h.name.setText(m.machineName);

        // 1️ Determine state FIRST
        String state = m.state != null ? m.state.toUpperCase() : "DISCONNECTED";

        boolean isActive =
                state.equals("AVAILABLE") ||
                        state.equals("RUNNING");

        int color = isActive
                ? Color.parseColor("#1A56A0")   // ACTIVE
                : Color.parseColor("#BDBDBD");  // INACTIVE

        // 2️ THEN apply color to the drawable
        Drawable bg = h.circle.getBackground();
        if (bg instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) bg.mutate();
            shape.setColor(color);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(m));
    }


    @Override
    public int getItemCount() {
        return machines.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        View circle;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.machineName);
            circle = itemView.findViewById(R.id.machineStatusCircle);
        }
    }

}
