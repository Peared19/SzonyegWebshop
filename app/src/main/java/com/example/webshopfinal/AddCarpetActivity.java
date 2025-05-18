package com.example.webshopfinal;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.UUID;

public class AddCarpetActivity extends AppCompatActivity {
    private Uri selectedImageUri = null;
    private String uploadedImageUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_carpet);

        EditText nameET = findViewById(R.id.addCarpetName);
        EditText descET = findViewById(R.id.addCarpetDescription);
        EditText priceET = findViewById(R.id.addCarpetPrice);
        EditText materialET = findViewById(R.id.addCarpetMaterial);
        EditText widthET = findViewById(R.id.addCarpetWidth);
        EditText lengthET = findViewById(R.id.addCarpetLength);
        EditText colorET = findViewById(R.id.addCarpetColor);
        Button saveBtn = findViewById(R.id.saveCarpetButton);
        ImageView imagePreview = findViewById(R.id.addCarpetImagePreview);
        Button selectImageBtn = findViewById(R.id.selectCarpetImageButton);
        ProgressBar loadingBar = findViewById(R.id.addCarpetLoadingBar);

        saveBtn.setEnabled(false); 

        ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imagePreview);
                    
                    uploadImageToFirebase(selectedImageUri);
                }
            }
        );

        selectImageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        saveBtn.setOnClickListener(v -> {
            String name = nameET.getText().toString().trim();
            String desc = descET.getText().toString().trim();
            String priceStr = priceET.getText().toString().trim();
            String material = materialET.getText().toString().trim();
            String widthStr = widthET.getText().toString().trim();
            String lengthStr = lengthET.getText().toString().trim();
            String color = colorET.getText().toString().trim();

            if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty() || material.isEmpty() ||
                widthStr.isEmpty() || lengthStr.isEmpty() || color.isEmpty() || uploadedImageUrl == null) {
                Toast.makeText(this, "Minden mező és a kép feltöltése kötelező!", Toast.LENGTH_SHORT).show();
                return;
            }

            double price, width, length;
            try {
                price = Double.parseDouble(priceStr);
                width = Double.parseDouble(widthStr);
                length = Double.parseDouble(lengthStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Ár, szélesség és hosszúság csak szám lehet!", Toast.LENGTH_SHORT).show();
                return;
            }

            Carpet carpet = new Carpet(name, desc, price, material, width, length, color, uploadedImageUrl);
            FirebaseFirestore.getInstance().collection("carpets").add(carpet)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Szőnyeg hozzáadva!", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("SECRET_KEY", 99); 
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hiba történt a mentéskor!", Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (imageUri == null) return;
        ProgressBar loadingBar = findViewById(R.id.addCarpetLoadingBar);
        loadingBar.setVisibility(View.VISIBLE);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
            .child("carpet_images/" + UUID.randomUUID().toString() + ".jpg");
        UploadTask uploadTask = storageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                uploadedImageUrl = uri.toString();
                Toast.makeText(this, "Kép feltöltve!", Toast.LENGTH_SHORT).show();
                Button saveBtn = findViewById(R.id.saveCarpetButton);
                saveBtn.setEnabled(true);
                loadingBar.setVisibility(View.GONE);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Kép feltöltése sikertelen!", Toast.LENGTH_SHORT).show();
            loadingBar.setVisibility(View.GONE);
        });
    }
} 