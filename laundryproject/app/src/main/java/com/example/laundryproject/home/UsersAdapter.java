package com.example.laundryproject.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundryproject.R;
import com.example.laundryproject.data.UserRepository;
import com.example.laundryproject.model.User;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(UserRepository.UserWithId user);
    }

    private List<UserRepository.UserWithId> userList;
    private OnUserClickListener listener;

    public UsersAdapter(List<UserRepository.UserWithId> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserRepository.UserWithId item = userList.get(position);
        User user = item.user;

        holder.emailText.setText(user.email != null ? user.email : "No email");
        holder.apartmentText.setText("Apartment: " + (user.aptNumber != null ? user.aptNumber : "N/A"));

        holder.itemView.setOnClickListener(v -> listener.onUserClick(item));

        View circle = holder.itemView.findViewById(R.id.statusCircle);

        if (user.enabled) {
            circle.getBackground().setTint(0xFF00C853); // green glow
        } else {
            circle.getBackground().setTint(0xFFBDBDBD); // grey
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView emailText, apartmentText;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            emailText = itemView.findViewById(R.id.tvUserEmail);
            apartmentText = itemView.findViewById(R.id.tvUserApartment);
        }
    }
}
