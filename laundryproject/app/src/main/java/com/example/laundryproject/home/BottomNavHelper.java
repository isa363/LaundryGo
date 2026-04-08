package com.example.laundryproject.home;

import android.app.Activity;
import android.content.Intent;

import com.example.laundryproject.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class BottomNavHelper {

    private BottomNavHelper() {}

    public static void setup(Activity activity, int bottomNavId, int selectedItemId) {
        BottomNavigationView bottomNav = activity.findViewById(bottomNavId);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == selectedItemId) {
                return true;
            }

            Intent intent = null;

            if (id == R.id.nav_home) {
                intent = new Intent(activity, HomeActivity.class);
            } else if (id == R.id.nav_machines) {
                intent = new Intent(activity, MainActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(activity, HistoryActivity.class);
            } else if (id == R.id.nav_account) {
                intent = new Intent(activity, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0);
                activity.finish();
                return true;
            }

            return false;
        });
    }
}