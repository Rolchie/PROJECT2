package com.maramagagriculturalaid.app;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ActivityLogger utility class for logging various activities in the Agricultural Aid application.
 * This class provides static methods to log different types of activities to the Recent Activities
 * collection in Firestore for each barangay.
 */
public class ActivityLogger {
    private static final String TAG = "ActivityLogger";
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Log when a new farmer is added to the system
     * @param barangay The barangay where the farmer was added
     * @param farmerName The name of the farmer that was added
     */
    public static void logFarmerAdded(String barangay, String farmerName) {
        logActivity(barangay, "farmer_added", "Added farmer", farmerName, null);
    }

    /**
     * Log when a farmer's information is edited/updated
     * @param barangay The barangay where the farmer was edited
     * @param farmerName The name of the farmer that was edited
     */
    public static void logFarmerEdited(String barangay, String farmerName) {
        logActivity(barangay, "farmer_edited", "Edited farmer", farmerName, null);
    }

    /**
     * Log when a farmer is removed from the system
     * @param barangay The barangay where the farmer was removed
     * @param farmerName The name of the farmer that was removed
     */
    public static void logFarmerRemoved(String barangay, String farmerName) {
        logActivity(barangay, "farmer_removed", "Removed farmer", farmerName, null);
    }

    /**
     * Log when a new subsidy application is submitted
     * @param barangay The barangay where the subsidy was applied
     * @param farmerName The name of the farmer applying for subsidy
     * @param supportType The type of support requested (e.g., "Seeds and Fertilizers", "Livestock Support")
     */
    public static void logSubsidyAdded(String barangay, String farmerName, String supportType) {
        String description = farmerName;
        if (supportType != null && !supportType.trim().isEmpty()) {
            description += " - " + supportType;
        }
        logActivity(barangay, "subsidy_added", "Added subsidy application", description, supportType);
    }

    /**
     * Log when a subsidy application status is changed (approved/rejected)
     * @param barangay The barangay where the subsidy status was changed
     * @param farmerName The name of the farmer whose subsidy status changed
     * @param newStatus The new status (e.g., "Approved", "Rejected")
     * @param supportType The type of support (optional)
     */
    public static void logSubsidyStatusChanged(String barangay, String farmerName, String newStatus, String supportType) {
        String description = farmerName + " - " + newStatus;
        if (supportType != null && !supportType.trim().isEmpty()) {
            description += " (" + supportType + ")";
        }
        logActivity(barangay, "subsidy_status_changed", "Subsidy " + newStatus.toLowerCase(), description, supportType);
    }

    /**
     * Log when a farmer's farm type is changed
     * @param barangay The barangay where the farm type was changed
     * @param farmerName The name of the farmer whose farm type changed
     * @param newFarmType The new farm type (e.g., "Crop", "Livestock", "Mixed")
     */
    public static void logFarmTypeChanged(String barangay, String farmerName, String newFarmType) {
        String description = farmerName + " - Changed to " + newFarmType;
        logActivity(barangay, "farm_type_changed", "Updated farm type", description, newFarmType);
    }

    /**
     * Log when crops are updated for a farmer
     * @param barangay The barangay where crops were updated
     * @param farmerName The name of the farmer whose crops were updated
     * @param cropsGrown The new crops grown
     */
    public static void logCropsUpdated(String barangay, String farmerName, String cropsGrown) {
        String description = farmerName + " - " + cropsGrown;
        logActivity(barangay, "crops_updated", "Updated crops", description, cropsGrown);
    }

    /**
     * Log when livestock information is updated for a farmer
     * @param barangay The barangay where livestock was updated
     * @param farmerName The name of the farmer whose livestock was updated
     * @param livestockType The type of livestock
     * @param livestockCount The number of livestock (optional)
     */
    public static void logLivestockUpdated(String barangay, String farmerName, String livestockType, String livestockCount) {
        String description = farmerName + " - " + livestockType;
        if (livestockCount != null && !livestockCount.trim().isEmpty()) {
            description += " (" + livestockCount + ")";
        }
        logActivity(barangay, "livestock_updated", "Updated livestock", description, livestockType);
    }

    /**
     * Log a custom activity with specific parameters
     * @param barangay The barangay where the activity occurred
     * @param type The type of activity (for filtering/categorization)
     * @param title The title/action of the activity
     * @param description The description of the activity
     * @param additionalInfo Additional information (optional)
     */
    public static void logCustomActivity(String barangay, String type, String title, String description, String additionalInfo) {
        logActivity(barangay, type, title, description, additionalInfo);
    }

