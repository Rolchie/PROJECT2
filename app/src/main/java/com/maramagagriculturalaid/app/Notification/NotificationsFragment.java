package com.maramagagriculturalaid.app.Notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SubsidyManagement.SubsidyDetailsActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private static final String CHANNEL_ID = "subsidy_notifications";

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private FirebaseFirestore db;
    private String currentBarangay;
    private ListenerRegistration notificationsListener;
    private ListenerRegistration subsidyListener;
    private NotificationManagerCompat notificationManagerCompat;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        db = FirebaseFirestore.getInstance();
        notificationManagerCompat = NotificationManagerCompat.from(requireActivity());

        createNotificationChannel();
        requestNotificationPermission();

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerViewNotifications);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateLayout = view.findViewById(R.id.empty_state);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        if (recyclerView == null) {
            Toast.makeText(getContext(), "Error: RecyclerView not found", Toast.LENGTH_LONG).show();
            return view;
        }

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        // Initialize adapter with empty list
        adapter = new NotificationAdapter(new ArrayList<>(), notification -> {
            markNotificationAsRead(notification);
            openNotificationDetails(notification);
        });
        recyclerView.setAdapter(adapter);

        // Get current user's barangay
        getCurrentUserBarangay();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentBarangay != null) {
            setupRealTimeNotificationsListener();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (subsidyListener != null) {
            subsidyListener.remove();
        }
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }

    private void setupRealTimeNotificationsListener() {
        if (notificationsListener != null) {
            notificationsListener.remove();
        }

        notificationsListener = db.collection("Barangays")
                .document(currentBarangay)
                .collection("Notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        showErrorState("Error loading notifications");
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        updateEmptyState(true);
                        return;
                    }

                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setId(doc.getId());
                            notifications.add(notification);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification", e);
                        }
                    }

                    updateNotificationsList(notifications);
                });
    }

    private void updateNotificationsList(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            updateEmptyState(true);
        } else {
            adapter.updateNotifications(notifications);
            adapter.notifyDataSetChanged();
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void getCurrentUserBarangay() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showErrorState("You must be logged in to view notifications");
            return;
        }

        db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentBarangay = documentSnapshot.getString("Barangay");
                        if (currentBarangay == null) currentBarangay = documentSnapshot.getString("barangay");
                        if (currentBarangay == null) currentBarangay = documentSnapshot.getString("barangayName");

                        if (currentBarangay != null && !currentBarangay.isEmpty()) {
                            setupRealTimeNotificationsListener();
                            setupSubsidyStatusListener();
                        } else {
                            showErrorState("Barangay not set in your profile");
                        }
                    } else {
                        showErrorState("User profile not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user document", e);
                    showErrorState("Error loading user profile");
                });
    }

    private void setupSubsidyStatusListener() {
        if (subsidyListener != null) {
            subsidyListener.remove();
        }

        subsidyListener = db.collection("Barangays")
                .document(currentBarangay)
                .collection("SubsidyRequests")
                .whereIn("status", List.of("Approved", "Rejected"))
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed", e);
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED ||
                                dc.getType() == DocumentChange.Type.MODIFIED) {
                            checkAndCreateNotification(dc.getDocument());
                        }
                    }
                });
    }

    private void checkAndCreateNotification(DocumentSnapshot document) {
        String subsidyId = document.getId();
        String status = document.getString("status");

        db.collection("Barangays")
                .document(currentBarangay)
                .collection("Notifications")
                .whereEqualTo("subsidyId", subsidyId)
                .whereEqualTo("status", status)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        createNotification(document);
                    }
                });
    }

    private void createNotification(DocumentSnapshot document) {
        try {
            String farmerId = document.getString("farmerId");
            String farmerName = document.getString("farmerName");
            String status = document.getString("status");
            String farmType = document.getString("farmType");
            String subsidyId = document.getId();

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("farmerId", farmerId != null ? farmerId : "");
            notificationData.put("FirstName", extractFirstName(farmerName));
            notificationData.put("LastName", extractLastName(farmerName));
            notificationData.put("MiddleInitial", extractMiddleInitial(farmerName));
            notificationData.put("subsidyId", subsidyId);
            notificationData.put("status", status != null ? status : "");
            notificationData.put("timestamp", new Date());
            notificationData.put("isRead", false);
            notificationData.put("barangay", currentBarangay);
            notificationData.put("farmType", farmType != null ? farmType : "");

            db.collection("Barangays")
                    .document(currentBarangay)
                    .collection("Notifications")
                    .add(notificationData)
                    .addOnSuccessListener(docRef -> {
                        createSystemNotification("Subsidy " + status,
                                "Application for " + farmerName + " has been " + status.toLowerCase(),
                                subsidyId);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
        }
    }

    private String extractFirstName(String fullName) {
        if (fullName == null) return "";
        String[] parts = fullName.split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    private String extractLastName(String fullName) {
        if (fullName == null) return "";
        String[] parts = fullName.split(" ");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

    private String extractMiddleInitial(String fullName) {
        if (fullName == null) return "";
        String[] parts = fullName.split(" ");
        return parts.length > 2 ? parts[1].substring(0, 1) : "";
    }

    private void createSystemNotification(String title, String message, String subsidyId) {
        try {
            Intent intent = new Intent(requireContext(), SubsidyDetailsActivity.class);
            intent.putExtra("subsidyId", subsidyId);
            intent.putExtra("barangay", currentBarangay);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                            PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_notifications_24)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                notificationManagerCompat.notify(subsidyId.hashCode(), builder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating system notification", e);
        }
    }

    private void markNotificationAsRead(Notification notification) {
        if (!notification.isRead()) {
            db.collection("Barangays")
                    .document(currentBarangay)
                    .collection("Notifications")
                    .document(notification.getId())
                    .update("isRead", true)
                    .addOnSuccessListener(aVoid -> {
                        notification.setRead(true);
                        adapter.notifyItemChanged(adapter.getPosition(notification));
                    });
        }
    }

    private void openNotificationDetails(Notification notification) {
        Intent intent = new Intent(getActivity(), NotificationDetailActivity.class);
        intent.putExtra("subsidyId", notification.getSubsidyId());
        intent.putExtra("Barangay", notification.getBarangay());
        startActivity(intent);
    }

    private void showErrorState(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }

    private void updateEmptyState(boolean isEmpty) {
        progressBar.setVisibility(View.GONE);
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            emptyStateText.setText("No notifications available");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Subsidy Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for subsidy applications");
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}