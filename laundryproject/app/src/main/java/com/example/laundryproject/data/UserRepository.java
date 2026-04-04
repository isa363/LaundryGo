package com.example.laundryproject.data;

import androidx.annotation.NonNull;

import com.example.laundryproject.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore db;
    private static final String USERS_COLLECTION = "users";
    private static final String BUILDINGS_COLLECTION = "buildingCodes";

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    // -------------------- CALLBACK INTERFACES --------------------

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface LoadUserCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface ApartmentCheckCallback {
        void onResult(boolean exists);
        void onFailure(String errorMessage);
    }

    public interface BuildingListCallback {
        void onSuccess(List<String> buildingNames);
        void onFailure(String errorMessage);
    }

    public interface BuildingCodeFetchCallback {
        void onSuccess(String code);
        void onFailure(String errorMessage);
    }

    public interface LoadUsersCallback {
        void onSuccess(List<UserWithId> users);
        void onFailure(String errorMessage);
    }

    // -------------------- DATA CLASSES --------------------

    public static class UserWithId {
        public String uid;
        public User user;

        public UserWithId(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }
    }

    public static class BuildingItem {
        public String buildingName;
        public String code;
        public boolean enabled;

        public BuildingItem(String buildingName, String code, boolean enabled) {
            this.buildingName = buildingName;
            this.code = code;
            this.enabled = enabled;
        }
    }

    // -------------------- USER METHODS --------------------

    public void saveUser(String uid, User user, @NonNull FirestoreCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to save user."
                ));
    }

    public void getUser(String uid, @NonNull LoadUserCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onFailure("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to load user."
                ));
    }

    public void checkApartmentExists(String buildingName, String aptNumber, @NonNull ApartmentCheckCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("buildingCode", buildingName)
                .whereEqualTo("aptNumber", aptNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        callback.onResult(!queryDocumentSnapshots.isEmpty()))
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to check apartment."
                ));
    }

    // -------------------- BUILDING METHODS --------------------

    /** Returns list of building names for dropdown */
    public void getAllBuildings(@NonNull BuildingListCallback callback) {
        db.collection(BUILDINGS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> buildings = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        buildings.add(doc.getId());
                    }

                    callback.onSuccess(buildings);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to load buildings."
                ));
    }

    /** Fetch building code for selected building */
    public void getBuildingCode(String buildingName, @NonNull BuildingCodeFetchCallback callback) {
        db.collection(BUILDINGS_COLLECTION)
                .document(buildingName)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("Building not found");
                        return;
                    }

                    String code = doc.getString("code");
                    callback.onSuccess(code);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to fetch building code."
                ));
    }

    /** Create a building (document ID = buildingName) */
    public void createBuilding(String buildingName, String code, boolean enabled, @NonNull FirestoreCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("buildingName", buildingName);
        data.put("code", code);
        data.put("enabled", enabled);

        db.collection(BUILDINGS_COLLECTION)
                .document(buildingName)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to create building."
                ));
    }

    /** Edit building info */
    public void editBuilding(String buildingName, String code, boolean enabled, @NonNull FirestoreCallback callback) {
        db.collection(BUILDINGS_COLLECTION)
                .document(buildingName)
                .update(
                        "code", code,
                        "enabled", enabled
                )
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to update building."
                ));
    }

    /** Enable/disable building */
    public void updateBuildingEnabled(String buildingName, boolean enabled, @NonNull FirestoreCallback callback) {
        db.collection(BUILDINGS_COLLECTION)
                .document(buildingName)
                .update("enabled", enabled)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to update building status."
                ));
    }

    /** Delete building */
    public void deleteBuilding(String buildingName, @NonNull FirestoreCallback callback) {
        db.collection(BUILDINGS_COLLECTION)
                .document(buildingName)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to delete building."
                ));
    }

    // -------------------- ADMIN PANEL: LOAD ALL USERS --------------------

    public void getAllUsers(@NonNull LoadUsersCallback callback) {
        db.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserWithId> users = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            users.add(new UserWithId(document.getId(), user));
                        }
                    }

                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to load users."
                ));
    }
    public void updateUserField(String uid, String field, Object value, @NonNull FirestoreCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .update(field, value)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to update user."
                ));
    }
    public void deleteUserDocument(String uid, @NonNull FirestoreCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to delete user."
                ));
    }
}
