package com.maramagagriculturalaid.app.Notification;

import android.util.Log;

import com.google.firebase.firestore.*;
import java.util.*;

public class NotificationHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void getBarangayNotifications(OnNotificationsLoadedListener listener) {
        db.collection("notifications")
                .whereEqualTo("type", "Barangay")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setId(doc.getId());
                        notifications.add(notification);
                    }
                    listener.onNotificationsLoaded(notifications);
                });
    }

    public void markAsRead(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .update("isRead", true);
    }

    public static void sendSubsidyNotification(String farmerId, String firstName,
                                               String lastName, String middleInitial,
                                               String subsidyId, boolean isApproved) {
        Notification notification = new Notification(
                farmerId,
                firstName,
                lastName,
                middleInitial,
                subsidyId,
                isApproved ? "Approved" : "Rejected"
        );

        FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(notification)
                .addOnFailureListener(e -> Log.e("Notification", "Error sending notification", e));
    }

    public interface OnNotificationsLoadedListener {
        void onNotificationsLoaded(List<Notification> notifications);
        void onError(String error);
    }
}
