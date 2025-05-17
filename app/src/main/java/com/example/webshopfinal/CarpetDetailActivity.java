package com.example.webshopfinal;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.widget.Button;
import com.google.firebase.auth.FirebaseUser;

public class CarpetDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carpet_detail);

        ImageView imageView = findViewById(R.id.detailCarpetImageView);
        TextView nameText = findViewById(R.id.detailCarpetName);
        TextView priceText = findViewById(R.id.detailCarpetPrice);
        TextView materialText = findViewById(R.id.detailCarpetMaterial);
        TextView sizeText = findViewById(R.id.detailCarpetSize);
        TextView colorText = findViewById(R.id.detailCarpetColor);
        TextView descriptionText = findViewById(R.id.detailCarpetDescription);

        String carpetDocId = getIntent().getStringExtra("carpet_doc_id");
        Log.d("CarpetDetailActivity", "carpet_doc_id from intent: " + carpetDocId);
        if (carpetDocId == null || carpetDocId.isEmpty()) {
            Toast.makeText(this, "Hibás szőnyeg azonosító!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("carpets")
            .document(carpetDocId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Carpet carpet = documentSnapshot.toObject(Carpet.class);
                    if (carpet != null) {
                        nameText.setText(carpet.getName());
                        priceText.setText(String.format("%.0f Ft", carpet.getPrice()));
                        materialText.setText("Anyag: " + carpet.getMaterial());
                        sizeText.setText(String.format("Méret: %.1f x %.1f m (%.1f m²)", carpet.getWidth(), carpet.getLength(), carpet.getArea()));
                        colorText.setText("Szín: " + carpet.getColor());
                        descriptionText.setText("Leírás: " + carpet.getDescription());
                        Glide.with(this)
                                .load(carpet.getImageUrl())
                                .placeholder(R.drawable.rug_background)
                                .into(imageView);

                        Button addToCartBtn = findViewById(R.id.addToCartBtn);
                        addToCartBtn.setOnClickListener(v -> {
                            FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null && carpetDocId != null) {
                                FirestoreDao.getInstance().addToCart(user.getUid(), carpetDocId, 1, new FirestoreDao.OnCartOperationListener() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(CarpetDetailActivity.this, "Hozzáadva a kosárhoz!", Toast.LENGTH_SHORT).show();
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(CarpetDetailActivity.this, "Hiba a kosárhoz adás során!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(this, "Hiba: nem vagy bejelentkezve!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(this, "Szőnyeg nem található!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Hiba történt a szőnyeg betöltésekor!", Toast.LENGTH_SHORT).show();
                finish();
            });
    }
} 