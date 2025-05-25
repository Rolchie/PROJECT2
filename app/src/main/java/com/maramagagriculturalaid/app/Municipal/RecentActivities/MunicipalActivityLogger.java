package com.maramagagriculturalaid.app.Municipal.RecentActivities;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * MunicipalActivityLogger - Utility class for logging municipal subsidy decisions
 * to the Recent Activities collection within each Barangay document.
 *
 * This logger specifically tracks subsidy approval and rejection decisions:
 * - Approved Applications
 * - Rejected Applications
 */
public class MunicipalActivityLogger {

    private static final String TAG = "MunicipalActivityLogger";
    private static final String RECENT_ACTIVITIES_COLLECTION = "Recent Activities";
    private static final int MAX_ACTIVITIES_PER_BARANGAY = 30; // Limit to prevent excessive data

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Log when a subsidy application is approved
     */
    public static void logSubsidyApproved(String barangay, String farmerName, String subsidyType, String applicationId) {
        if (barangay == null || barangay.isEmpty() || farmerName == null || farmerName.isEmpty()) {
            Log.w(TAG, "Cannot log subsidy approval - missing barangay or farmer name");
            return;
        }

        String description = "Approved " + farmerName + "'s application";
        if (subsidyType != null && !subsidyType.isEmpty()) {
            description += " for " + subsidyType;
        }

        Map<String, Object> activityData = new HashMap<>();
        activityData.put("type", "subsidy_approved");
        activityData.put("title", "Approved an Application");
        activityData.put("description", description);
        activityData.put("farmerName", farmerName);
        activityData.put("barangay", barangay);
        activityData.put("subsidyType", subsidyType != null ? subsidyType : "General Subsidy");
        activityData.put("applicationId", applicationId != null ? applicationId : "");
        activityData.put("timestamp", Timestamp.now());
        activityData.put("icon", "check_circle");
        activityData.put("priority", "high");
        activityData.put("status", "approved");

        logActivity(barangay, activityData);
        Log.d(TAG, "Logged subsidy approval: " + farmerName + " in " + barangay + " (" + subsidyType + ")");
    }

    /**
     * Log when a subsidy application is rejected
     */
    public static void logSubsidyRejected(String barangay, String farmerName, String subsidyType, String applicationId, String reason) {
        if (barangay == null || barangay.isEmpty() || farmerName == null || farmerName.isEmpty()) {
            Log.w(TAG, "Cannot log subsidy rejection - missing barangay or farmer name");
            return;
        }

        String description = "Rejected " + farmerName + "'s application";
        if (subsidyType != null && !subsidyType.isEmpty()) {
            description += " for " + subsidyType;
        }
        if (reason != null && !reason.isEmpty()) {
            description += " - Reason: " + reason;
        }

        Map<String, Object> activityData = new HashMap<>();
        activityData.put("type", "subsidy_rejected");
        activityData.put("title", "Rejected an Application");
        activityData.put("description", description);
        activityData.put("farmerName", farmerName);
        activityData.put("barangay", barangay);
        activityData.put("subsidyType", subsidyType != null ? subsidyType : "General Subsidy");
        activityData.put("applicationId", applicationId != null ? applicationId : "");
        activityData.put("rejectionReason", reason != null ? reason : "");
        activityData.put("timestamp", Timestamp.now());
        activityData.put("icon", "cancel");
        activityData.put("priority", "high");
        activityData.put("status", "rejected");

        logActivity(barangay, activityData);
        Log.d(TAG, "Logged subsidy rejection: " + farmerName + " in " + barangay + " (" + subsidyType + ")");
    }

    /**
     * Log when a subsidy application status is updated (for other status changes)
     */
    public static void logSubsidyStatusUpdated(String barangay, String farmerName, String subsidyType, String applicationId, String oldStatus, String newStatus) {
        if (barangay == null || barangay.isEmpty() || farmerName == null || farmerName.isEmpty()) {
            Log.w(TAG, "Cannot log subsidy status update - missing barangay or farmer name");
            return;
        }

        // Only log if it's an approval or rejection
        if ("approved".equalsIgnoreCase(newStatus)) {
            logSubsidyApproved(barangay, farmerName, subsidyType, applicationId);
        } else if ("rejected".equalsIgnoreCase(newStatus)) {
            logSubsidyRejected(barangay, farmerName, subsidyType, applicationId, "Status updated to rejected");
        }
        // Don't log other status changes like "pending", "under review", etc.
    }

