package com.example.laundryproject.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> items;

    public HistoryAdapter(List<HistoryItem> items) {
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ViewHolder h, int pos) {
        HistoryItem item = items.get(pos);

        h.machineName.setText(item.machineName);

        // Convert epoch to local date and time automatically
        h.date.setText(DateFormat.getDateTimeInstance(
                        DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(item.epochSeconds * 1000L)));

        h.duration.setText(
                String.format(Locale.US, "%.0f min", item.durationMin));

        // Cost from Firebase displayed in CAD
        h.cost.setText(
                String.format(Locale.US, "$%.2f CAD", item.costUSD));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView machineName, date, duration, cost;

        ViewHolder(View v) {
            super(v);
            machineName = v.findViewById(R.id.histMachineName);
            date        = v.findViewById(R.id.histDate);
            duration    = v.findViewById(R.id.histDuration);
            cost        = v.findViewById(R.id.histCost);
        }
    }

}
