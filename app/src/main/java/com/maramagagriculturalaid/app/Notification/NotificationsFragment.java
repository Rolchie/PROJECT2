package com.maramagagriculturalaid.app.Notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private static final String CHANNEL_ID = "subsidy_notifications";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private FirebaseFirestore db;
    private String currentBarangay;
    private String currentUserId;
    private String currentUserRole;
    private ListenerRegistration notificationsListener;
    private ListenerRegistration subsidyListener;
    private NotificationManagerCompat notificationManagerCompat;
    private List<Notification> notificationsList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        if (getContext() == null) {
            Log.e(TAG, "Context is null in onCreateView");
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        notificationManagerCompat = NotificationManagerCompat.from(requireActivity());
        notificationsList = new ArrayList<>();

        // Get current user info
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            Log.d(TAG, "Current user ID: " + currentUserId);
        } else {
            Log.e(TAG, "No current user found");
            showErrorState("Please log in to view notifications");
            return view;
        }

        // Create notification channel first
        createNotificationChannel();
        // Request notification permission
        requestNotificationPermission();
        // Initialize views
        initializeViews(view);
        // Get current user's barangay and role
        getCurrentUserInfo();

        return view;
    }

    private void initializeViews(View view) {
        try {
            recyclerView = view.findViewById(R.id.recyclerViewNotifications);
            progressBar = view.findViewById(R.id.progress_bar);
            emptyStateLayout = view.findViewById(R.id.empty_state);
            emptyStateText = view.findViewById(R.id.empty_state_text);

            Log.d(TAG, "Views found - RecyclerView: " + (recyclerView != null) +
                    ", ProgressBar: " + (progressBar != null) +
                    ", EmptyState: " + (emptyStateLayout != null));

            if (recyclerView == null) {
                Log.e(TAG, "RecyclerView not found in layout!");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: RecyclerView not found", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // Set up RecyclerView
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setHasFixedSize(true);

            // Initialize adapter
            if (notificationsList == null) {
                notificationsList = new ArrayList<>();
            }

            adapter = new NotificationAdapter(notificationsList, notification -> {
                if (notification != null) {
                    Log.d(TAG, "Notification clicked: " + notification.getId());
                    markNotificationAsRead(notification);
                    openNotificationDetails(notification);
                }
            });
            recyclerView.setAdapter(adapter);

            Log.d(TAG, "RecyclerView and adapter set up successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (currentBarangay != null && !currentBarangay.isEmpty()) {
            setupRealTimeNotificationsListener();
        } else {
            Log.w(TAG, "currentBarangay is null or empty in onResume");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
        if (subsidyListener != null) {
            subsidyListener.remove();
            subsidyListener = null;
        }
        if (notificationsListener != null) {
            notificationsListener.remove();
            notificationsListener = null;
        }
    }

    /**
     * Get current user's barangay and role information
     */
    private void getCurrentUserInfo() {
        Log.d(TAG, "Getting current user info");

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }

        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            Log.e(TAG, "No current user found");
            showErrorState("You must be logged in to view notifications");
            return;
        }

        db.collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d(TAG, "User document retrieved successfully");

                    if (documentSnapshot.exists()) {
                        // Get barangay information
                        currentBarangay = getStringFromDocument(documentSnapshot, "Barangay", "barangay", "barangayName");
                        // Get user role
                        currentUserRole = getStringFromDocument(documentSnapshot, "Role", "role", "userRole");

                        Log.d(TAG, "User info - Barangay: " + currentBarangay + ", Role: " + currentUserRole);

                        if (currentBarangay != null && !currentBarangay.trim().isEmpty()) {
                            currentBarangay = currentBarangay.trim();
                            setupRealTimeNotificationsListener();

                            // Only setup subsidy listener for barangay users
                            if (isBarangayUser()) {
                                setupSubsidyStatusListener();
                            } else {
                                Log.d(TAG, "User is not a barangay user, skipping subsidy listener setup");
                            }
                        } else {
                            Log.e(TAG, "Barangay not found in user profile");
                            showErrorState("Barangay not set in your profile. Please update your profile.");
                        }
                    } else {
                        Log.e(TAG, "User document does not exist");
                        showErrorState("User profile not found. Please contact support.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user document", e);
                    showErrorState("Error loading user profile: " + e.getMessage());
                });
    }

    /**
     * Helper method to get string from document with multiple field name attempts
     */
    private String getStringFromDocument(DocumentSnapshot doc, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = doc.getString(fieldName);
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Check if current user is a barangay user (should receive notifications)
     */
    private boolean isBarangayUser() {
        if (currentUserRole == null) {
            return true; // Default to true if role is not set
        }
        String role = currentUserRole.toLowerCase().trim();
        return role.contains("barangay") || role.contains("farmer") || role.contains("user");
    }

    /**
     * Setup listener for notifications specific to user's barangay
     */
    private void setupRealTimeNotificationsListener() {
        Log.d(TAG, "Setting up real-time notifications listener for barangay: " + currentBarangay);

        if (notificationsListener != null) {
            notificationsListener.remove();
        }

        // Show loading state
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }

        // Query the specific barangay's Notifications collection
        notificationsListener = db.collection("Barangays")
                .document(currentBarangay)
                .collection("Notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    Log.d(TAG, "Notifications listener triggered for barangay: " + currentBarangay);

                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        showErrorState("Error loading notifications: " + error.getMessage());
                        return;
                    }

                    if (value == null) {
                        Log.w(TAG, "Snapshot is null");
                        updateEmptyState(true);
                        return;
                    }

                    Log.d(TAG, "Received " + value.size() + " notification documents for barangay: " + currentBarangay);

                    if (value.isEmpty()) {
                        Log.d(TAG, "No notifications found for barangay: " + currentBarangay);
                        updateEmptyState(true);
                        return;
                    }

                    List<Notification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            Log.d(TAG, "Processing notification document: " + doc.getId());

                            Notification notification = createNotificationFromDocument(doc);
                            if (notification != null) {
                                notifications.add(notification);
                                Log.d(TAG, "Added notification: " + notification.getFullName() + " - " + notification.getStatus() +
                                        " - Timestamp: " + notification.getTimestamp());
                                Log.d(TAG, "Message will be: " + notification.getFormattedMessage());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
                        }
                    }

                    Log.d(TAG, "Successfully parsed " + notifications.size() + " notifications for barangay: " + currentBarangay);
                    updateNotificationsList(notifications);
                });
    }

    /**
     * Setup listener for subsidy status changes in user's barangay only
     */
    private void setupSubsidyStatusListener() {
        Log.d(TAG, "Setting up subsidy status listener for barangay: " + currentBarangay);

        if (subsidyListener != null) {
            subsidyListener.remove();
        }

        // Query the specific barangay's SubsidyRequests collection
        subsidyListener = db.collection("Barangays")
                .document(currentBarangay)
                .collection("SubsidyRequests")
                .whereIn("status", Arrays.asList("Approved", "Rejected"))
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Subsidy status listen failed for barangay: " + currentBarangay, e);
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "Subsidy snapshots is null for barangay: " + currentBarangay);
                        return;
                    }

                    Log.d(TAG, "Subsidy status listener triggered with " + snapshots.getDocumentChanges().size() +
                            " changes for barangay: " + currentBarangay);

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED ||
                                dc.getType() == DocumentChange.Type.MODIFIED) {

                            DocumentSnapshot document = dc.getDocument();
                            Log.d(TAG, "Processing subsidy change for barangay " + currentBarangay +
                                    ": " + document.getId() + " - " + dc.getType());
                            checkAndCreateNotification(document);
                        }
                    }
                });
    }

    /**
     * Check if notification already exists for this subsidy and status in the specific barangay
     */
    private void checkAndCreateNotification(DocumentSnapshot document) {
        String subsidyId = document.getId();
        String status = document.getString("status");

        Log.d(TAG, "Checking if notification exists for subsidy: " + subsidyId +
                " with status: " + status + " in barangay: " + currentBarangay);

        // Query only by subsidyId and status since we're already in the barangay's collection
        db.collection("Barangays")
                .document(currentBarangay)
                .collection("Notifications")
                .whereEqualTo("subsidyId", subsidyId)
                .whereEqualTo("status", status)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().isEmpty()) {
                            Log.d(TAG, "No existing notification found for barangay " + currentBarangay +
                                    ", creating new one");
                            createNotificationForBarangay(document);
                        } else {
                            Log.d(TAG, "Notification already exists for this subsidy and status in barangay: " +
                                    currentBarangay);
                        }
                    } else {
                        Log.e(TAG, "Error checking for existing notification in barangay: " + currentBarangay,
                                task.getException());
                    }
                });
    }

    /**
     * Create notification specifically for the barangay where the subsidy was processed
     */
    private void createNotificationForBarangay(DocumentSnapshot document) {
        try {
            Log.d(TAG, "Creating notification for document: " + document.getId() +
                    " in barangay: " + currentBarangay);

            String farmerId = document.getString("farmerId");
            String farmerName = document.getString("farmerName");
            String status = document.getString("status");
            String farmType = document.getString("farmType");
            String subsidyId = document.getId();

            // Get the original timestamp from the subsidy document to preserve it
            Date originalTimestamp = null;
            try {
                // Try to get the original timestamp from the subsidy document
                Object timestampObj = document.get("dateProcessed");
                if (timestampObj == null) {
                    timestampObj = document.get("timestamp");
                }
                if (timestampObj == null) {
                    timestampObj = document.get("lastModified");
                }

                if (timestampObj instanceof Long) {
                    originalTimestamp = new Date((Long) timestampObj);
                } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                    originalTimestamp = ((com.google.firebase.Timestamp) timestampObj).toDate();
                } else if (timestampObj instanceof Date) {
                    originalTimestamp = (Date) timestampObj;
                }

                // If no timestamp found, use current time
                if (originalTimestamp == null) {
                    originalTimestamp = new Date();
                }

                Log.d(TAG, "Using timestamp: " + originalTimestamp + " for notification");

            } catch (Exception e) {
                Log.e(TAG, "Error getting timestamp, using current time", e);
                originalTimestamp = new Date();
            }

            Log.d(TAG, "Notification data - Farmer: " + farmerName + ", Status: " + status +
                    ", Barangay: " + currentBarangay + ", Timestamp: " + originalTimestamp);

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("farmerId", farmerId != null ? farmerId : "");
            notificationData.put("FirstName", extractFirstName(farmerName));
            notificationData.put("LastName", extractLastName(farmerName));
            notificationData.put("MiddleInitial", extractMiddleInitial(farmerName));
            notificationData.put("subsidyId", subsidyId);
            notificationData.put("status", status != null ? status : "");
            notificationData.put("timestamp", originalTimestamp); // Use original timestamp
            notificationData.put("isRead", false);
            notificationData.put("barangay", currentBarangay);
            notificationData.put("farmType", farmType != null ? farmType : "");

            // Create notification in the specific barangay's collection
            db.collection("Barangays")
                    .document(currentBarangay)
                    .collection("Notifications")
                    .add(notificationData)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "Notification created successfully for barangay " + currentBarangay +
                                ": " + docRef.getId());

                        // Create the notification message for system notification (popup)
                        String notificationMessage = createSystemNotificationMessage(farmerId, farmerName, status);

                        createSystemNotificationForBarangay(
                                "Subsidy " + status,
                                notificationMessage,
                                subsidyId
                        );
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating notification for barangay " + currentBarangay, e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error in createNotificationForBarangay", e);
        }
    }

    /**
     * Helper method to create the notification message for system notifications (popup)
     * Format: "The Subsidy application of ID: farmerID, Name has been approved/rejected"
     */
    private String createSystemNotificationMessage(String farmerId, String farmerName, String status) {
        StringBuilder message = new StringBuilder();
        message.append("The Subsidy application of ");

        // Add farmer ID if available
        if (farmerId != null && !farmerId.trim().isEmpty()) {
            message.append("ID: ").append(farmerId.trim()).append(", ");
        }

        // Add farmer name
        if (farmerName != null && !farmerName.trim().isEmpty()) {
            message.append(farmerName.trim());
        } else {
            message.append("Unknown Farmer");
        }

        // Add status
        message.append(" has been ");
        if (status != null && !status.trim().isEmpty()) {
            message.append(status.toLowerCase());
        } else {
            message.append("processed");
        }

        return message.toString();
    }

    /**
     * Create system notification with the correct message format
     */
    private void createSystemNotificationForBarangay(String title, String message, String subsidyId) {
        try {
            Log.d(TAG, "Creating system notification for barangay " + currentBarangay + ": " + title);
            Log.d(TAG, "Notification message: " + message);

            if (getContext() == null) {
                Log.w(TAG, "Context is null, cannot create system notification");
                return;
            }

            if (!notificationManagerCompat.areNotificationsEnabled()) {
                Log.w(TAG, "Notifications are disabled by user");
                return;
            }

            if (!isBarangayUser()) {
                Log.d(TAG, "User is not a barangay user, skipping system notification");
                return;
            }

            Intent intent = new Intent(requireContext(), SubsidyDetailsActivity.class);
            intent.putExtra("subsidyId", subsidyId);
            intent.putExtra("barangay", currentBarangay);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    requireContext(),
                    (subsidyId + currentBarangay).hashCode(),
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                            PendingIntent.FLAG_UPDATE_CURRENT
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline_notifications_24)
                    .setContentTitle(title)
                    .setContentText(message) // Uses the system notification message format
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message)) // Allow longer text
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setVibrate(new long[]{0, 250, 250, 250})
                    .setLights(Color.BLUE, 1000, 1000)
                    .setGroup(currentBarangay)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setWhen(System.currentTimeMillis()) // Use current time for notification display
                    .setShowWhen(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManagerCompat.notify((subsidyId + currentBarangay).hashCode(), builder.build());
                    Log.d(TAG, "System notification posted successfully for barangay: " + currentBarangay);
                } else {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted");
                }
            } else {
                notificationManagerCompat.notify((subsidyId + currentBarangay).hashCode(), builder.build());
                Log.d(TAG, "System notification posted successfully for barangay: " + currentBarangay);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating system notification for barangay: " + currentBarangay, e);
        }
    }

    /**
     * Create notification from document with proper timestamp handling
     */
    private Notification createNotificationFromDocument(QueryDocumentSnapshot doc) {
        try {
            Notification notification = new Notification();
            notification.setId(doc.getId());

            // Get basic fields
            notification.setFarmerId(doc.getString("farmerId"));
            notification.setSubsidyId(doc.getString("subsidyId"));
            notification.setStatus(doc.getString("status"));
            notification.setBarangay(doc.getString("barangay"));
            notification.setFarmType(doc.getString("farmType"));

            // Handle timestamp properly to preserve individual timestamps
            Date timestamp = null;
            try {
                // Try different timestamp field names and types
                Object timestampObj = doc.get("timestamp");

                if (timestampObj instanceof com.google.firebase.Timestamp) {
                    timestamp = ((com.google.firebase.Timestamp) timestampObj).toDate();
                    Log.d(TAG, "Got Firestore Timestamp: " + timestamp);
                } else if (timestampObj instanceof Date) {
                    timestamp = (Date) timestampObj;
                    Log.d(TAG, "Got Date timestamp: " + timestamp);
                } else if (timestampObj instanceof Long) {
                    timestamp = new Date((Long) timestampObj);
                    Log.d(TAG, "Got Long timestamp: " + timestamp);
                } else {
                    Log.w(TAG, "Unknown timestamp type: " + (timestampObj != null ? timestampObj.getClass() : "null"));
                    timestamp = new Date(); // Fallback to current time
                }

            } catch (Exception e) {
                Log.e(TAG, "Error parsing timestamp for document " + doc.getId(), e);
                timestamp = new Date(); // Fallback to current time
            }

            notification.setTimestamp(timestamp);
            Log.d(TAG, "Set notification timestamp to: " + timestamp + " for document: " + doc.getId());

            // Handle read status
            Boolean isRead = doc.getBoolean("isRead");
            notification.setRead(isRead != null ? isRead : false);

            // Set name fields
            String firstName = doc.getString("FirstName");
            String lastName = doc.getString("LastName");
            String middleInitial = doc.getString("MiddleInitial");

            notification.setFirstName(firstName != null ? firstName : "");
            notification.setLastName(lastName != null ? lastName : "");
            notification.setMiddleInitial(middleInitial != null ? middleInitial : "");

            // If no name components, try full name
            if ((firstName == null || firstName.trim().isEmpty()) &&
                    (lastName == null || lastName.trim().isEmpty())) {
                String directFullName = doc.getString("farmerName");
                if (directFullName != null && !directFullName.trim().isEmpty()) {
                    notification.setFirstName(directFullName);
                } else {
                    notification.setFirstName("Unknown Farmer");
                }
            }

            Log.d(TAG, "Created notification object for barangay " + currentBarangay + ": " +
                    notification.getFullName() + " - " + notification.getStatus() +
                    " - Final Timestamp: " + notification.getTimestamp());
            return notification;

        } catch (Exception e) {
            Log.e(TAG, "Error creating notification from document", e);
            return null;
        }
    }

    private void updateNotificationsList(List<Notification> notifications) {
        Log.d(TAG, "Updating notifications list with " + notifications.size() +
                " items for barangay: " + currentBarangay);

        try {
            if (notifications.isEmpty()) {
                updateEmptyState(true);
            } else {
                if (notificationsList == null) {
                    notificationsList = new ArrayList<>();
                }
                notificationsList.clear();
                notificationsList.addAll(notifications);

                // Log timestamps and messages for debugging
                for (int i = 0; i < Math.min(notifications.size(), 3); i++) {
                    Notification n = notifications.get(i);
                    Log.d(TAG, "Notification " + i + ": " + n.getFullName() +
                            " - Timestamp: " + n.getTimestamp() +
                            " - Message: " + n.getFormattedMessage());
                }

                if (adapter != null) {
                    adapter.updateNotifications(notificationsList);
                    Log.d(TAG, "Adapter updated with notifications for barangay: " + currentBarangay);
                } else {
                    Log.e(TAG, "Adapter is null!");
                }

                // Update UI visibility
                if (recyclerView != null) {
                    recyclerView.setVisibility(View.VISIBLE);
                }
                if (emptyStateLayout != null) {
                    emptyStateLayout.setVisibility(View.GONE);
                }
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                Log.d(TAG, "UI updated successfully for barangay: " + currentBarangay);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notifications list for barangay: " + currentBarangay, e);
            showErrorState("Error updating notifications: " + e.getMessage());
        }
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }

    private String extractMiddleInitial(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 2) {
            return parts[1].length() > 0 ? parts[1].substring(0, 1).toUpperCase() : "";
        }
        return "";
    }

    private void markNotificationAsRead(Notification notification) {
        if (notification == null || notification.getId() == null) {
            Log.w(TAG, "Cannot mark null notification as read");
            return;
        }

        if (!notification.isRead()) {
            Log.d(TAG, "Marking notification as read: " + notification.getId() +
                    " in barangay: " + currentBarangay);

            db.collection("Barangays")
                    .document(currentBarangay)
                    .collection("Notifications")
                    .document(notification.getId())
                    .update("isRead", true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification marked as read successfully");
                        notification.setRead(true);

                        if (adapter != null) {
                            int position = adapter.getPosition(notification);
                            if (position >= 0) {
                                adapter.notifyItemChanged(position);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error marking notification as read", e);
                    });
        }
    }

    private void openNotificationDetails(Notification notification) {
        try {
            Log.d(TAG, "Opening notification details for: " + notification.getId() +
                    " in barangay: " + currentBarangay);

            Intent intent = new Intent(getActivity(), NotificationDetailActivity.class);
            intent.putExtra("notificationId", notification.getId());
            intent.putExtra("barangay", notification.getBarangay());
            intent.putExtra("subsidyId", notification.getSubsidyId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening notification details", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening notification details", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showErrorState(String message) {
        Log.d(TAG, "Showing error state: " + message);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }
        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
        if (emptyStateText != null) {
            emptyStateText.setText(message);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        Log.d(TAG, "Updating empty state: " + isEmpty + " for barangay: " + currentBarangay);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (isEmpty) {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
            if (emptyStateText != null) {
                String message = currentBarangay != null ?
                        "No notifications available for " + currentBarangay :
                        "No notifications available";
                emptyStateText.setText(message);
            }
        } else {
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted");
            }
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS permission not required for this Android version");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getContext() != null) {
            Log.d(TAG, "Creating notification channel");

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Subsidy Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for subsidy applications in your barangay");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 250, 250, 250});

            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created successfully");
            } else {
                Log.e(TAG, "NotificationManager is null");
            }
        } else {
            Log.d(TAG, "Notification channel not required for this Android version");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Notification permission granted", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "Notification permission denied");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Notification permission denied. You won't receive push notifications.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
