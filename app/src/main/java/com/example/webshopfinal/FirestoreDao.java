package com.example.webshopfinal;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.List;
import java.util.ArrayList;

public class FirestoreDao {
    private static FirestoreDao instance;
    private final FirebaseFirestore firestore;

    private FirestoreDao() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreDao getInstance() {
        if (instance == null) {
            instance = new FirestoreDao();
        }
        return instance;
    }

    
    public void uploadCarpet(Context context, Carpet carpet) {
        firestore.collection("carpets")
                .add(carpet)
                .addOnSuccessListener(documentReference -> Toast.makeText(context, "Szőnyeg sikeresen feltöltve Firestore-ba!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Hiba a feltöltéskor: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    
    public void saveUserAddress(Context context, FirebaseUser user, String zip, String city, String street, String houseNumber) {
        if (user == null) return;
        Map<String, Object> address = new HashMap<>();
        address.put("zip", zip);
        address.put("city", city);
        address.put("street", street);
        address.put("houseNumber", houseNumber);
        firestore.collection("users")
                .document(user.getUid())
                .set(address, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Lakcím elmentve Firestore-ba!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Hiba a mentéskor: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public void uploadProfileImage(Context context, FirebaseUser user, Uri imageUri) {
        if (user == null || imageUri == null) return;
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images/" + user.getUid() + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot ->
            storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                
                Map<String, Object> update = new HashMap<>();
                update.put("profileImageUrl", downloadUri.toString());
                firestore.collection("users")
                        .document(user.getUid())
                        .set(update, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> Toast.makeText(context, "Profilkép feltöltve!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(context, "URL mentése sikertelen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
        ).addOnFailureListener(e -> Toast.makeText(context, "Kép feltöltése sikertelen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public void createUserDocument(FirebaseUser user, String displayName, com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess, com.google.android.gms.tasks.OnFailureListener onFailure) {
        if (user == null) return;
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", user.getUid());
        userMap.put("email", user.getEmail());
        userMap.put("displayName", displayName);
        userMap.put("profileImageUrl", "");
        firestore.collection("users")
                .document(user.getUid())
                .set(userMap)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public interface UserCallback {
        void onUserLoaded(User user);
    }

    public void getUser(FirebaseUser firebaseUser, UserCallback callback) {
        if (firebaseUser == null) return;
        firestore.collection("users")
            .document(firebaseUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);
                    callback.onUserLoaded(user);
                } else {
                    callback.onUserLoaded(null);
                }
            })
            .addOnFailureListener(e -> callback.onUserLoaded(null));
    }

    public static class CarpetWithId {
        public final Carpet carpet;
        public final String docId;
        public CarpetWithId(Carpet carpet, String docId) {
            this.carpet = carpet;
            this.docId = docId;
        }
    }
    public interface CarpetsWithIdCallback {
        void onCarpetsLoaded(List<CarpetWithId> carpets);
    }
    public void getAllCarpetsWithId(CarpetsWithIdCallback callback) {
        firestore.collection("carpets")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<CarpetWithId> carpets = new ArrayList<>();
                for (var doc : queryDocumentSnapshots) {
                    Carpet carpet = doc.toObject(Carpet.class);
                    carpets.add(new CarpetWithId(carpet, doc.getId()));
                }
                callback.onCarpetsLoaded(carpets);
            })
            .addOnFailureListener(e -> callback.onCarpetsLoaded(new ArrayList<>()));
    }

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
    }
    public void isUserAdmin(FirebaseUser firebaseUser, AdminCheckCallback callback) {
        if (firebaseUser == null) { callback.onResult(false); return; }
        firestore.collection("users")
            .document(firebaseUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                    callback.onResult(isAdmin != null && isAdmin);
                } else {
                    callback.onResult(false);
                }
            })
            .addOnFailureListener(e -> callback.onResult(false));
    }

    public void addToCart(String userId, String carpetDocId, int quantity, OnCartOperationListener listener) {
        if (userId == null || carpetDocId == null) return;
        firestore.collection("users").document(userId)
            .collection("cart").document(carpetDocId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                int newQty = quantity;
                if (documentSnapshot.exists()) {
                    CartItem item = documentSnapshot.toObject(CartItem.class);
                    newQty += (item != null ? item.getQuantity() : 0);
                }
                firestore.collection("users").document(userId)
                    .collection("cart").document(carpetDocId)
                    .set(new CartItem(carpetDocId, newQty))
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(listener::onError);
            })
            .addOnFailureListener(listener::onError);
    }

    public interface OnCartItemsLoadedListener {
        void onCartItemsLoaded(List<CartItem> items);
        void onError(Exception e);
    }

    public interface OnCartOperationListener {
        void onSuccess();
        void onError(Exception e);
    }

    public void updateCartItemQuantity(String userId, String carpetDocId, int newQuantity, OnCartOperationListener listener) {
        firestore.collection("users").document(userId)
                .collection("cart")
                .document(carpetDocId)
                .update("quantity", newQuantity)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public void getCartItems(String userId, OnCartItemsLoadedListener listener) {
        if (userId == null) { 
            listener.onCartItemsLoaded(new ArrayList<>()); 
            return; 
        }
        firestore.collection("users").document(userId)
            .collection("cart").get()
            .addOnSuccessListener(querySnapshot -> {
                List<CartItem> items = new ArrayList<>();
                for (var doc : querySnapshot) {
                    CartItem item = doc.toObject(CartItem.class);
                    items.add(item);
                }
                listener.onCartItemsLoaded(items);
            })
            .addOnFailureListener(listener::onError);
    }

    public void removeCartItem(String userId, String carpetDocId) {
        if (userId == null || carpetDocId == null) return;
        firestore.collection("users").document(userId)
            .collection("cart").document(carpetDocId).delete();
    }
} 