package com.example.laundryproject.home;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.laundryproject.R;
import com.example.laundryproject.data.TicketRepository;

public class SubmitTicketActivity extends AppCompatActivity {

    private EditText etSubject, etMessage;
    private Spinner spinnerCategory;
    private Button btnSubmitTicket;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private TicketRepository ticketRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_ticket);

        toolbar = findViewById(R.id.toolbarSubmitTicket);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Submit Ticket");
            toolbar.setTitleTextColor(Color.BLACK);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        etSubject = findViewById(R.id.etTicketSubject);
        etMessage = findViewById(R.id.etTicketMessage);
        spinnerCategory = findViewById(R.id.spinnerTicketCategory);
        btnSubmitTicket = findViewById(R.id.btnSubmitTicket);
        progressBar = findViewById(R.id.progressSubmitTicket);

        ticketRepository = new TicketRepository();

        setupSpinner();

        btnSubmitTicket.setOnClickListener(v -> submitTicket());
    }

    private void setupSpinner() {
        String[] categories = {
                "Machine Issue",
                "Payment Issue",
                "Account Issue",
                "General Question",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(16f);
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                textView.setTextColor(Color.WHITE);
                textView.setTextSize(16f);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void submitTicket() {
        String subject = etSubject.getText().toString().trim();
        String message = etMessage.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (TextUtils.isEmpty(subject)) {
            etSubject.setError("Subject is required");
            etSubject.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(message)) {
            etMessage.setError("Message is required");
            etMessage.requestFocus();
            return;
        }

        setLoading(true);

        ticketRepository.createTicket(subject, category, message, new TicketRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(SubmitTicketActivity.this, "Ticket submitted successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String error) {
                setLoading(false);
                Toast.makeText(SubmitTicketActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSubmitTicket.setEnabled(!isLoading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}