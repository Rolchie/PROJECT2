package com.maramagagriculturalaid.app.Notification;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationDetailActivity extends AppCompatActivity {

    private static final String TAG = "NotificationDetail";

    private TextView tvStatus, tvDate, tvSubsidyId;
    private TextView tvFarmerId, tvFarmerName, tvBarangay, tvFarmType;
    private TextView tvCrops, tvLivestock, tvDetails;
    private TextView cropsLabel, livestockLabel;
    private Button btnDelete;
    private FirebaseFirestore db;
    private String notificationId;
    private String barangay;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_detail);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get notification ID and barangay from intent
        notificationId = getIntent().getStringExtra("notificationId");
        barangay = getIntent().getStringExtra("barangay");

        if (notificationId == null || barangay == null) {
            Toast.makeText(this, "Error: Missing notification information", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Missing notificationId or barangay: " +
                    "notificationId=" + notificationId + ", barangay=" + barangay);
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Load notification data
        loadNotificationData();

        // Set up back button
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        // Set up delete button
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void initializeViews() {
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

        // Details section
        tvDetails = findViewById(R.id.tv_details);

        // Delete button
        btnDelete = findViewById(R.id.btn_delete);
    }

    private void loadNotificationData() {
        // Use the correct Firestore path
        db.collection("Barangays")
                .document(barangay)
                .collection("Notifications")
                .document(notificationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Notification document found: " + documentSnapshot.getId());

                        // Set basic notification info
                        String status = documentSnapshot.getString("status");
                        tvStatus.setText(status);
                        setStatusBackground(status);

                        // Get farmer information
                        String farmerId = documentSnapshot.getString("farmerId");
                        tvFarmerId.setText(farmerId);

                        // Get name components
                        String firstName = documentSnapshot.getString("FirstName");
                        String lastName = documentSnapshot.getString("LastName");
                        String middleInitial = documentSnapshot.getString("MiddleInitial");

                        // Construct full name
                        StringBuilder fullName = new StringBuilder();
                        if (firstName != null) fullName.append(firstName);
                        if (middleInitial != null && !middleInitial.isEmpty()) {
                            fullName.append(" ").append(middleInitial);
                        }
                        if (lastName != null) fullName.append(" ").append(lastName);

                        tvFarmerName.setText(fullName.toString());
                        tvBarangay.setText(documentSnapshot.getString("barangay"));

                        // Set farm details
                        String farmType = documentSnapshot.getString("farmType");
                        tvFarmType.setText(farmType != null ? farmType : "Not specified");

                        // Set subsidy details
                        tvSubsidyId.setText(documentSnapshot.getString("subsidyId"));

                        // Format date if available
                        Date timestamp = documentSnapshot.getDate("timestamp");
                        if (timestamp != null) {
                            tvDate.setText(dateFormat.format(timestamp));
                        } else {
                            tvDate.setText("Unknown date");
                        }

                        // Format the details message with the requested format
                        // "The subsidy application of (farmerID): Lastname, Firstname has been approved/rejected"
                        String formattedMessage = "The subsidy application of (" +
                                (farmerId != null ? farmerId : "Unknown") + "): " +
                                (lastName != null ? lastName : "") + ", " +
                                (firstName != null ? firstName : "") +
                                " has been " + (status != null ? status.toLowerCase() : "processed") + ".";

                        // Add additional information based on status
                        if ("Approved".equalsIgnoreCase(status)) {
                            formattedMessage += " The farmer can now claim their subsidy.";
                        } else if ("Rejected".equalsIgnoreCase(status)) {
                            formattedMessage += " The farmer may reapply or contact the office for more information.";
                        }

                        tvDetails.setText(formattedMessage);

                        // Load subsidy details for more information
                        loadSubsidyDetails(documentSnapshot.getString("subsidyId"));

                    } else {
                        Log.e(TAG, "Notification document does not exist");
                        Toast.makeText(NotificationDetailActivity.this,
                                "Notification not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading notification: " + e.getMessage());
                    Toast.makeText(NotificationDetailActivity.this,
                            "Error loading notification: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSubsidyDetails(String subsidyId) {
        if (subsidyId == null || subsidyId.isEmpty()) {
            Log.w(TAG, "No subsidyId provided, skipping subsidy details");
            return;
        }

        db.collection("Barangays")
                .document(barangay)
                .collection("SubsidyRequests")
                .document(subsidyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Subsidy document found: " + documentSnapshot.getId());

                        // Update farm type if not already set
                        if (tvFarmType.getText().toString().equals("Not specified")) {
                            String farmType = documentSnapshot.getString("farmType");
                            tvFarmType.setText(farmType != null ? farmType : "Not specified");
                        }

                        // Check for crops
                        String crops = documentSnapshot.getString("crops");
                        if (crops != null && !crops.isEmpty()) {
                            cropsLabel.setVisibility(View.VISIBLE);
                            tvCrops.setVisibility(View.VISIBLE);
                            tvCrops.setText(crops);
                        }

                        // Check for livestock
                        String livestock = documentSnapshot.getString("livestock");
                        if (livestock != null && !livestock.isEmpty()) {
                            livestockLabel.setVisibility(View.VISIBLE);
                            tvLivestock.setVisibility(View.VISIBLE);
                            tvLivestock.setText(livestock);
                        }

                        // Add any additional subsidy details you want to display
                        String supportType = documentSnapshot.getString("supportType");
                        if (supportType != null && !supportType.isEmpty() && tvDetails != null) {
                            String currentDetails = tvDetails.getText().toString();
                            tvDetails.setText(currentDetails + "\n\nSupport Type: " + supportType);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading subsidy details: " + e.getMessage());
                });
    }

    private void setStatusBackground(String status) {
        int backgroundColor;
        int textColor;

        if (status != null) {
            if (status.equalsIgnoreCase("Approved")) {
                // Use a green background with black text
                backgroundColor = getResources().getColor(R.color.ButtonGreen);
                textColor = getResources().getColor(android.R.color.white);
            } else if (status.equalsIgnoreCase("Pending")) {
                // Use a yellow/orange background with black text
                backgroundColor = getResources().getColor(R.color.orange);
                textColor = getResources().getColor(android.R.color.black);
            } else if (status.equalsIgnoreCase("Rejected")) {
                // Use a red background with white text
                backgroundColor = getResources().getColor(R.color.red);
                textColor = getResources().getColor(android.R.color.white);
            } else {
                // Default to blue background with white text
                backgroundColor = getResources().getColor(android.R.color.holo_blue_light);
                textColor = getResources().getColor(android.R.color.white);
            }
        } else {
            backgroundColor = getResources().getColor(android.R.color.holo_blue_light);
            textColor = getResources().getColor(android.R.color.white);
        }

        // Apply the background color
        tvStatus.setBackgroundColor(backgroundColor);
        tvStatus.setTextColor(textColor);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotification())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotification() {
        db.collection("Barangays")
                .document(barangay)
                .collection("Notifications")
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification successfully deleted");
                    Toast.makeText(NotificationDetailActivity.this,
                            "Notification deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting notification: " + e.getMessage());
                    Toast.makeText(NotificationDetailActivity.this,
                            "Error deleting notification", Toast.LENGTH_SHORT).show();
                });
    }
}