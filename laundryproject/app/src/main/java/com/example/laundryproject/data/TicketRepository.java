package com.example.laundryproject.data;

import com.example.laundryproject.model.Ticket;
import com.example.laundryproject.model.TicketMessage;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public TicketRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface TicketListCallback {
        void onSuccess(List<Ticket> tickets);
        void onFailure(String error);
    }

    public interface TicketCallback {
        void onSuccess(Ticket ticket);
        void onFailure(String error);
    }

    public interface MessageListCallback {
        void onSuccess(List<TicketMessage> messages);
        void onFailure(String error);
    }

    public void createTicket(String subject, String category, String firstMessage, SimpleCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            callback.onFailure("User not logged in.");
            return;
        }

        String uid = currentUser.getUid();
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";

        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    String aptNumber = "";
                    String buildingCode = "";

                    if (userDoc.exists()) {
                        if (userDoc.getString("aptNumber") != null) {
                            aptNumber = userDoc.getString("aptNumber");
                        }

                        if (userDoc.getString("buildingCode") != null) {
                            buildingCode = userDoc.getString("buildingCode");
                        }
                    }

                    if (buildingCode.trim().isEmpty()) {
                        callback.onFailure("User has no building assigned.");
                        return;
                    }

                    DocumentReference ticketRef = db.collection("tickets").document();
                    String ticketId = ticketRef.getId();
                    Timestamp now = Timestamp.now();

                    Map<String, Object> ticketData = new HashMap<>();
                    ticketData.put("ticketId", ticketId);
                    ticketData.put("userId", uid);
                    ticketData.put("userEmail", email);
                    ticketData.put("aptNumber", aptNumber);
                    ticketData.put("buildingCode", buildingCode);
                    ticketData.put("subject", subject);
                    ticketData.put("category", category);
                    ticketData.put("status", "open");
                    ticketData.put("lastMessage", firstMessage);
                    ticketData.put("archived", false);
                    ticketData.put("createdAt", now);
                    ticketData.put("updatedAt", now);
                    ticketData.put("closedAt", null);

                    ticketRef.set(ticketData)
                            .addOnSuccessListener(unused -> {
                                Map<String, Object> messageData = new HashMap<>();
                                DocumentReference messageRef = ticketRef.collection("messages").document();

                                messageData.put("messageId", messageRef.getId());
                                messageData.put("senderId", uid);
                                messageData.put("senderType", "user");
                                messageData.put("message", firstMessage);
                                messageData.put("timestamp", now);

                                messageRef.set(messageData)
                                        .addOnSuccessListener(unused1 -> callback.onSuccess())
                                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public ListenerRegistration listenForUserTickets(String userId, TicketListCallback callback) {
        return db.collection("tickets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", false)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<Ticket> tickets = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Ticket ticket = doc.toObject(Ticket.class);
                            ticket.setTicketId(doc.getId());
                            tickets.add(ticket);
                        }
                    }
                    callback.onSuccess(tickets);
                });
    }

    public ListenerRegistration listenForClosedUserTickets(String userId, TicketListCallback callback) {
        return db.collection("tickets")
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", true)
                .orderBy("closedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<Ticket> tickets = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Ticket ticket = doc.toObject(Ticket.class);
                            ticket.setTicketId(doc.getId());
                            tickets.add(ticket);
                        }
                    }
                    callback.onSuccess(tickets);
                });
    }

    public ListenerRegistration listenForBuildingTickets(String buildingCode, TicketListCallback callback) {
        return db.collection("tickets")
                .whereEqualTo("buildingCode", buildingCode)
                .whereEqualTo("archived", false)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<Ticket> tickets = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Ticket ticket = doc.toObject(Ticket.class);
                            ticket.setTicketId(doc.getId());
                            tickets.add(ticket);
                        }
                    }
                    callback.onSuccess(tickets);
                });
    }

    public ListenerRegistration listenForClosedBuildingTickets(String buildingCode, TicketListCallback callback) {
        return db.collection("tickets")
                .whereEqualTo("buildingCode", buildingCode)
                .whereEqualTo("archived", true)
                .orderBy("closedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<Ticket> tickets = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Ticket ticket = doc.toObject(Ticket.class);
                            ticket.setTicketId(doc.getId());
                            tickets.add(ticket);
                        }
                    }
                    callback.onSuccess(tickets);
                });
    }

    public ListenerRegistration listenForMessages(String ticketId, MessageListCallback callback) {
        return db.collection("tickets")
                .document(ticketId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    List<TicketMessage> messages = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            TicketMessage message = doc.toObject(TicketMessage.class);
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }
                    }
                    callback.onSuccess(messages);
                });
    }

    public ListenerRegistration listenToTicket(String ticketId, TicketCallback callback) {
        return db.collection("tickets")
                .document(ticketId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onFailure(error.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Ticket ticket = snapshot.toObject(Ticket.class);
                        if (ticket != null) {
                            ticket.setTicketId(snapshot.getId());
                            callback.onSuccess(ticket);
                        } else {
                            callback.onFailure("Failed to parse ticket.");
                        }
                    } else {
                        callback.onFailure("Ticket not found.");
                    }
                });
    }

    public void sendMessage(String ticketId, String messageText, boolean isAdmin, SimpleCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            callback.onFailure("User not logged in.");
            return;
        }

        String senderId = currentUser.getUid();
        String senderType = isAdmin ? "admin" : "user";
        String nextStatus = isAdmin ? "answered" : "open";

        DocumentReference ticketRef = db.collection("tickets").document(ticketId);
        DocumentReference messageRef = ticketRef.collection("messages").document();

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", messageRef.getId());
        messageData.put("senderId", senderId);
        messageData.put("senderType", senderType);
        messageData.put("message", messageText);
        messageData.put("timestamp", Timestamp.now());

        messageRef.set(messageData)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", messageText);
                    updates.put("status", nextStatus);
                    updates.put("updatedAt", Timestamp.now());

                    ticketRef.update(updates)
                            .addOnSuccessListener(unused1 -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure("Parent ticket update failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Message write failed: " + e.getMessage()));
    }

    public void updateTicketStatus(String ticketId, String status, SimpleCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", Timestamp.now());

        if ("closed".equalsIgnoreCase(status)) {
            updates.put("archived", true);
            updates.put("closedAt", Timestamp.now());
        }

        db.collection("tickets").document(ticketId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteTicket(String ticketId, SimpleCallback callback) {
        db.collection("tickets")
                .document(ticketId)
                .collection("messages")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        db.collection("tickets").document(ticketId)
                                .delete()
                                .addOnSuccessListener(unused -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                        return;
                    }

                    final int totalMessages = querySnapshot.size();
                    final int[] deletedCount = {0};
                    final boolean[] failed = {false};

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        doc.getReference().delete()
                                .addOnSuccessListener(unused -> {
                                    if (failed[0]) return;

                                    deletedCount[0]++;
                                    if (deletedCount[0] == totalMessages) {
                                        db.collection("tickets").document(ticketId)
                                                .delete()
                                                .addOnSuccessListener(unused2 -> callback.onSuccess())
                                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!failed[0]) {
                                        failed[0] = true;
                                        callback.onFailure("Failed deleting messages: " + e.getMessage());
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}