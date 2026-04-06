package com.example.laundryproject.model;

import com.google.firebase.Timestamp;

public class Ticket {
    private String ticketId;
    private String userId;
    private String userEmail;
    private String aptNumber;
    private String buildingCode;
    private String subject;
    private String category;
    private String status;
    private String lastMessage;
    private boolean archived;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp closedAt;

    public Ticket() {
        // Required for Firestore
    }

    public Ticket(String ticketId, String userId, String userEmail, String aptNumber,
                  String buildingCode, String subject, String category, String status,
                  String lastMessage, boolean archived,
                  Timestamp createdAt, Timestamp updatedAt, Timestamp closedAt) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.aptNumber = aptNumber;
        this.buildingCode = buildingCode;
        this.subject = subject;
        this.category = category;
        this.status = status;
        this.lastMessage = lastMessage;
        this.archived = archived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAptNumber() {
        return aptNumber;
    }

    public void setAptNumber(String aptNumber) {
        this.aptNumber = aptNumber;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Timestamp closedAt) {
        this.closedAt = closedAt;
    }
}