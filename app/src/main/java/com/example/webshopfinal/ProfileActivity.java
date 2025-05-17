package com.example.webshopfinal;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.HashMap;
import java.util.Map;

import com.example.webshopfinal.FirestoreDao;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.ProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.List;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.util.Log;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextView emailTextView;
    private ImagePicker imagePicker;
    private ImageView profileImageView;
    private Button changePhotoButton;
    private Button deletePhotoButton;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private enum PendingAction { NONE, CAMERA, GALLERY }
    private PendingAction pendingAction = PendingAction.NONE;
    private ProgressBar profileLoadingBar;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Handle camera image
                    Uri imageUri = imagePicker.getImageUri();
                    loadImage(imageUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Handle gallery image
                    if (result.getData() != null && result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        loadImage(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        emailTextView = findViewById(R.id.profileEmailText);

        // Lakcím mezők
        EditText zipEditText = findViewById(R.id.zipEditText);
        EditText cityEditText = findViewById(R.id.cityEditText);
        EditText streetEditText = findViewById(R.id.streetEditText);
        EditText houseNumberEditText = findViewById(R.id.houseNumberEditText);
        Button saveAddressButton = findViewById(R.id.saveAddressButton);
        profileLoadingBar = findViewById(R.id.profileLoadingBar);

        // Disable fields and button while loading
        zipEditText.setEnabled(false);
        cityEditText.setEnabled(false);
        streetEditText.setEnabled(false);
        houseNumberEditText.setEnabled(false);
        saveAddressButton.setEnabled(false);

        imagePicker = new ImagePicker(this);
        profileImageView = findViewById(R.id.profileImageView);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        deletePhotoButton = findViewById(R.id.deletePhotoButton);
        changePhotoButton.setEnabled(false);
        deletePhotoButton.setEnabled(false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            emailTextView.setText(currentUser.getEmail());
            // Load user object and set address fields
            FirestoreDao.getInstance().getUser(currentUser, user -> {
                if (user != null) {
                    if (user.getZip() != null) zipEditText.setText(user.getZip());
                    if (user.getCity() != null) cityEditText.setText(user.getCity());
                    if (user.getStreet() != null) streetEditText.setText(user.getStreet());
                    if (user.getHouseNumber() != null) houseNumberEditText.setText(user.getHouseNumber());
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(this).load(user.getProfileImageUrl()).into(profileImageView);
                        deletePhotoButton.setEnabled(true);
                    }
                }
                // Hide loading bar and enable fields/buttons
                profileLoadingBar.setVisibility(View.GONE);
                zipEditText.setEnabled(true);
                cityEditText.setEnabled(true);
                streetEditText.setEnabled(true);
                houseNumberEditText.setEnabled(true);
                saveAddressButton.setEnabled(true);
                changePhotoButton.setEnabled(true);
            });
        } else {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show();
            profileLoadingBar.setVisibility(View.GONE);
            finish();
            return;
        }

        saveAddressButton.setOnClickListener(v -> {
            String zip = zipEditText.getText().toString().trim();
            String city = cityEditText.getText().toString().trim();
            String street = streetEditText.getText().toString().trim();
            String houseNumber = houseNumberEditText.getText().toString().trim();

            boolean valid = true;
            if (zip.isEmpty()) {
                zipEditText.setError("Kötelező mező");
                valid = false;
            }
            if (city.isEmpty()) {
                cityEditText.setError("Kötelező mező");
                valid = false;
            }
            if (street.isEmpty()) {
                streetEditText.setError("Kötelező mező");
                valid = false;
            }
            if (houseNumber.isEmpty()) {
                houseNumberEditText.setError("Kötelező mező");
                valid = false;
            }
            if (!valid) {
                Toast.makeText(this, "Minden mező kitöltése kötelező!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirestoreDao.getInstance().saveUserAddress(this, currentUser, zip, city, street, houseNumber);
        });

        changePhotoButton.setOnClickListener(v -> showImagePickerDialog());
        deletePhotoButton.setOnClickListener(v -> deleteProfilePicture());

        LinearLayout ordersContainer = findViewById(R.id.ordersContainer);
        loadOrders(ordersContainer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Frissítjük a felhasználói adatokat és rendeléseket
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirestoreDao.getInstance().getUser(currentUser, user -> {
                if (user != null) {
                    EditText zipEditText = findViewById(R.id.zipEditText);
                    EditText cityEditText = findViewById(R.id.cityEditText);
                    EditText streetEditText = findViewById(R.id.streetEditText);
                    EditText houseNumberEditText = findViewById(R.id.houseNumberEditText);
                    if (user.getZip() != null) zipEditText.setText(user.getZip());
                    if (user.getCity() != null) cityEditText.setText(user.getCity());
                    if (user.getStreet() != null) streetEditText.setText(user.getStreet());
                    if (user.getHouseNumber() != null) houseNumberEditText.setText(user.getHouseNumber());
                }
            });
            LinearLayout ordersContainer = findViewById(R.id.ordersContainer);
            loadOrders(ordersContainer);
        }
    }

    private void showImagePickerDialog() {
        String[] options = {"Kamera", "Galéria"};
        new AlertDialog.Builder(this)
                .setTitle("Profilkép módosítása")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            pendingAction = PendingAction.CAMERA;
                            imagePicker.checkCameraPermission(() -> {
                                // Only open camera if permission is already granted
                                imagePicker.openCamera(cameraLauncher);
                            });
                            break;
                        case 1:
                            pendingAction = PendingAction.GALLERY;
                            imagePicker.checkGalleryPermission(() -> {
                                // Only open gallery if permission is already granted
                                imagePicker.openGallery(galleryLauncher);
                            });
                            break;
                    }
                })
                .show();
    }

    private void loadImage(Uri uri) {
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(profileImageView);
        
        uploadImageToFirebase(uri);
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a reference to the location where the image will be stored
        StorageReference storageRef = storage.getReference()
                .child("profile_images")
                .child(currentUser.getUid() + ".jpg");

        // Upload the image
        UploadTask uploadTask = storageRef.putFile(imageUri);

        // Add progress listener
        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Toast.makeText(this, "Feltöltés: " + (int) progress + "%", Toast.LENGTH_SHORT).show();
        });

        // Add success listener
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Update the user's profile in Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("profileImageUrl", uri.toString());

                firestore.collection("users")
                        .document(currentUser.getUid())
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profilkép sikeresen feltöltve!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Hiba történt a profil frissítésekor!", Toast.LENGTH_SHORT).show();
                        });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Hiba történt a kép feltöltésekor!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ImagePicker.CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingAction == PendingAction.CAMERA) {
                    imagePicker.openCamera(cameraLauncher);
                }
            } else {
                Toast.makeText(this, "Kamera engedély szükséges", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ImagePicker.GALLERY_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingAction == PendingAction.GALLERY) {
                    imagePicker.openGallery(galleryLauncher);
                }
            } else {
                Toast.makeText(this, "Galéria engedély szükséges", Toast.LENGTH_SHORT).show();
            }
        }
        pendingAction = PendingAction.NONE;
    }

    private void loadOrders(LinearLayout ordersContainer) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        ordersContainer.removeAllViews();
        ProgressBar loadingBar = findViewById(R.id.profileLoadingBar);
        loadingBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("orders")
            .whereEqualTo("userId", currentUser.getUid())
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Order> orders = new ArrayList<>();
                for (var doc : querySnapshot) {
                    Order order = doc.toObject(Order.class);
                    orders.add(order);
                }
                
                if (orders.isEmpty()) {
                    TextView noOrdersText = new TextView(this);
                    noOrdersText.setText("Még nincsenek rendeléseid");
                    noOrdersText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    noOrdersText.setPadding(0, 32, 0, 32);
                    ordersContainer.addView(noOrdersText);
                    loadingBar.setVisibility(View.GONE);
                    return;
                }

                LayoutInflater inflater = LayoutInflater.from(this);
                for (Order order : orders) {
                    View orderView = inflater.inflate(R.layout.item_order, ordersContainer, false);
                    TextView orderNumberText = orderView.findViewById(R.id.orderNumberText);
                    LinearLayout itemsContainer = orderView.findViewById(R.id.orderItemsContainer);
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault());
                    String orderNumber = order.getTimestamp() != null ? sdf.format(order.getTimestamp().toDate()) : "-";
                    orderNumberText.setText("Rendelés #" + orderNumber);
                    
                    if (order.getItems() != null) {
                        for (CartItem item : order.getItems()) {
                            View itemView = inflater.inflate(R.layout.item_order_carpet, itemsContainer, false);
                            TextView nameText = itemView.findViewById(R.id.orderCarpetName);
                            ImageView imageView = itemView.findViewById(R.id.orderCarpetImage);
                            TextView quantityText = itemView.findViewById(R.id.orderCarpetQuantity);
                            
                            nameText.setText("-");
                            quantityText.setText("x" + item.getQuantity());
                            imageView.setImageResource(R.drawable.rug_background);
                            
                            FirebaseFirestore.getInstance().collection("carpets")
                                .document(item.getCarpetDocId())
                                .get()
                                .addOnSuccessListener(carpetDoc -> {
                                    Carpet carpet = carpetDoc.toObject(Carpet.class);
                                    if (carpet != null) {
                                        nameText.setText(carpet.getName());
                                        Glide.with(ProfileActivity.this)
                                            .load(carpet.getImageUrl())
                                            .placeholder(R.drawable.rug_background)
                                            .into(imageView);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ProfileActivity", "Error loading carpet: " + e.getMessage());
                                    nameText.setText("Hiba a betöltés során");
                                });
                            
                            itemsContainer.addView(itemView);
                        }
                    }
                    ordersContainer.addView(orderView);
                }
                loadingBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                Log.e("ProfileActivity", "Error loading orders: " + e.getMessage());
                Toast.makeText(this, "Hiba a rendelések betöltése során", Toast.LENGTH_SHORT).show();
                loadingBar.setVisibility(View.GONE);
            });
    }

    private void deleteProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
            .setTitle("Profilkép törlése")
            .setMessage("Biztosan törölni szeretnéd a profilképed?")
            .setPositiveButton("Igen", (dialog, which) -> {
                // Delete from Firebase Storage
                StorageReference storageRef = storage.getReference()
                    .child("profile_images")
                    .child(currentUser.getUid() + ".jpg");

                storageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        // Update Firestore to remove the profile image URL
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profileImageUrl", "");

                        firestore.collection("users")
                            .document(currentUser.getUid())
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                // Reset the profile image to default
                                profileImageView.setImageResource(R.drawable.default_profile);
                                deletePhotoButton.setEnabled(false);
                                Toast.makeText(this, getString(R.string.profile_picture_deleted), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Hiba történt a profil frissítésekor!", Toast.LENGTH_SHORT).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        // If the file doesn't exist in storage, just update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("profileImageUrl", "");

                        firestore.collection("users")
                            .document(currentUser.getUid())
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                profileImageView.setImageResource(R.drawable.default_profile);
                                deletePhotoButton.setEnabled(false);
                                Toast.makeText(this, getString(R.string.profile_picture_deleted), Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e2 -> {
                                Toast.makeText(this, "Hiba történt a profil frissítésekor!", Toast.LENGTH_SHORT).show();
                            });
                    });
            })
            .setNegativeButton("Mégse", null)
            .show();
    }
} 