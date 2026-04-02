package com.example.laundryproject.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.laundryproject.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailDialog extends DialogFragment {

    public interface EmailUpdateListener {
        void onEmailVerificationSent(String pendingEmail);
    }

    private EmailUpdateListener listener;

    public void setEmailUpdateListener(EmailUpdateListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_email, null);

        EditText currentEmail = view.findViewById(R.id.currentEmail);
        EditText currentPassword = view.findViewById(R.id.currentPassword);
        EditText newEmail = view.findViewById(R.id.newEmail);
        Button confirmBtn = view.findViewById(R.id.confirmBtn);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.getEmail() != null) {
            currentEmail.setText(user.getEmail());
            currentEmail.setEnabled(false);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        confirmBtn.setOnClickListener(v -> {
            String password = currentPassword.getText().toString().trim();
            String email = newEmail.getText().toString().trim();

            if (TextUtils.isEmpty(password) || TextUtils.isEmpty(email)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (user == null || user.getEmail() == null) {
                Toast.makeText(getContext(), "No user logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.equalsIgnoreCase(user.getEmail())) {
                newEmail.setError("New email must be different from current email");
                newEmail.requestFocus();
                return;
            }

            confirmBtn.setEnabled(false);

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

            user.reauthenticate(credential)
                    .addOnSuccessListener(unused ->
                            user.verifyBeforeUpdateEmail(email)
                                    .addOnSuccessListener(v2 -> {
                                        Toast.makeText(
                                                getContext(),
                                                "Verification email sent. Check your inbox.",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        if (listener != null) {
                                            listener.onEmailVerificationSent(email);
                                        }

                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {
                                        confirmBtn.setEnabled(true);
                                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    })
                    )
                    .addOnFailureListener(e -> {
                        confirmBtn.setEnabled(true);
                        Toast.makeText(
                                getContext(),
                                "Authentication failed. Check your password.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        });

        return dialog;
    }
}