package com.example.laundryproject.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminViewTicketsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewTickets;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private LinearLayout emptyContainer;
    private Toolbar toolbar;

    private TicketAdapter ticketAdapter;
    private final List<Ticket> ticketList = new ArrayList<>();

    private TicketRepository ticketRepository;
    private ListenerRegistration ticketListener;

    private String adminBuilding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_tickets);

        toolbar = findViewById(R.id.toolbarAdminTickets);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Building Tickets");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setTitleTextColor(Color.BLACK);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerViewTickets = findViewById(R.id.recyclerViewAdminTickets);
        progressBar = findViewById(R.id.progressAdminTickets);
        tvEmpty = findViewById(R.id.tvEmptyAdminTickets);
        emptyContainer = findViewById(R.id.tvEmptyAdminTicketsContainer);

        adminBuilding = getIntent().getStringExtra("adminBuilding");

        if (adminBuilding == null || adminBuilding.trim().isEmpty()) {
            Toast.makeText(this, "Admin building not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ticketRepository = new TicketRepository();

        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));
        ticketAdapter = new TicketAdapter(ticketList, true, ticket -> {
            Intent intent = new Intent(AdminViewTicketsActivity.this, TicketChatActivity.class);
            intent.putExtra("ticketId", ticket.getTicketId());
            intent.putExtra("isAdmin", true);
            startActivity(intent);
        });
        recyclerViewTickets.setAdapter(ticketAdapter);

        loadTickets();
    }

    private void loadTickets() {
        progressBar.setVisibility(View.VISIBLE);
        emptyContainer.setVisibility(View.GONE);

        ticketListener = ticketRepository.listenForBuildingTickets(adminBuilding, new TicketRepository.TicketListCallback() {
            @Override
            public void onSuccess(List<Ticket> tickets) {
                progressBar.setVisibility(View.GONE);
                ticketList.clear();
                ticketList.addAll(tickets);
                ticketAdapter.notifyDataSetChanged();
                emptyContainer.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String error) {
                progressBar.setVisibility(View.GONE);
                emptyContainer.setVisibility(ticketList.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(AdminViewTicketsActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
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