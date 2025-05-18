package com.example.webshopfinal;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartItemListener {
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private List<Carpet> carpets;
    private TextView totalPriceText;
    private FirestoreDao dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
        recyclerView = findViewById(R.id.cartRecyclerView);
        totalPriceText = findViewById(R.id.totalPriceText);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartItems = new ArrayList<>();
        carpets = new ArrayList<>();
        dao = FirestoreDao.getInstance();
        loadCartItems();
        Button checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setOnClickListener(v -> handleCheckout());
    }

    private void loadCartItems() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dao.getCartItems(userId, new FirestoreDao.OnCartItemsLoadedListener() {
            @Override
            public void onCartItemsLoaded(List<CartItem> items) {
                cartItems.clear();
                cartItems.addAll(items);
                loadCarpets();
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(CartActivity.this, "Hiba a kosár betöltésekor", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCarpets() {
        dao.getAllCarpetsWithId(new FirestoreDao.CarpetsWithIdCallback() {
            @Override
            public void onCarpetsLoaded(List<FirestoreDao.CarpetWithId> carpetsWithId) {
                carpets.clear();
                for (FirestoreDao.CarpetWithId c : carpetsWithId) {
                    Carpet carpet = c.carpet;
                    if (carpet != null) {
                        carpet.setDocId(c.docId);
                        carpets.add(carpet);
                    }
                }
                updateAdapter();
                updateTotalPrice();
            }
        });
    }

    private void updateAdapter() {
        if (adapter == null) {
            adapter = new CartAdapter(this, cartItems, carpets, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < recyclerView.getChildCount(); i++) {
                    View child = recyclerView.getChildAt(i);
                    Animation animation = AnimationUtils.loadAnimation(CartActivity.this, R.anim.fade_in);
                    animation.setStartOffset(i * 100);
                    child.startAnimation(animation);
                }
            }
        });
    }

    private void updateTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            for (Carpet carpet : carpets) {
                if (carpet != null && carpet.getDocId() != null && 
                    carpet.getDocId().equals(item.getCarpetDocId())) {
                    total += carpet.getPrice() * item.getQuantity();
                    break;
                }
            }
        }
        totalPriceText.setText(String.format("%.0f Ft", total));
    }

    @Override
    public void onRemove(CartItem item) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dao.removeCartItem(userId, item.getCarpetDocId());
        cartItems.remove(item);
        updateAdapter();
        updateTotalPrice();
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dao.updateCartItemQuantity(userId, item.getCarpetDocId(), newQuantity, new FirestoreDao.OnCartOperationListener() {
            @Override
            public void onSuccess() {
                item.setQuantity(newQuantity);
                updateAdapter();
                updateTotalPrice();
            }
            @Override
            public void onError(Exception e) {
                Toast.makeText(CartActivity.this, "Hiba a mennyiség módosítása során", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCheckout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Nem vagy bejelentkezve!", Toast.LENGTH_SHORT).show();
            return;
        }
        FirestoreDao.getInstance().getUser(user, loadedUser -> {
            if (loadedUser == null ||
                isEmpty(loadedUser.getZip()) ||
                isEmpty(loadedUser.getCity()) ||
                isEmpty(loadedUser.getStreet()) ||
                isEmpty(loadedUser.getHouseNumber())) {
                new AlertDialog.Builder(this)
                    .setTitle("Hiányzó lakcím")
                    .setMessage("A rendelés leadásához előbb töltsd ki a lakcímedet a profilodban!")
                    .setPositiveButton("Profil szerkesztése", (dialog, which) -> {
                        startActivity(new android.content.Intent(this, ProfileActivity.class));
                    })
                    .setNegativeButton("Mégse", null)
                    .show();
                return;
            }
            placeOrder(user, loadedUser);
        });
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void placeOrder(FirebaseUser user, User loadedUser) {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "A kosár üres!", Toast.LENGTH_SHORT).show();
            return;
        }
        Button checkoutButton = findViewById(R.id.checkoutButton);
        checkoutButton.setEnabled(false);
        List<CartItem> orderItems = new ArrayList<>();
        for (CartItem item : cartItems) {
            orderItems.add(new CartItem(item.getCarpetDocId(), item.getQuantity()));
        }
        double total = 0;
        for (CartItem item : orderItems) {
            for (Carpet carpet : carpets) {
                if (carpet != null && carpet.getDocId() != null &&
                    carpet.getDocId().equals(item.getCarpetDocId())) {
                    total += carpet.getPrice() * item.getQuantity();
                    break;
                }
            }
        }
        Map<String, Object> order = new HashMap<>();
        order.put("userId", user.getUid());
        order.put("userEmail", user.getEmail());
        order.put("address", new HashMap<String, Object>() {{
            put("zip", loadedUser.getZip());
            put("city", loadedUser.getCity());
            put("street", loadedUser.getStreet());
            put("houseNumber", loadedUser.getHouseNumber());
        }});
        order.put("items", orderItems);
        order.put("total", total);
        order.put("timestamp", Timestamp.now());
        FirebaseFirestore.getInstance().collection("orders")
            .add(order)
            .addOnSuccessListener(documentReference -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                clearCart(user.getUid(), () -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(this, "Rendelés sikeresen leadva!", Toast.LENGTH_LONG).show();
                    showOrderPlacedNotification();
                    try {
                        scheduleOrderNotification();
                    } catch (Exception e) {
                        Log.e("CartActivity", "Error scheduling notification: " + e.getMessage());
                    }
                    try {
                        Intent intent = new Intent(this, HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("SECRET_KEY", 99);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        Log.e("CartActivity", "Error navigating to home: " + e.getMessage());
                    }
                });
            })
            .addOnFailureListener(e -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                checkoutButton.setEnabled(true);
                Toast.makeText(this, "Hiba a rendelés leadásakor!", Toast.LENGTH_SHORT).show();
                Log.e("CartActivity", "Error placing order: " + e.getMessage());
            });
    }

    private void clearCart(String userId, Runnable onComplete) {
        if (userId == null) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        FirebaseFirestore.getInstance().collection("users")
            .document(userId).collection("cart")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                List<Task<Void>> deleteTasks = new ArrayList<>();
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    deleteTasks.add(doc.getReference().delete());
                }
                if (deleteTasks.isEmpty()) {
                    if (!isFinishing() && !isDestroyed()) {
                        cartItems.clear();
                        updateAdapter();
                        updateTotalPrice();
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } else {
                    Tasks.whenAll(deleteTasks)
                        .addOnSuccessListener(aVoid -> {
                            if (!isFinishing() && !isDestroyed()) {
                                cartItems.clear();
                                updateAdapter();
                                updateTotalPrice();
                            }
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CartActivity", "Error clearing cart: " + e.getMessage());
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(this, "Hiba a kosár törlése során!", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e("CartActivity", "Error querying cart: " + e.getMessage());
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(this, "Hiba a kosár lekérdezése során!", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void scheduleOrderNotification() {
        try {
            Intent intent = new Intent(this, OrderNotificationReceiver.class);
            intent.setAction("com.example.webshopfinal.ORDER_NOTIFICATION");
            intent.putExtra("order_success", true);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                long triggerAtMillis = System.currentTimeMillis() + 5000;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        );
                        Log.d("CartActivity", "Exact alarm scheduled");
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        );
                        Log.d("CartActivity", "Inexact alarm scheduled");
                    }
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    );
                    Log.d("CartActivity", "Exact alarm scheduled for older Android");
                }
                Log.d("CartActivity", "Notification scheduled for: " + new java.util.Date(triggerAtMillis));
            }
        } catch (Exception e) {
            Log.e("CartActivity", "Error scheduling notification: " + e.getMessage());
        }
    }

    private void showOrderPlacedNotification() {
        String channelId = "order_channel";
        String channelName = "Order Notifications";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Rendelés leadva!")
            .setContentText("A rendelésed sikeresen leadva.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}