    /**
     * Core method to log activity to Firestore
     */
    private static void logActivity(String barangay, Map<String, Object> activityData) {
        if (barangay == null || barangay.isEmpty() || activityData == null) {
            Log.w(TAG, "Cannot log activity - invalid parameters");
            return;
        }

        try {
            // Generate a unique document ID using timestamp and type
            String activityId = "activity_" + System.currentTimeMillis() + "_" + activityData.get("type");

            // Reference to the barangay's Recent Activities collection
            DocumentReference activityRef = db.collection("Barangays")
                    .document(barangay)
                    .collection(RECENT_ACTIVITIES_COLLECTION)
                    .document(activityId);

            // Add the activity
            activityRef.set(activityData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Activity logged successfully: " + activityId);
                        // Clean up old activities to prevent excessive data
                        cleanupOldActivities(barangay);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to log activity: " + activityId, e);
                    });

            // Update the barangay document's last activity timestamp
            updateBarangayLastActivity(barangay);

        } catch (Exception e) {
            Log.e(TAG, "Exception while logging activity", e);
        }
    }

    /**
     * Update the barangay document with the last activity timestamp
     */
    private static void updateBarangayLastActivity(String barangay) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("lastActivityTimestamp", Timestamp.now());
        updateData.put("lastUpdated", Timestamp.now());

        db.collection("Barangays").document(barangay)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Updated barangay last activity timestamp: " + barangay);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update barangay last activity timestamp", e);
                });
    }

    /**
     * Clean up old activities to prevent excessive data accumulation
     * Keeps only the most recent activities based on MAX_ACTIVITIES_PER_BARANGAY
     */
    private static void cleanupOldActivities(String barangay) {
        db.collection("Barangays")
                .document(barangay)
                .collection(RECENT_ACTIVITIES_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() > MAX_ACTIVITIES_PER_BARANGAY) {
                        Log.d(TAG, "Cleaning up old activities for " + barangay +
                                ". Current count: " + queryDocumentSnapshots.size());

                        // Delete activities beyond the limit
                        for (int i = MAX_ACTIVITIES_PER_BARANGAY; i < queryDocumentSnapshots.size(); i++) {
                            queryDocumentSnapshots.getDocuments().get(i).getReference().delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Deleted old activity");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.w(TAG, "Failed to delete old activity", e);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to cleanup old activities", e);
                });
    }

    /**
     * Get the count of recent activities for a barangay
     */
    public static void getActivityCount(String barangay, ActivityCountCallback callback) {
        if (barangay == null || barangay.isEmpty() || callback == null) {
            Log.w(TAG, "Cannot get activity count - invalid parameters");
            return;
        }

        db.collection("Barangays")
                .document(barangay)
                .collection(RECENT_ACTIVITIES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    callback.onCountReceived(count);
                    Log.d(TAG, "Activity count for " + barangay + ": " + count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get activity count for " + barangay, e);
                    callback.onCountReceived(0);
                });
    }

    /**
     * Interface for activity count callback
     */
    public interface ActivityCountCallback {
        void onCountReceived(int count);
    }

    /**
     * Get recent subsidy decisions for a barangay
     */
    public static void getRecentSubsidyDecisions(String barangay, int limit, SubsidyDecisionsCallback callback) {
        if (barangay == null || barangay.isEmpty() || callback == null) {
            Log.w(TAG, "Cannot get recent decisions - invalid parameters");
            return;
        }

        db.collection("Barangays")
                .document(barangay)
                .collection(RECENT_ACTIVITIES_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onDecisionsReceived(queryDocumentSnapshots);
                    Log.d(TAG, "Retrieved " + queryDocumentSnapshots.size() + " recent decisions for " + barangay);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get recent decisions for " + barangay, e);
                    callback.onDecisionsReceived(null);
                });
    }

    /**
     * Interface for subsidy decisions callback
     */
    public interface SubsidyDecisionsCallback {
        void onDecisionsReceived(com.google.firebase.firestore.QuerySnapshot decisions);
    }

    /**
     * Clear all activities for a barangay (use with caution)
     */
    public static void clearAllActivities(String barangay, ClearActivitiesCallback callback) {
        if (barangay == null || barangay.isEmpty()) {
            Log.w(TAG, "Cannot clear activities - missing barangay");
            if (callback != null) callback.onClearComplete(false);
            return;
        }

        db.collection("Barangays")
                .document(barangay)
                .collection(RECENT_ACTIVITIES_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No activities to clear for " + barangay);
                        if (callback != null) callback.onClearComplete(true);
                        return;
                    }

                    int totalActivities = queryDocumentSnapshots.size();
                    final int[] deletedCount = {0};

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    deletedCount[0]++;
                                    if (deletedCount[0] >= totalActivities) {
                                        Log.d(TAG, "Cleared all activities for " + barangay);
                                        if (callback != null) callback.onClearComplete(true);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete activity", e);
                                    deletedCount[0]++;
                                    if (deletedCount[0] >= totalActivities) {
                                        if (callback != null) callback.onClearComplete(false);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get activities for clearing", e);
                    if (callback != null) callback.onClearComplete(false);
                });
    }

    /**
     * Interface for clear activities callback
     */
    public interface ClearActivitiesCallback {
        void onClearComplete(boolean success);
    }
}
