package com.example.laundryproject.util;

import android.util.Patterns;


// Helper functions to just make sure user inputs are valid.
public class ValidationUtils {

    public static boolean isEmailValid(String email) {
        return email != null && !email.trim().isEmpty()
                && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isPasswordValid(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
}