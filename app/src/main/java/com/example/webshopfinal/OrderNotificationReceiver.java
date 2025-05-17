package com.example.webshopfinal;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;

public class OrderNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Create notification channel
            createNotificationChannel(context);

            // Create intent to open MainActivity (login screen)
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // Create pending intent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "order_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Sikeres rendelés!")
                .setContentText("A rendelésed sikeresen rögzítésre került.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

            // Show notification
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(1, builder.build());
                Log.d("OrderNotificationReceiver", "Notification shown successfully");
            }
        } catch (Exception e) {
            Log.e("OrderNotificationReceiver", "Error showing notification: " + e.getMessage());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                "order_channel",
                "Rendelés értesítések",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Értesítések a sikeres rendelésekhez");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setShowBadge(true);
            
            NotificationManager notificationManager = 
                context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
} 