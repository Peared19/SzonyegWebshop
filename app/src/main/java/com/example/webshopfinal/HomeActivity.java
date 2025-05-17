package com.example.webshopfinal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.ImageView;
import android.content.Intent;
import android.view.View;
import android.widget.ProgressBar;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.webshopfinal.FirestoreDao;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.pm.PackageManager;

public class HomeActivity extends AppCompatActivity {
    private static final String LOG_TAG = HomeActivity.class.getName();
    private static final int SECRET_KEY = 99;
    private FirebaseAuth mAuth;
    private RecyclerView carpetRecyclerView;
    private CarpetAdapter carpetAdapter;
    private List<FirestoreDao.CarpetWithId> carpetList = new ArrayList<>();
    private ProgressBar homeLoadingBar;
    private FirestoreDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        // Notification permission kérés Android 13+ esetén
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }

        mAuth = FirebaseAuth.getInstance();
        dao = FirestoreDao.getInstance();
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        // Verify secret key
        int secretKey = getIntent().getIntExtra("SECRET_KEY", 0);
        if (secretKey != SECRET_KEY) {
            Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up loading bar
        homeLoadingBar = findViewById(R.id.homeLoadingBar);
        homeLoadingBar.setVisibility(View.VISIBLE);

        // Set up RecyclerView
        carpetRecyclerView = findViewById(R.id.carpetRecyclerView);
        carpetRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        carpetAdapter = new CarpetAdapter(this, carpetList, carpetDocId -> {
            // On carpet click: open detail activity
            Intent intent = new Intent(HomeActivity.this, CarpetDetailActivity.class);
            intent.putExtra("carpet_doc_id", carpetDocId);
            startActivity(intent);
        });
        carpetRecyclerView.setAdapter(carpetAdapter);

        // Load carpets from Firestore
        loadCarpets();



        // Profil ikon kattintás
        ImageView profileIcon = findViewById(R.id.profileIcon);
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        ImageView cartIcon = findViewById(R.id.cartIcon);
        cartIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CartActivity.class);
            startActivity(intent);
        });

        FloatingActionButton addCarpetFab = findViewById(R.id.addCarpetFab);
        addCarpetFab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddCarpetActivity.class);
            startActivity(intent);
        });
        FirestoreDao.getInstance().isUserAdmin(currentUser, isAdmin -> {
            if (isAdmin) {
                addCarpetFab.setVisibility(View.VISIBLE);
            } else {
                addCarpetFab.setVisibility(View.GONE);
            }
        });
    }

    private void loadCarpets() {
        homeLoadingBar.setVisibility(View.VISIBLE);
        FirestoreDao.getInstance().getAllCarpetsWithId(carpets -> {
            carpetList.clear();
            carpetList.addAll(carpets);
            carpetAdapter.notifyDataSetChanged();
            homeLoadingBar.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCarpets();
    }

    private void showAddToCartDialog(Carpet carpet) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_to_cart, null);
        TextView quantityText = dialogView.findViewById(R.id.quantityText);
        ImageButton decreaseBtn = dialogView.findViewById(R.id.decreaseBtn);
        ImageButton increaseBtn = dialogView.findViewById(R.id.increaseBtn);
        Button addButton = dialogView.findViewById(R.id.addButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        int[] quantity = {1};
        quantityText.setText(String.valueOf(quantity[0]));

        decreaseBtn.setOnClickListener(v -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                quantityText.setText(String.valueOf(quantity[0]));
            }
        });

        increaseBtn.setOnClickListener(v -> {
            quantity[0]++;
            quantityText.setText(String.valueOf(quantity[0]));
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        addButton.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            dao.addToCart(userId, carpet.getDocId(), quantity[0], new FirestoreDao.OnCartOperationListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(HomeActivity.this, "Sikeresen hozzáadva a kosárhoz", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(HomeActivity.this, "Hiba a kosárhoz adás során", Toast.LENGTH_SHORT).show();
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
} 