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
    private static final String BUILDING_CODES_COLLECTION = "buildingCodes";

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

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

    public interface BuildingCodeCheckCallback {
        void onResult(boolean exists);
        void onFailure(String errorMessage);
    }

    public interface LoadUsersCallback {
        void onSuccess(List<UserWithId> users);
        void onFailure(String errorMessage);
    }

    public interface LoadBuildingCodesCallback {
        void onSuccess(List<BuildingCodeItem> buildingCodes);
        void onFailure(String errorMessage);
    }

    public static class UserWithId {
        public String uid;
        public User user;

        public UserWithId(String uid, User user) {
            this.uid = uid;
            this.user = user;
        }
    }

    public static class BuildingCodeItem {
        public String code;
        public String buildingName;
        public boolean enabled;

        public BuildingCodeItem(String code, String buildingName, boolean enabled) {
            this.code = code;
            this.buildingName = buildingName;
            this.enabled = enabled;
        }
    }

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

    public void checkApartmentExists(String buildingCode, String aptNumber, @NonNull ApartmentCheckCallback callback) {
        db.collection(USERS_COLLECTION)
                .whereEqualTo("buildingCode", buildingCode)
                .whereEqualTo("aptNumber", aptNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots ->
                        callback.onResult(!queryDocumentSnapshots.isEmpty()))
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to check apartment."
                ));
    }

    public void checkBuildingCodeExists(String buildingCode, @NonNull BuildingCodeCheckCallback callback) {
        db.collection(BUILDING_CODES_COLLECTION)
                .document(buildingCode)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onResult(false);
                        return;
                    }

                    Boolean enabled = documentSnapshot.getBoolean("enabled");
                    callback.onResult(Boolean.TRUE.equals(enabled));
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to check building code."
                ));
    }

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

    public void updateUser(String uid, User user, @NonNull FirestoreCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(uid)
                .set(user)
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

    public void createBuildingCode(String code, String buildingName, boolean enabled, @NonNull FirestoreCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("buildingName", buildingName);
        data.put("enabled", enabled);

        db.collection(BUILDING_CODES_COLLECTION)
                .document(code)
                .set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to save building code."
                ));
    }

    public void editBuildingCode(String code, String buildingName, boolean enabled, @NonNull FirestoreCallback callback) {
        db.collection(BUILDING_CODES_COLLECTION)
                .document(code)
                .update(
                        "buildingName", buildingName,
                        "enabled", enabled
                )
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to edit building code."
                ));
    }

    public void updateBuildingCodeEnabled(String code, boolean enabled, @NonNull FirestoreCallback callback) {
        db.collection(BUILDING_CODES_COLLECTION)
                .document(code)
                .update("enabled", enabled)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to update building code."
                ));
    }

    public void deleteBuildingCode(String code, @NonNull FirestoreCallback callback) {
        db.collection(BUILDING_CODES_COLLECTION)
                .document(code)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to delete building code."
                ));
    }

    public void getAllBuildingCodes(@NonNull LoadBuildingCodesCallback callback) {
        db.collection(BUILDING_CODES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BuildingCodeItem> buildingCodes = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String code = document.getId();
                        String buildingName = document.getString("buildingName");
                        Boolean enabled = document.getBoolean("enabled");

                        buildingCodes.add(new BuildingCodeItem(
                                code,
                                buildingName != null ? buildingName : "",
                                Boolean.TRUE.equals(enabled)
                        ));
                    }

                    callback.onSuccess(buildingCodes);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to load building codes."
                ));
    }
}