package com.example.laundryproject;

public class User {
    public String email;
    public String password;
    public String aptNumber;
    public boolean enabled;
    public String accountType;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public User(String email, String password, String aptNumber, boolean enabled, String accountType) {
        this.email = email;
        this.password = password;
        this.aptNumber = aptNumber;
        this.enabled = enabled;
        this.accountType = accountType;
    }
}
