package com.maramagagriculturalaid.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.maramagagriculturalaid.app.MainActivity;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SubsidyManagement.SubsidyDetailsActivity;

import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "subsidy_notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            sendNotification(title, body, remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Save the token to your server or Firestore
        sendRegistrationToServer(token);
    }

    private void handleDataMessage(Map<String, String> data) {
        try {
            String title = data.get("title");
            String message = data.get("message");
            String subsidyId = data.get("subsidyId");
            String barangay = data.get("barangay");

            // Send notification
            sendNotification(title, message, data);

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        String subsidyId = data.get("subsidyId");
        String barangay = data.get("barangay");

        Intent intent;
        if (subsidyId != null) {
            // Open SubsidyDetailActivity if subsidyId is provided
            intent = new Intent(this, SubsidyDetailsActivity.class);
            intent.putExtra("subsidyId", subsidyId);
            if (barangay != null) {
                intent.putExtra("barangay", barangay);
            }
        } else {
            // Otherwise open MainActivity
            intent = new Intent(this, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create PendingIntent
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_MUTABLE);
        }

        // Set notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Create notification channel for Android 8.0+
        createNotificationChannel();

        // Build notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_popup_reminder)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Generate unique notification ID
        int notificationId = new Random().nextInt(3000);

        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Subsidy Notifications",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("Notifications for subsidy application status changes");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Save the FCM token to your server or Firestore
        // This allows you to target this specific device when sending notifications

        // Example: Save token to Firestore
        /*
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();

            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated!"))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating token", e));
        }
        */
    }
}
