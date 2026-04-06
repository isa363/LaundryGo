package com.example.laundryproject.home;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.model.TicketMessage;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TicketMessageAdapter extends RecyclerView.Adapter<TicketMessageAdapter.MessageViewHolder> {

    private final List<TicketMessage> messages;

    public TicketMessageAdapter(List<TicketMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        TicketMessage message = messages.get(position);
        boolean isAdmin = "admin".equalsIgnoreCase(message.getSenderType());

        holder.tvSender.setText(isAdmin ? "Admin" : "User");
        holder.tvMessage.setText(message.getMessage() != null ? message.getMessage() : "");
        holder.tvTime.setText(formatTimestamp(message.getTimestamp()));

        LinearLayout.LayoutParams rootParams = (LinearLayout.LayoutParams) holder.cardMessage.getLayoutParams();
        if (isAdmin) {
            rootParams.gravity = Gravity.START;
        } else {
            rootParams.gravity = Gravity.END;
        }
        holder.cardMessage.setLayoutParams(rootParams);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(timestamp.toDate());
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CardView cardMessage;
        TextView tvSender, tvMessage, tvTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMessage = itemView.findViewById(R.id.cardMessage);
            tvSender = itemView.findViewById(R.id.tvSenderType);
            tvMessage = itemView.findViewById(R.id.tvMessageText);
            tvTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}