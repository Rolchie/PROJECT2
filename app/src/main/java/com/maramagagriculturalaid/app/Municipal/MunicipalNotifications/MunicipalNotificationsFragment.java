package com.maramagagriculturalaid.app.Municipal.MunicipalNotifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.maramagagriculturalaid.app.Notification.Notification;
import com.maramagagriculturalaid.app.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MunicipalNotificationsFragment extends Fragment implements MunicipalNotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "MunicipalNotificationsFragment";
    private static final int NOTIFICATION_DETAIL_REQUEST = 1001;

    // UI Components
    private RecyclerView recyclerViewNotifications;
    private LinearLayout emptyState;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data and Firebase
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MunicipalNotificationAdapter adapter;
    private List<Notification> notificationsList;
    private ListenerRegistration notificationsListener;
    private List<ListenerRegistration> subsidyListeners;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initFirebase();
        setupRecyclerView();

        // Start monitoring subsidy applications
        startMonitoringSubsidyApplications();

        // Load municipal notifications
        loadMunicipalNotifications();
    }

    private void initViews(View view) {
        recyclerViewNotifications = view.findViewById(R.id.recyclerViewNotifications);
        emptyState = view.findViewById(R.id.empty_state);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        progressBar = view.findViewById(R.id.progress_bar);

    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        notificationsList = new ArrayList<>();
        subsidyListeners = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new MunicipalNotificationAdapter(getContext(), notificationsList);
        adapter.setOnNotificationClickListener(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerViewNotifications.setLayoutManager(layoutManager);
        recyclerViewNotifications.setAdapter(adapter);
    }

    private void startMonitoringSubsidyApplications() {
        // List of all barangays to monitor
        String[] barangays = {
                "Anahawon", "Bagongsilang", "Base Camp", "Bayabason", "Camp 1",
                "Colambugon", "Dagumba-an", "Danggawan", "Dologon", "Kiharong",
                "Kisanday", "Kuya", "La Roxas", "North Poblacion"
        };

        for (String barangay : barangays) {
            ListenerRegistration listener = db.collection("Barangays")
                    .document(barangay)
                    .collection("SubsidyRequests")
                    .addSnapshotListener((queryDocumentSnapshots, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Error monitoring subsidy requests for " + barangay, e);
                            return;
                        }

                        if (queryDocumentSnapshots != null) {
                            for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                // Check if this subsidy request already has a notification
                                checkAndCreateNotification(document, barangay);
                            }
                        }
                    });

            subsidyListeners.add(listener);
        }

        Log.d(TAG, "Started monitoring subsidy applications for all barangays");
    }

    private void checkAndCreateNotification(DocumentSnapshot subsidyDoc, String barangay) {
        String subsidyId = subsidyDoc.getId();

        // Check if notification already exists for this subsidy
        db.collection("Municipal")
                .document("Maramag")
                .collection("Municipal Notifications")
                .whereEqualTo("subsidyId", subsidyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No notification exists, create one
                        createMunicipalNotification(subsidyDoc, barangay);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing notifications", e);
                });
    }

    private void createMunicipalNotification(DocumentSnapshot subsidyDoc, String barangay) {
        try {
            // Create notification object
            Notification notification = new Notification();
            notification.setSubsidyId(subsidyDoc.getId());
            notification.setBarangay(barangay);
            notification.setTimestamp(new Date());
            notification.setRead(false);
            notification.setStatus("Pending");
            notification.setType("subsidy_application");

            // Get farmer data from subsidy document
            String farmerId = subsidyDoc.getString("farmerId");
            String firstName = subsidyDoc.getString("firstName");
            String lastName = subsidyDoc.getString("lastName");
            String middleInitial = subsidyDoc.getString("middleInitial");
            String farmType = subsidyDoc.getString("farmType");

            notification.setFarmerId(farmerId);
            notification.setFirstName(firstName);
            notification.setLastName(lastName);
            notification.setMiddleInitial(middleInitial);
            notification.setFarmType(farmType);

            // Build full name
            String fullName = buildFullName(firstName, middleInitial, lastName);
            notification.setFullName(fullName);

            // Set title and message
            notification.setTitle("New Subsidy Application");
            notification.setMessage("From: " + barangay);

            // Save to Municipal Notifications
            db.collection("Municipal")
                    .document("Maramag")
                    .collection("Municipal Notifications")
                    .document(subsidyDoc.getId())
                    .set(notification)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Municipal notification created for subsidy: " + subsidyDoc.getId() + " from " + barangay);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating municipal notification", e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error creating notification from subsidy document", e);
        }
    }

    private String buildFullName(String firstName, String middleInitial, String lastName) {
        StringBuilder fullName = new StringBuilder();

        if (firstName != null && !firstName.trim().isEmpty()) {
            fullName.append(firstName.trim());
        }

        if (middleInitial != null && !middleInitial.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleInitial.trim());
            if (!middleInitial.endsWith(".")) {
                fullName.append(".");
            }
        }

        if (lastName != null && !lastName.trim().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName.trim());
        }

        return fullName.toString().isEmpty() ? "Unknown Farmer" : fullName.toString();
    }

    private void loadMunicipalNotifications() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showEmptyState("Please log in to view notifications");
            return;
        }

        showLoading(true);
        Log.d(TAG, "Loading municipal notifications from Municipal/Maramag/Municipal Notifications");

        // Load notifications with real-time updates
        notificationsListener = db.collection("Municipal")
                .document("Maramag")
                .collection("Municipal Notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    showLoading(false);

                    if (e != null) {
                        Log.e(TAG, "Error listening to municipal notifications", e);
                        showEmptyState("Error loading notifications. Please try again.");
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        notificationsList.clear();

                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            try {
                                Notification notification = document.toObject(Notification.class);
                                if (notification != null) {
                                    notification.setId(document.getId());
                                    notificationsList.add(notification);
                                    Log.d(TAG, "Added notification: " + notification.getTitle() + " - " + notification.getMessage());
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error parsing notification document: " + document.getId(), ex);
                            }
                        }

                        updateUI();
                    }
                });
    }

    private void updateUI() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }

        if (notificationsList.isEmpty()) {
            showEmptyState("No notifications yet.\nNew subsidy applications will appear here.");
        } else {
            showNotifications();
            adapter.updateNotifications(notificationsList);
            Log.d(TAG, "Updated UI with " + notificationsList.size() + " municipal notifications");
        }
    }

    private void refreshNotifications() {
        Log.d(TAG, "Refreshing notifications...");

        // The real-time listener will automatically update the UI
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerViewNotifications != null) {
            recyclerViewNotifications.setVisibility(show ? View.GONE : View.VISIBLE);
        }
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        if (emptyState != null) {
            emptyState.setVisibility(View.VISIBLE);
        }
        if (emptyStateText != null) {
            emptyStateText.setText(message);
        }
        if (recyclerViewNotifications != null) {
            recyclerViewNotifications.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showNotifications() {
        if (recyclerViewNotifications != null) {
            recyclerViewNotifications.setVisibility(View.VISIBLE);
        }
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onNotificationClick(Notification notification, int position) {
        // Mark notification as read
        markNotificationAsRead(notification, position);

        // Open notification detail
        openNotificationDetail(notification);
    }

    private void markNotificationAsRead(Notification notification, int position) {
        if (!notification.isRead()) {
            db.collection("Municipal")
                    .document("Maramag")
                    .collection("Municipal Notifications")
                    .document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Municipal notification marked as read");
                        adapter.markAsRead(position);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error marking municipal notification as read", e);
                    });
        }
    }

    private void openNotificationDetail(Notification notification) {
        Intent intent = new Intent(getContext(), MunicipalNotificationDetail.class);

        // Pass all notification data
        intent.putExtra("notificationId", notification.getId());
        intent.putExtra("farmerId", notification.getFarmerId());
        intent.putExtra("firstName", notification.getFirstName());
        intent.putExtra("lastName", notification.getLastName());
        intent.putExtra("middleInitial", notification.getMiddleInitial());
        intent.putExtra("status", notification.getStatus());
        intent.putExtra("subsidyId", notification.getSubsidyId());
        intent.putExtra("farmType", notification.getFarmType());
        intent.putExtra("barangay", notification.getBarangay());
        intent.putExtra("read", notification.isRead());

        if (notification.getTimestamp() != null) {
            intent.putExtra("timestamp", notification.getTimestamp().getTime());
        }

        startActivityForResult(intent, NOTIFICATION_DETAIL_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NOTIFICATION_DETAIL_REQUEST && resultCode == getActivity().RESULT_OK) {
            if (data != null && data.getBooleanExtra("deleted", false)) {
                String deletedNotificationId = data.getStringExtra("notificationId");
                removeNotificationFromList(deletedNotificationId);
            }
        }
    }

    private void removeNotificationFromList(String notificationId) {
        for (int i = 0; i < notificationsList.size(); i++) {
            if (notificationsList.get(i).getId().equals(notificationId)) {
                notificationsList.remove(i);
                adapter.notifyItemRemoved(i);
                break;
            }
        }

        if (notificationsList.isEmpty()) {
            showEmptyState("No notifications yet.\nNew subsidy applications will appear here.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Remove the notifications listener
        if (notificationsListener != null) {
            notificationsListener.remove();
        }

        // Remove all subsidy listeners
        for (ListenerRegistration listener : subsidyListeners) {
            listener.remove();
        }
        subsidyListeners.clear();
    }

    // Method to get unread notification count
    public int getUnreadCount() {
        int count = 0;
        for (Notification notification : notificationsList) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }
}