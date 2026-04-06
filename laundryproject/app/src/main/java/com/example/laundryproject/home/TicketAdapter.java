package com.example.laundryproject.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.model.Ticket;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private final List<Ticket> ticketList;
    private final boolean isAdmin;
    private final OnTicketClickListener listener;

    public TicketAdapter(List<Ticket> ticketList, boolean isAdmin, OnTicketClickListener listener) {
        this.ticketList = ticketList;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = ticketList.get(position);

        holder.tvSubject.setText(ticket.getSubject() != null ? ticket.getSubject() : "No Subject");
        holder.tvCategory.setText("Category: " + safe(ticket.getCategory()));
        holder.tvStatus.setText("Status: " + safe(ticket.getStatus()));
        holder.tvLastMessage.setText("Last: " + safe(ticket.getLastMessage()));
        holder.tvDate.setText(formatTimestamp(ticket.getUpdatedAt()));

        if (isAdmin) {
            holder.tvExtra.setVisibility(View.VISIBLE);
            holder.tvExtra.setText("User: " + safe(ticket.getUserEmail())
                                    + " | Apt: " + safe(ticket.getAptNumber())
                                    + " | Building: " + safe(ticket.getBuildingCode()));
        } else {
            holder.tvExtra.setVisibility(View.GONE);
        }

        holder.cardTicket.setOnClickListener(v -> listener.onTicketClick(ticket));
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "-";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        CardView cardTicket;
        TextView tvSubject, tvCategory, tvStatus, tvLastMessage, tvDate, tvExtra;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTicket = itemView.findViewById(R.id.cardTicket);
            tvSubject = itemView.findViewById(R.id.tvTicketSubject);
            tvCategory = itemView.findViewById(R.id.tvTicketCategory);
            tvStatus = itemView.findViewById(R.id.tvTicketStatus);
            tvLastMessage = itemView.findViewById(R.id.tvTicketLastMessage);
            tvDate = itemView.findViewById(R.id.tvTicketDate);
            tvExtra = itemView.findViewById(R.id.tvTicketExtra);
        }
    }
}