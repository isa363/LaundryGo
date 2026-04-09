package com.example.laundryproject.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.data.TicketRepository;
import com.example.laundryproject.model.Ticket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewTickets;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Toolbar toolbar;

    private TicketAdapter ticketAdapter;
    private final List<Ticket> ticketList = new ArrayList<>();

    private TicketRepository ticketRepository;
    private ListenerRegistration ticketListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tickets);

        toolbar = findViewById(R.id.toolbarMyTickets);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Tickets");
            toolbar.setTitleTextColor(android.graphics.Color.BLACK);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerViewTickets = findViewById(R.id.recyclerViewMyTickets);
        progressBar = findViewById(R.id.progressMyTickets);
        tvEmpty = findViewById(R.id.tvEmptyMyTickets);

        ticketRepository = new TicketRepository();

        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new TicketAdapter(ticketList, false, ticket -> {
            Intent intent = new Intent(MyTicketsActivity.this, TicketChatActivity.class);
            intent.putExtra("ticketId", ticket.getTicketId());
            intent.putExtra("isAdmin", false);
            startActivity(intent);
        });
        recyclerViewTickets.setAdapter(ticketAdapter);

        loadTickets();
    }

    private void loadTickets() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        ticketListener = ticketRepository.listenForUserTickets(uid, new TicketRepository.TicketListCallback() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                progressBar.setVisibility(View.GONE);
                ticketList.clear();
                ticketList.addAll(tickets);
                ticketAdapter.notifyDataSetChanged();
                tvEmpty.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MyTicketsActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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