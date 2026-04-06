package com.example.laundryproject.model;

import com.google.firebase.Timestamp;

public class TicketMessage {
    private String messageId;
    private String senderId;
    private String senderType; // "user" or "admin"
    private String message;
    private Timestamp timestamp;

    public TicketMessage() {
        // Required for Firestore
    }

    public TicketMessage(String messageId, String senderId, String senderType, String message, Timestamp timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderType = senderType;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}