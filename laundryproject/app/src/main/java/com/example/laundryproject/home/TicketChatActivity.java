package com.example.laundryproject.home;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.data.TicketRepository;
import com.example.laundryproject.model.Ticket;
import com.example.laundryproject.model.TicketMessage;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class TicketChatActivity extends AppCompatActivity {

    private TextView tvChatSubject, tvChatStatus, tvChatInfo;
    private RecyclerView recyclerViewMessages;
    private EditText etReplyMessage;
    private ImageButton btnSendReply;
    private Button btnCloseTicket;
    private Button btnDeleteTicket;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private TicketRepository ticketRepository;
    private TicketMessageAdapter messageAdapter;
    private final List<TicketMessage> messageList = new ArrayList<>();

    private ListenerRegistration messageListener;
    private ListenerRegistration ticketListener;

    private String ticketId;
    private boolean isAdmin = false;
    private Ticket currentTicket;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_chat);

        toolbar = findViewById(R.id.toolbarTicketChat);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ticket Details");
            toolbar.setTitleTextColor(Color.BLACK);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        tvChatSubject = findViewById(R.id.tvChatSubject);
        tvChatStatus = findViewById(R.id.tvChatStatus);
        tvChatInfo = findViewById(R.id.tvChatInfo);
        recyclerViewMessages = findViewById(R.id.recyclerViewTicketMessages);
        etReplyMessage = findViewById(R.id.etReplyMessage);
        btnSendReply = findViewById(R.id.btnSendReply);
        btnCloseTicket = findViewById(R.id.btnCloseTicket);
        btnDeleteTicket = findViewById(R.id.btnDeleteTicket);
        progressBar = findViewById(R.id.progressTicketChat);

        ticketRepository = new TicketRepository();

        ticketId = getIntent().getStringExtra("ticketId");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        if (ticketId == null || ticketId.trim().isEmpty()) {
            Toast.makeText(this, "Invalid ticket.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new TicketMessageAdapter(messageList);
        recyclerViewMessages.setAdapter(messageAdapter);

        btnCloseTicket.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        btnDeleteTicket.setVisibility(View.GONE);

        btnSendReply.setOnClickListener(v -> sendReply());
        btnCloseTicket.setOnClickListener(v -> closeTicket());
        btnDeleteTicket.setOnClickListener(v -> confirmDeleteTicket());

        listenToTicket();
        listenToMessages();
    }

    private void listenToTicket() {
        progressBar.setVisibility(View.VISIBLE);

        ticketListener = ticketRepository.listenToTicket(ticketId, new TicketRepository.TicketCallback() {
            @Override
            public void onSuccess(Ticket ticket) {
                progressBar.setVisibility(View.GONE);
                currentTicket = ticket;

                tvChatSubject.setText(ticket.getSubject() != null ? ticket.getSubject() : "No Subject");
                tvChatStatus.setText("Status: " + (ticket.getStatus() != null ? ticket.getStatus() : "-"));

                String info;
                if (isAdmin) {
                    info = "User: " + safe(ticket.getUserEmail()) + " | Apt: " + safe(ticket.getAptNumber())
                            + "\nCategory: " + safe(ticket.getCategory())
                            + "\nBuilding: " + safe(ticket.getBuildingCode());
                } else {
                    info = "Category: " + safe(ticket.getCategory())
                            + "\nBuilding: " + safe(ticket.getBuildingCode());
                }
                tvChatInfo.setText(info);

                if (isAdmin) {
                    boolean archived = ticket.isArchived();
                    btnCloseTicket.setVisibility(archived ? View.GONE : View.VISIBLE);
                    btnDeleteTicket.setVisibility(archived ? View.VISIBLE : View.GONE);
                }

                boolean isClosed = "closed".equalsIgnoreCase(ticket.getStatus()) || ticket.isArchived();
                etReplyMessage.setEnabled(!isClosed);
                btnSendReply.setEnabled(!isClosed);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TicketChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void listenToMessages() {
        messageListener = ticketRepository.listenForMessages(ticketId, new TicketRepository.MessageListCallback() {
            @Override
            public void onSuccess(List<TicketMessage> messages) {
                messageList.clear();
                messageList.addAll(messages);
                messageAdapter.notifyDataSetChanged();

                if (!messageList.isEmpty()) {
                    recyclerViewMessages.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(TicketChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendReply() {
        String reply = etReplyMessage.getText().toString().trim();

        if (TextUtils.isEmpty(reply)) {
            etReplyMessage.setError("Message is required");
            etReplyMessage.requestFocus();
            return;
        }

        setSending(true);

        ticketRepository.sendMessage(ticketId, reply, isAdmin, new TicketRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setSending(false);
                etReplyMessage.setText("");
                Toast.makeText(TicketChatActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String error) {
                setSending(false);
                Toast.makeText(TicketChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void closeTicket() {
        ticketRepository.updateTicketStatus(ticketId, "closed", new TicketRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(TicketChatActivity.this, "Ticket closed and archived", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(TicketChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDeleteTicket() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Ticket")
                .setMessage("Delete this closed ticket permanently?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTicket())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTicket() {
        ticketRepository.deleteTicket(ticketId, new TicketRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(TicketChatActivity.this, "Ticket deleted", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(TicketChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setSending(boolean isSending) {
        btnSendReply.setEnabled(!isSending);
        etReplyMessage.setEnabled(!isSending);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
        if (ticketListener != null) {
            ticketListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}