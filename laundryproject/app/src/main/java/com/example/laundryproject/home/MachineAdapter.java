package com.example.laundryproject.home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;

import java.util.List;
import java.util.Locale;

public class MachineAdapter extends RecyclerView.Adapter<MachineAdapter.ViewHolder> {

    public interface OnClickListener {
        void onClick(MachineItem machine);
    }

    private List<MachineItem> items;
    private OnClickListener listener;

    public MachineAdapter(List<MachineItem> items, OnClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_machine, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int pos) {
        MachineItem item = items.get(pos);

        // machineName = "Washer 1" displayed as title
        h.machineName.setText(item.machineName);
        // machineId = "machine_1" shown small grey underneath
        h.machineId.setText(item.machineId);
        h.stateText.setText(item.state);

        GradientDrawable gd =
                (GradientDrawable) h.circle.getBackground();

        // Price from Firebase machine node — fallback 2.50 if not set
        double priceValue = 2.50; // default fallback

        if ( item.price > 0) {
             priceValue = item.price;
       }

        String priceText = String.format(Locale.US, "$%.2f CAD", priceValue);

        if ("RUNNING".equalsIgnoreCase(item.state)) {
            gd.setColor(Color.parseColor("#FF9800"));
            gd.setStroke(6, Color.parseColor("#F57C00"));
            h.stateText.setTextColor(Color.parseColor("#FF9800"));
            h.timer.setVisibility(View.VISIBLE);
            h.timer.setText(formatTime(item.elapsedSeconds));
            h.cost.setText(priceText);

        } else if ("AVAILABLE".equalsIgnoreCase(item.state)) {
            gd.setColor(Color.parseColor("#4CAF50"));
            gd.setStroke(6, Color.parseColor("#1B5E20"));
            h.stateText.setTextColor(Color.parseColor("#4CAF50"));
            h.timer.setVisibility(View.GONE);
            h.cost.setText(priceText);

        } else {
            gd.setColor(Color.parseColor("#9E9E9E"));
            gd.setStroke(6, Color.parseColor("#616161"));
            h.stateText.setTextColor(Color.parseColor("#9E9E9E"));
            h.timer.setVisibility(View.GONE);
            h.cost.setText("—");
        }

        h.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String formatTime(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View     circle;
        TextView machineName, machineId, stateText, timer, cost;

        ViewHolder(View v) {
            super(v);
            circle      = v.findViewById(R.id.cardStatusCircle);
            machineName = v.findViewById(R.id.cardMachineName);
            machineId   = v.findViewById(R.id.cardMachineId);
            stateText   = v.findViewById(R.id.cardStateText);
            timer       = v.findViewById(R.id.cardTimer);
            cost        = v.findViewById(R.id.cardCost);
        }
    }
}
