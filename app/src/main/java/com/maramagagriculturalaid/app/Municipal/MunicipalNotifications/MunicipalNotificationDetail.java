package com.maramagagriculturalaid.app.Municipal.MunicipalNotifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.Notification.Notification;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MunicipalNotificationDetail extends AppCompatActivity {

    private static final String TAG = "MunicipalNotificationDetail";

    // UI Components
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvStatus, tvDate, tvSubsidyId;
    private TextView tvFarmerId, tvFarmerName, tvBarangay, tvFarmType;
    private TextView tvCrops, tvLivestock, tvDetails;
    private TextView cropsLabel, livestockLabel;
    private CardView supportDetailsCard;
    private TextView tvSupportDetails;
    private AppCompatButton btnDelete;

    // Data
    private FirebaseFirestore db;
    private String notificationId;
    private String barangay;
    private Notification notification;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_municipal_notification_detail);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        extractIntentData();

        // Validate required data
        if (!validateIntentData()) {
            return;
        }

        // Initialize views
        initializeViews();

        // Setup click listeners
        setupClickListeners();

        // Load notification data
        loadNotificationData();
    }

    private void extractIntentData() {
        notificationId = getIntent().getStringExtra("notificationId");
        barangay = getIntent().getStringExtra("barangay");

        // Try to reconstruct notification object from individual extras
        if (getIntent().hasExtra("farmerId")) {
            try {
                notification = new Notification();
                notification.setId(notificationId);
                notification.setFarmerId(getIntent().getStringExtra("farmerId"));
                notification.setFirstName(getIntent().getStringExtra("firstName"));
                notification.setLastName(getIntent().getStringExtra("lastName"));
                notification.setMiddleInitial(getIntent().getStringExtra("middleInitial"));
                notification.setStatus(getIntent().getStringExtra("status"));
                notification.setSubsidyId(getIntent().getStringExtra("subsidyId"));
                notification.setFarmType(getIntent().getStringExtra("farmType"));
                notification.setBarangay(barangay);
                notification.setRead(getIntent().getBooleanExtra("read", false));

                long timestampLong = getIntent().getLongExtra("timestamp", 0L);
                if (timestampLong > 0) {
                    notification.setTimestamp(new Date(timestampLong));
                }

                Log.d(TAG, "Notification object reconstructed from intent extras");
            } catch (Exception e) {
                Log.w(TAG, "Could not reconstruct notification object from intent", e);
                notification = null;
            }
        }

        Log.d(TAG, "Intent data - notificationId: " + notificationId + ", barangay: " + barangay);
    }

    private boolean validateIntentData() {
        if (notificationId == null || notificationId.isEmpty()) {
            Toast.makeText(this, "Error: Missing notification ID", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Missing notificationId");
            finish();
            return false;
        }

        return true;
    }

    private void initializeViews() {
        // Header
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);

        // Status section
        tvStatus = findViewById(R.id.tv_status);
        tvDate = findViewById(R.id.tv_date);
        tvSubsidyId = findViewById(R.id.tv_subsidy_id);

        // Farmer details section
        tvFarmerId = findViewById(R.id.tv_farmer_id);
        tvFarmerName = findViewById(R.id.tv_farmer_name);
        tvBarangay = findViewById(R.id.tv_barangay);
        tvFarmType = findViewById(R.id.tv_farm_type);

        // Optional fields
        tvCrops = findViewById(R.id.tv_crops);
        tvLivestock = findViewById(R.id.tv_livestock);
        cropsLabel = findViewById(R.id.crops_label);
        livestockLabel = findViewById(R.id.livestock_label);

        // Support details - now using CardView
        supportDetailsCard = findViewById(R.id.support_details_card);
        tvSupportDetails = findViewById(R.id.tv_support_details);

        // Details section
        tvDetails = findViewById(R.id.tv_details);

        // Delete button
        btnDelete = findViewById(R.id.btn_delete);

        // Initially hide optional sections
        hideOptionalSections();
    }

    private void hideOptionalSections() {
        if (cropsLabel != null) cropsLabel.setVisibility(View.GONE);
        if (tvCrops != null) tvCrops.setVisibility(View.GONE);
        if (livestockLabel != null) livestockLabel.setVisibility(View.GONE);
        if (tvLivestock != null) tvLivestock.setVisibility(View.GONE);
        if (supportDetailsCard != null) supportDetailsCard.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }
    }

    private void loadNotificationData() {
        // If we already have the notification object, use it
        if (notification != null) {
            populateNotificationData(notification);
            return;
        }

        // Otherwise, load from Firestore (Municipal collection)
        Log.d(TAG, "Loading notification from Municipal Firestore: " + notificationId);

        db.collection("Municipal")
                .document("Maramag")
                .collection("Municipal Notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Municipal notification document found: " + documentSnapshot.getId());

                        try {
                            Notification loadedNotification = documentSnapshot.toObject(Notification.class);
                            if (loadedNotification != null) {
                                loadedNotification.setId(documentSnapshot.getId());
                                populateNotificationData(loadedNotification);
                            } else {
                                Log.e(TAG, "Failed to convert document to Notification object");
                                showErrorAndFinish("Error loading notification data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error converting document to Notification", e);
                            showErrorAndFinish("Error parsing notification data");
                        }
                    } else {
                        Log.e(TAG, "Municipal notification document does not exist");
                        showErrorAndFinish("Notification not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading municipal notification: " + e.getMessage(), e);
                    showErrorAndFinish("Error loading notification: " + e.getMessage());
                });
    }

    private void populateNotificationData(Notification notification) {
        this.notification = notification;

        try {
            // Set status
            String status = notification.getStatus();
            if (tvStatus != null) {
                tvStatus.setText(status != null ? status : "Pending");
                setStatusBackground(status);
            }

            // Set date
            if (tvDate != null) {
                Date timestamp = notification.getTimestamp();
                if (timestamp != null) {
                    tvDate.setText(dateFormat.format(timestamp));
                } else {
                    tvDate.setText("Unknown date");
                }
            }

            // Set subsidy ID
            if (tvSubsidyId != null) {
                String subsidyId = notification.getSubsidyId();
                tvSubsidyId.setText(subsidyId != null ? subsidyId : "N/A");
            }

            // Set farmer details
            if (tvFarmerId != null) {
                String farmerId = notification.getFarmerId();
                tvFarmerId.setText(farmerId != null ? farmerId : "N/A");
            }

            if (tvFarmerName != null) {
                String fullName = notification.getFullName();
                tvFarmerName.setText(fullName != null ? fullName : "Unknown");
            }

            if (tvBarangay != null) {
                String barangayName = notification.getBarangay();
                tvBarangay.setText(barangayName != null ? barangayName : "Unknown");
            }

            if (tvFarmType != null) {
                String farmType = notification.getFarmType();
                tvFarmType.setText(farmType != null && !farmType.isEmpty() ? farmType : "Not specified");
            }

            // Set notification details
            if (tvDetails != null) {
                String formattedMessage = generateDetailMessage(notification);
                tvDetails.setText(formattedMessage);
            }

            // Show support details if available
            showSupportDetails(notification);

            Log.d(TAG, "Successfully populated municipal notification data");

        } catch (Exception e) {
            Log.e(TAG, "Error populating notification data", e);
            showErrorAndFinish("Error displaying notification data");
        }
    }

    private String generateDetailMessage(Notification notification) {
        String status = notification.getStatus();
        String fullName = notification.getFullName();
        String barangayName = notification.getBarangay();
        String farmType = notification.getFarmType();

        StringBuilder message = new StringBuilder();

        if ("subsidy_application".equals(notification.getType()) || notification.getSubsidyId() != null) {
            message.append("Subsidy application submitted by ");
            message.append(fullName != null ? fullName : "Unknown Farmer");
            message.append(" from ").append(barangayName != null ? barangayName : "Unknown Barangay");
            message.append(" barangay.");

            if (farmType != null && !farmType.isEmpty()) {
                message.append("\n\nFarm Type: ").append(farmType);
            }

            if ("Pending".equalsIgnoreCase(status)) {
                message.append("\n\nThis application is awaiting municipal review and approval.");
            } else if ("Approved".equalsIgnoreCase(status)) {
                message.append("\n\nThis application has been approved. The farmer can now claim their subsidy benefits.");
            } else if ("Rejected".equalsIgnoreCase(status)) {
                message.append("\n\nThis application has been rejected. The farmer may reapply or contact the office for more information.");
            }
        } else {
            // Handle other notification types
            message.append("Notification from ").append(barangayName != null ? barangayName : "Unknown Barangay");
            message.append(" regarding ").append(fullName != null ? fullName : "Unknown Farmer");
        }

        return message.toString();
    }

    private void showSupportDetails(Notification notification) {
        // Create support details based on farm type and status
        StringBuilder supportDetails = new StringBuilder();

        String farmType = notification.getFarmType();
        String status = notification.getStatus();

        if ("Approved".equalsIgnoreCase(status)) {
            supportDetails.append("✓ Subsidy application approved\n");

            if (farmType != null) {
                switch (farmType.toLowerCase()) {
                    case "crop":
                    case "crops":
                        supportDetails.append("• Seeds and fertilizer support\n");
                        supportDetails.append("• Agricultural tools assistance\n");
                        supportDetails.append("• Technical guidance provided");
                        break;
                    case "livestock":
                        supportDetails.append("• Animal feed support\n");
                        supportDetails.append("• Veterinary assistance\n");
                        supportDetails.append("• Livestock management training");
                        break;
                    case "mixed":
                        supportDetails.append("• Comprehensive farming support\n");
                        supportDetails.append("• Seeds, fertilizer, and feed\n");
                        supportDetails.append("• Technical guidance for both crops and livestock");
                        break;
                    default:
                        supportDetails.append("• General agricultural support\n");
                        supportDetails.append("• Technical assistance provided");
                        break;
                }
            }
        } else if ("Rejected".equalsIgnoreCase(status)) {
            supportDetails.append("✗ Subsidy application rejected\n");
            supportDetails.append("• Review application requirements\n");
            supportDetails.append("• Contact office for clarification\n");
            supportDetails.append("• Reapplication possible after addressing issues");
        } else {
            // For other statuses like "Added" or "Updated"
            supportDetails.append("• Farmer registration completed\n");
            supportDetails.append("• Eligible for future subsidy programs\n");
            supportDetails.append("• Contact barangay office for available programs");
        }

        if (supportDetails.length() > 0 && supportDetailsCard != null) {
            supportDetailsCard.setVisibility(View.VISIBLE);
            if (tvSupportDetails != null) {
                tvSupportDetails.setText(supportDetails.toString());
            }
        }
    }

    private void setStatusBackground(String status) {
        if (tvStatus == null || status == null) return;

        int backgroundResource;
        int textColor = getResources().getColor(android.R.color.white);

        switch (status.toLowerCase()) {
            case "approved":
                backgroundResource = R.drawable.status_approved_background;
                textColor = getResources().getColor(android.R.color.white);
                break;
            case "rejected":
                backgroundResource = R.drawable.status_rejected_background;
                textColor = getResources().getColor(android.R.color.white);
                break;
            case "pending":
                backgroundResource = R.drawable.status_pending_background;
                textColor = getResources().getColor(android.R.color.black);
                break;
            case "added":
                backgroundResource = R.drawable.status_approved_background; // Use green for added
                textColor = getResources().getColor(android.R.color.white);
                break;
            case "updated":
                backgroundResource = R.drawable.status_pending_background; // Use orange for updated
                textColor = getResources().getColor(android.R.color.black);
                break;
            default:
                backgroundResource = R.drawable.status_pending_background;
                textColor = getResources().getColor(android.R.color.black);
                break;
        }

        tvStatus.setBackgroundResource(backgroundResource);
        tvStatus.setTextColor(textColor);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotification())
                .setNegativeButton("Cancel", null)
                .setIcon(R.drawable.baseline_delete_24)
                .show();
    }

    private void deleteNotification() {
        if (notificationId == null) {
            Toast.makeText(this, "Error: Cannot delete notification", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Deleting municipal notification: " + notificationId);

        db.collection("Municipal")
                .document("Maramag")
                .collection("Municipal Notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Municipal notification successfully deleted");
                    Toast.makeText(MunicipalNotificationDetail.this,
                            "Notification deleted successfully", Toast.LENGTH_SHORT).show();

                    // Set result to indicate deletion
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("deleted", true);
                    resultIntent.putExtra("notificationId", notificationId);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting municipal notification: " + e.getMessage(), e);
                    Toast.makeText(MunicipalNotificationDetail.this,
                            "Error deleting notification: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
