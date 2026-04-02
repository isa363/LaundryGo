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

public class PassDialog extends DialogFragment {

    public interface PasswordUpdateListener {
        void onPasswordUpdated();
    }

    private PasswordUpdateListener listener;

    public void setPasswordUpdateListener(PasswordUpdateListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pass, null);

        EditText currentPassword = view.findViewById(R.id.currentPassword);
        EditText newPassword = view.findViewById(R.id.newPassword);
        EditText confirmNewPassword = view.findViewById(R.id.confirmNewPassword);
        Button confirmBtn = view.findViewById(R.id.confirmBtn);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        confirmBtn.setOnClickListener(v -> {
            String currentPass = currentPassword.getText().toString().trim();
            String newPass = newPassword.getText().toString().trim();
            String confirmPass = confirmNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                newPassword.setError("Password must be at least 6 characters");
                newPassword.requestFocus();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                confirmNewPassword.setError("Passwords do not match");
                confirmNewPassword.requestFocus();
                return;
            }

            if (user == null || user.getEmail() == null) {
                Toast.makeText(getContext(), "No user logged in.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.equals(currentPass)) {
                newPassword.setError("New password must be different");
                newPassword.requestFocus();
                return;
            }

            confirmBtn.setEnabled(false);

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential)
                    .addOnSuccessListener(unused ->
                            user.updatePassword(newPass)
                                    .addOnSuccessListener(v2 -> {
                                        Toast.makeText(
                                                getContext(),
                                                "Password updated successfully.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        if (listener != null) {
                                            listener.onPasswordUpdated();
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
                        currentPassword.setError("Current password is incorrect");
                        currentPassword.requestFocus();
                    });
        });

        return dialog;
    }
}