    /**
     * Core method that handles the actual logging to Firestore
     * @param barangay The barangay where the activity occurred
     * @param type The type of activity
     * @param title The title of the activity
     * @param description The description of the activity
     * @param cropsGrown Additional information (used for crops/support type)
     */
    private static void logActivity(String barangay, String type, String title, String description, String cropsGrown) {
        // Validate required parameters
        if (barangay == null || barangay.trim().isEmpty()) {
            Log.w(TAG, "Cannot log activity - barangay is null or empty");
            return;
        }

        if (type == null || type.trim().isEmpty()) {
            Log.w(TAG, "Cannot log activity - type is null or empty");
            return;
        }

        if (title == null || title.trim().isEmpty()) {
            Log.w(TAG, "Cannot log activity - title is null or empty");
            return;
        }

        if (description == null || description.trim().isEmpty()) {
            Log.w(TAG, "Cannot log activity - description is null or empty");
            return;
        }

        try {
            // Create activity data map
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", type.trim());
            activity.put("title", title.trim());
            activity.put("description", description.trim());
            activity.put("timestamp", System.currentTimeMillis());

            // Extract farmer name from description if it contains " - "
            String farmerName = description.trim();
            if (description.contains(" - ")) {
                farmerName = description.split(" - ")[0].trim();
            }
            activity.put("farmerName", farmerName);

            // Add crops/support type information if provided
            if (cropsGrown != null && !cropsGrown.trim().isEmpty()) {
                activity.put("cropsGrown", cropsGrown.trim());
            }

            // Add barangay information for reference
            activity.put("barangay", barangay.trim());

            // Log to Firestore
            db.collection("Barangays")
                    .document(barangay.trim())
                    .collection("Recent Activities")
                    .add(activity)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "✓ Activity logged successfully: " + title + " - " + description);
                        Log.d(TAG, "  Document ID: " + documentReference.getId());
                        Log.d(TAG, "  Barangay: " + barangay);
                        Log.d(TAG, "  Type: " + type);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "✗ Error logging activity: " + title, e);
                        Log.e(TAG, "  Barangay: " + barangay);
                        Log.e(TAG, "  Type: " + type);
                        Log.e(TAG, "  Description: " + description);
                    });

        } catch (Exception e) {
            Log.e(TAG, "✗ Unexpected error while logging activity", e);
            Log.e(TAG, "  Barangay: " + barangay);
            Log.e(TAG, "  Type: " + type);
            Log.e(TAG, "  Title: " + title);
            Log.e(TAG, "  Description: " + description);
        }
    }

    /**
     * Method to clean up old activities (optional - can be called periodically)
     * Removes activities older than the specified number of days
     * @param barangay The barangay to clean up activities for
     * @param daysToKeep Number of days to keep activities (default: 30 days)
     */
    public static void cleanupOldActivities(String barangay, int daysToKeep) {
        if (barangay == null || barangay.trim().isEmpty()) {
            Log.w(TAG, "Cannot cleanup activities - barangay is null or empty");
            return;
        }

        if (daysToKeep <= 0) {
            daysToKeep = 30; // Default to 30 days
        }

        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L);

        db.collection("Barangays")
                .document(barangay.trim())
                .collection("Recent Activities")
                .whereLessThan("timestamp", cutoffTime)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int deleteCount = queryDocumentSnapshots.size();
                    if (deleteCount > 0) {
                        Log.d(TAG, "Cleaning up " + deleteCount + " old activities for barangay: " + barangay);

                        // Delete old activities
                        queryDocumentSnapshots.forEach(document -> {
                            document.getReference().delete()
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error deleting old activity: " + document.getId(), e);
                                    });
                        });

                        Log.d(TAG, "✓ Cleanup completed for barangay: " + barangay);
                    } else {
                        Log.d(TAG, "No old activities to cleanup for barangay: " + barangay);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error during cleanup for barangay: " + barangay, e);
                });
    }

    public interface ActivityStatsCallback {
        void onStatsReceived(Map<String, Integer> stats);
        void onError(Exception e);
    }

    public static void getActivityStats(String barangay, ActivityStatsCallback callback) {
        if (barangay == null || barangay.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Barangay cannot be null or empty"));
            return;
        }

        db.collection("Barangays")
                .document(barangay.trim())
                .collection("Recent Activities")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> stats = new HashMap<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String type = document.getString("type");
                        if (type != null) {
                            stats.put(type, stats.getOrDefault(type, 0) + 1);
                        }
                    }

                    callback.onStatsReceived(stats);
                })
                .addOnFailureListener(callback::onError);
    }
}