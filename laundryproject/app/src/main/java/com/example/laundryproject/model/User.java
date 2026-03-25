package com.example.laundryproject.model;


// helper class that allows us to send a object to firebase database, if we ever want to save more stuff we can add it to this class and when instantiating we pass it the value
// Every variable in this class gets saved to the database
public class User {
    public String email;
    public String aptNumber;
    public boolean enabled;
    public String accountType;
    public User() {
        // Needed for Firebase object mapping
    }

    public User(String email, String aptNumber, boolean enabled, String accountType) {
        this.email = email;
        this.aptNumber = aptNumber;
        this.enabled = enabled;
        this.accountType = accountType;
    }
}