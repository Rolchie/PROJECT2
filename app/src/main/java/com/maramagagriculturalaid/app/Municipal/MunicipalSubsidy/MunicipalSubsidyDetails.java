package com.maramagagriculturalaid.app.Municipal.MunicipalSubsidy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MunicipalSubsidyDetails extends AppCompatActivity {

    private static final String TAG = "MunicipalSubsidyDetails";

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String subsidyId;
    private String barangayName;
    private String currentStatus = "Pending";
    private ProgressDialog progressDialog;

    // UI Components
    private TextView tvStatus, tvDate, tvSubsidyType;
    private TextView tvFarmerId, tvFarmerName, tvFarmType, tvLocation;

    // Dynamic fields based on farm type
    private TextView tvCropsLabel, tvCrops, tvLotSizeLabel, tvLotSize;
    private TextView tvLivestockTypeLabel, tvLivestockType, tvLivestockCountLabel, tvLivestockCount;

    // Subsidy request details
    private TextView tvSeedsRequestedLabel, tvSeedsRequested;
    private TextView tvFertilizersRequestedLabel, tvFertilizersRequested;
    private TextView tvMonetarySupportLabel, tvMonetarySupport;

    private LinearLayout fileItem;
    private TextView tvFileName;
    private MaterialButton btnApprove, btnReject;
    private ImageButton btnBack, btnDownloadFile;
    private View noFilesView;
    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_municipal_subsidy_details);

        try {
            Log.d(TAG, "=== ONCREATE DEBUG START ===");

            // Initialize Firebase
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            Log.d(TAG, "Firebase initialized");

            // Get data from intent
            subsidyId = getIntent().getStringExtra("subsidyId");
            barangayName = getIntent().getStringExtra("barangay");

            Log.d(TAG, "Intent data - subsidyId: " + subsidyId + ", barangay: " + barangayName);

            if (subsidyId == null) {
                Log.e(TAG, "No subsidy ID provided");
                Toast.makeText(this, "No subsidy ID provided", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize progress dialog
            initializeProgressDialog();
            Log.d(TAG, "Progress dialog initialized");

            // Initialize views
            initializeViews();
            Log.d(TAG, "Views initialized");

            // Set click listeners
            setupClickListeners();
            Log.d(TAG, "Click listeners setup");

            // Load subsidy data
            loadSubsidyData();
            Log.d(TAG, "Loading subsidy data...");

            Log.d(TAG, "=== ONCREATE DEBUG END ===");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeProgressDialog() {
        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Processing...");
            progressDialog.setCancelable(false);
            Log.d(TAG, "Progress dialog created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating progress dialog", e);
        }
    }

    private void initializeViews() {
        try {
            Log.d(TAG, "=== VIEW INITIALIZATION DEBUG ===");

            // Header views
            btnBack = findViewById(R.id.btn_back);
            Log.d(TAG, "btnBack: " + (btnBack != null ? "FOUND" : "NULL"));

            // Status card views
            tvStatus = findViewById(R.id.tv_status);
            tvDate = findViewById(R.id.tv_date);
            tvSubsidyType = findViewById(R.id.tv_subsidy_type);
            Log.d(TAG, "Status views - tvStatus: " + (tvStatus != null ? "FOUND" : "NULL"));

            // Basic farmer details views
            tvFarmerId = findViewById(R.id.tv_farmer_id);
            tvFarmerName = findViewById(R.id.tv_farmer_name);
            tvFarmType = findViewById(R.id.tv_farm_type);
            tvLocation = findViewById(R.id.tv_location);
            Log.d(TAG, "Farmer detail views initialized");

            // Crops-related views
            tvCropsLabel = findViewById(R.id.crops_label);
            tvCrops = findViewById(R.id.tv_crops);
            tvLotSizeLabel = findViewById(R.id.lot_size_label);
            tvLotSize = findViewById(R.id.tv_lot_size);

            // Livestock-related views
            tvLivestockTypeLabel = findViewById(R.id.livestock_type_label);
            tvLivestockType = findViewById(R.id.tv_livestock_type);
            tvLivestockCountLabel = findViewById(R.id.livestock_count_label);
            tvLivestockCount = findViewById(R.id.tv_livestock_count);

            // Subsidy request details
            tvSeedsRequestedLabel = findViewById(R.id.seeds_requested_label);
            tvSeedsRequested = findViewById(R.id.tv_seeds_requested);
            tvFertilizersRequestedLabel = findViewById(R.id.fertilizers_requested_label);
            tvFertilizersRequested = findViewById(R.id.tv_fertilizers_requested);
            tvMonetarySupportLabel = findViewById(R.id.monetary_support_label);
            tvMonetarySupport = findViewById(R.id.tv_monetary_support);

            // Attached files views
            fileItem = findViewById(R.id.file_item);
            tvFileName = findViewById(R.id.tv_file_name);
            btnDownloadFile = findViewById(R.id.btn_download_file);
            noFilesView = findViewById(R.id.tv_no_files);

            // Action buttons - CRITICAL DEBUG
            btnApprove = findViewById(R.id.btn_approve);
            btnReject = findViewById(R.id.btn_reject);
            buttonContainer = findViewById(R.id.button_container);

            Log.d(TAG, "=== BUTTON DEBUG ===");
            Log.d(TAG, "btnApprove: " + (btnApprove != null ? "FOUND" : "NULL"));
            Log.d(TAG, "btnReject: " + (btnReject != null ? "FOUND" : "NULL"));
            Log.d(TAG, "buttonContainer: " + (buttonContainer != null ? "FOUND" : "NULL"));

            if (btnApprove != null) {
                Log.d(TAG, "btnApprove visibility: " + btnApprove.getVisibility());
                Log.d(TAG, "btnApprove enabled: " + btnApprove.isEnabled());
                Log.d(TAG, "btnApprove clickable: " + btnApprove.isClickable());
            }

            if (btnReject != null) {
                Log.d(TAG, "btnReject visibility: " + btnReject.getVisibility());
                Log.d(TAG, "btnReject enabled: " + btnReject.isEnabled());
                Log.d(TAG, "btnReject clickable: " + btnReject.isClickable());
            }

            if (buttonContainer != null) {
                Log.d(TAG, "buttonContainer visibility: " + buttonContainer.getVisibility());
                Log.d(TAG, "buttonContainer child count: " + buttonContainer.getChildCount());
            }

            Log.d(TAG, "=== VIEW INITIALIZATION COMPLETE ===");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void setupClickListeners() {
        try {
            Log.d(TAG, "=== SETTING UP CLICK LISTENERS ===");

            if (btnBack != null) {
                btnBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "Back button clicked");
                        finish();
                    }
                });
                Log.d(TAG, "Back button listener set");
            } else {
                Log.w(TAG, "btnBack is null, cannot set listener");
            }

            if (btnApprove != null) {
                Log.d(TAG, "Setting approve button listener");
                btnApprove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "APPROVE BUTTON CLICKED!");
                        Toast.makeText(MunicipalSubsidyDetails.this, "Approve button clicked!", Toast.LENGTH_SHORT).show();
                        confirmApproval();
                    }
                });
                Log.d(TAG, "Approve button listener set successfully");

                // Test button immediately
                btnApprove.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Testing approve button state:");
                        Log.d(TAG, "- Visibility: " + btnApprove.getVisibility());
                        Log.d(TAG, "- Enabled: " + btnApprove.isEnabled());
                        Log.d(TAG, "- Clickable: " + btnApprove.isClickable());
                        Log.d(TAG, "- Has OnClickListener: " + btnApprove.hasOnClickListeners());
                    }
                });
            } else {
                Log.e(TAG, "btnApprove is NULL! Cannot set click listener!");
            }

            if (btnReject != null) {
                Log.d(TAG, "Setting reject button listener");
                btnReject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "REJECT BUTTON CLICKED!");
                        Toast.makeText(MunicipalSubsidyDetails.this, "Reject button clicked!", Toast.LENGTH_SHORT).show();
                        confirmRejection();
                    }
                });
                Log.d(TAG, "Reject button listener set successfully");

                // Test button immediately
                btnReject.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Testing reject button state:");
                        Log.d(TAG, "- Visibility: " + btnReject.getVisibility());
                        Log.d(TAG, "- Enabled: " + btnReject.isEnabled());
                        Log.d(TAG, "- Clickable: " + btnReject.isClickable());
                        Log.d(TAG, "- Has OnClickListener: " + btnReject.hasOnClickListeners());
                    }
                });
            } else {
                Log.e(TAG, "btnReject is NULL! Cannot set click listener!");
            }

            Log.d(TAG, "=== CLICK LISTENERS SETUP COMPLETE ===");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    private void confirmApproval() {
        try {
            Log.d(TAG, "confirmApproval() called");

            new AlertDialog.Builder(this)
                    .setTitle("Approve Application")
                    .setMessage("Are you sure you want to approve this subsidy application?")
                    .setPositiveButton("Approve", (dialog, which) -> {
                        Log.d(TAG, "User confirmed approval");
                        dialog.dismiss();
                        processStatusUpdate("Approved");
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d(TAG, "User cancelled approval");
                        dialog.dismiss();
                    })
                    .setCancelable(true)
                    .show();

            Log.d(TAG, "Approval dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing approval confirmation", e);
            Toast.makeText(this, "Error showing confirmation dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRejection() {
        try {
            Log.d(TAG, "confirmRejection() called");

            new AlertDialog.Builder(this)
                    .setTitle("Reject Application")
                    .setMessage("Are you sure you want to reject this subsidy application?")
                    .setPositiveButton("Reject", (dialog, which) -> {
                        Log.d(TAG, "User confirmed rejection");
                        dialog.dismiss();
                        processStatusUpdate("Rejected");
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d(TAG, "User cancelled rejection");
                        dialog.dismiss();
                    })
                    .setCancelable(true)
                    .show();

            Log.d(TAG, "Rejection dialog shown");
        } catch (Exception e) {
            Log.e(TAG, "Error showing rejection confirmation", e);
            Toast.makeText(this, "Error showing confirmation dialog: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void processStatusUpdate(String newStatus) {
        try {
            Log.d(TAG, "processStatusUpdate() called with status: " + newStatus);

            // Show loading dialog
            if (progressDialog != null) {
                progressDialog.setMessage("Updating application status...");
                progressDialog.show();
                Log.d(TAG, "Progress dialog shown");
            }

            // Disable buttons to prevent multiple clicks
            setButtonsEnabled(false);

            Log.d(TAG, "Processing status update to: " + newStatus);
            Log.d(TAG, "Subsidy ID: " + subsidyId);
            Log.d(TAG, "Barangay: " + barangayName);

            // Prepare update data
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", newStatus);
            updates.put("dateProcessed", System.currentTimeMillis());
            updates.put("lastModified", System.currentTimeMillis());

            // Add processing metadata
            if ("Approved".equals(newStatus)) {
                updates.put("approvedAt", System.currentTimeMillis());
                updates.put("processedBy", "Municipal Office");
            } else if ("Rejected".equals(newStatus)) {
                updates.put("rejectedAt", System.currentTimeMillis());
                updates.put("processedBy", "Municipal Office");
            }

            Log.d(TAG, "Update data prepared: " + updates.toString());

            // Update in the correct collection path
            updateSubsidyStatus(updates, newStatus);

        } catch (Exception e) {
            Log.e(TAG, "Error processing status update", e);
            hideProgressDialog();
            setButtonsEnabled(true);
            Toast.makeText(this, "Error processing update: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateSubsidyStatus(Map<String, Object> updates, String newStatus) {
        try {
            Log.d(TAG, "updateSubsidyStatus() called");

            // Try updating in the barangay-specific collection first
            if (barangayName != null && !barangayName.isEmpty()) {
                String path = "Barangays/" + barangayName + "/SubsidyRequests/" + subsidyId;
                Log.d(TAG, "Updating in path: " + path);

                db.collection("Barangays")
                        .document(barangayName)
                        .collection("SubsidyRequests")
                        .document(subsidyId)
                        .update(updates)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "Successfully updated status in barangay collection");
                                handleStatusUpdateSuccess(newStatus);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to update in barangay collection: " + e.getMessage(), e);
                                // Fallback to general collection
                                updateInGeneralCollection(updates, newStatus);
                            }
                        });
            } else {
                // No barangay specified, try general collection
                Log.d(TAG, "No barangay specified, using general collection");
                updateInGeneralCollection(updates, newStatus);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in updateSubsidyStatus", e);
            handleStatusUpdateFailure(e);
        }
    }

    private void updateInGeneralCollection(Map<String, Object> updates, String newStatus) {
        try {
            String path = "SubsidyRequests/" + subsidyId;
            Log.d(TAG, "Updating in general collection path: " + path);

            db.collection("SubsidyRequests")
                    .document(subsidyId)
                    .update(updates)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Successfully updated status in general collection");
                            handleStatusUpdateSuccess(newStatus);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Failed to update in general collection: " + e.getMessage(), e);
                            handleStatusUpdateFailure(e);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error updating in general collection", e);
            handleStatusUpdateFailure(e);
        }
    }

    private void handleStatusUpdateSuccess(String newStatus) {
        try {
            Log.d(TAG, "handleStatusUpdateSuccess() called with status: " + newStatus);

            // Hide progress dialog
            hideProgressDialog();

            // Update local status
            currentStatus = newStatus;

            // Update UI immediately
            runOnUiThread(() -> {
                try {
                    Log.d(TAG, "Updating UI on main thread");

                    // Update status text
                    if (tvStatus != null) {
                        tvStatus.setText(newStatus);
                        Log.d(TAG, "Status text updated to: " + newStatus);
                    }

                    // Update status background
                    updateStatusBackground(newStatus);

                    // Update button visibility - hide buttons for approved/rejected
                    updateButtonVisibility(newStatus);

                    // Re-enable buttons (though they should be hidden now)
                    setButtonsEnabled(true);

                    // Show success message
                    String message = "Application " + newStatus.toLowerCase() + " successfully!";
                    Toast.makeText(MunicipalSubsidyDetails.this, message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Success message shown: " + message);

                } catch (Exception e) {
                    Log.e(TAG, "Error updating UI after successful status update", e);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error handling status update success", e);
            hideProgressDialog();
            setButtonsEnabled(true);
        }
    }

    private void handleStatusUpdateFailure(Exception e) {
        try {
            Log.e(TAG, "handleStatusUpdateFailure() called", e);

            // Hide progress dialog
            hideProgressDialog();

            // Re-enable buttons
            setButtonsEnabled(true);

            // Show error message
            runOnUiThread(() -> {
                String errorMessage = "Failed to update status. Please check your connection and try again.";
                if (e.getMessage() != null) {
                    errorMessage += "\nError: " + e.getMessage();
                }
                Toast.makeText(MunicipalSubsidyDetails.this, errorMessage, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Error message shown: " + errorMessage);
            });

        } catch (Exception ex) {
            Log.e(TAG, "Error handling status update failure", ex);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        try {
            Log.d(TAG, "setButtonsEnabled() called with enabled: " + enabled);

            if (btnApprove != null) {
                btnApprove.setEnabled(enabled);
                btnApprove.setAlpha(enabled ? 1.0f : 0.5f);
                Log.d(TAG, "Approve button enabled: " + enabled);
            }
            if (btnReject != null) {
                btnReject.setEnabled(enabled);
                btnReject.setAlpha(enabled ? 1.0f : 0.5f);
                Log.d(TAG, "Reject button enabled: " + enabled);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting button states", e);
        }
    }

    private void hideProgressDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
                Log.d(TAG, "Progress dialog hidden");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding progress dialog", e);
        }
    }

    // Add a test method to manually trigger button functionality
    public void testButtons() {
        Log.d(TAG, "=== MANUAL BUTTON TEST ===");

        if (btnApprove != null) {
            Log.d(TAG, "Testing approve button manually");
            btnApprove.performClick();
        } else {
            Log.e(TAG, "Cannot test approve button - it's null");
        }

        if (btnReject != null) {
            Log.d(TAG, "Testing reject button manually");
            // Don't actually click, just test
            Log.d(TAG, "Reject button is ready for testing");
        } else {
            Log.e(TAG, "Cannot test reject button - it's null");
        }
    }

    private void loadSubsidyData() {
        try {
            Log.d(TAG, "loadSubsidyData() called");

            // Show loading
            if (progressDialog != null) {
                progressDialog.setMessage("Loading subsidy details...");
                progressDialog.show();
            }

            // Determine the correct path based on whether barangay is provided
            if (barangayName != null && !barangayName.isEmpty()) {
                String path = "Barangays/" + barangayName + "/SubsidyRequests/" + subsidyId;
                Log.d(TAG, "Loading from path: " + path);

                // Load from specific barangay
                db.collection("Barangays")
                        .document(barangayName)
                        .collection("SubsidyRequests")
                        .document(subsidyId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                try {
                                    Log.d(TAG, "Successfully loaded document from barangay collection");
                                    hideProgressDialog();
                                    handleDocumentSnapshot(documentSnapshot);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error handling document snapshot", e);
                                    hideProgressDialog();
                                    showErrorAndFinish("Error processing data");
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to load from barangay collection", e);
                                hideProgressDialog();
                                handleLoadFailure(e);
                            }
                        });
            } else {
                Log.d(TAG, "Loading from general collection");

                // Fallback: try to load from general collection
                db.collection("SubsidyRequests")
                        .document(subsidyId)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                try {
                                    Log.d(TAG, "Successfully loaded document from general collection");
                                    hideProgressDialog();
                                    handleDocumentSnapshot(documentSnapshot);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error handling document snapshot", e);
                                    hideProgressDialog();
                                    showErrorAndFinish("Error processing data");
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to load from general collection", e);
                                hideProgressDialog();
                                handleLoadFailure(e);
                            }
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading subsidy data", e);
            hideProgressDialog();
            showErrorAndFinish("Error loading data");
        }
    }

    private void handleDocumentSnapshot(DocumentSnapshot documentSnapshot) {
        try {
            if (documentSnapshot.exists()) {
                Map<String, Object> subsidyData = documentSnapshot.getData();
                if (subsidyData != null) {
                    Log.d(TAG, "Successfully loaded subsidy data");
                    Log.d(TAG, "Data keys: " + subsidyData.keySet().toString());
                    populateSubsidyData(subsidyData);
                    loadAttachedFiles(subsidyData);
                } else {
                    Log.e(TAG, "Subsidy data is null");
                    showErrorAndFinish("Invalid subsidy data");
                }
            } else {
                Log.e(TAG, "Subsidy document does not exist");
                showErrorAndFinish("Subsidy request not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleDocumentSnapshot", e);
            showErrorAndFinish("Error processing document");
        }
    }

    private void handleLoadFailure(Exception e) {
        Log.e(TAG, "Failed to load subsidy data", e);
        showErrorAndFinish("Failed to load subsidy data: " + e.getMessage());
    }

    private void showErrorAndFinish(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message", e);
        }
    }

    private void populateSubsidyData(Map<String, Object> subsidyData) {
        try {
            Log.d(TAG, "populateSubsidyData() called");

            // Status - Handle status first to control button visibility
            String status = (String) subsidyData.get("status");
            Log.d(TAG, "Raw status from database: '" + status + "'");

            if (status != null && !status.trim().isEmpty()) {
                currentStatus = status.trim();
                if (tvStatus != null) {
                    tvStatus.setText(currentStatus);
                }
            } else {
                Log.d(TAG, "No status found, defaulting to Pending");
                currentStatus = "Pending";
                if (tvStatus != null) {
                    tvStatus.setText(currentStatus);
                }
            }

            Log.d(TAG, "Current status set to: '" + currentStatus + "'");

            // Update UI and button visibility based on status - IMMEDIATELY
            updateButtonVisibility(currentStatus);

            // Date - handle Long timestamp from your database
            Object timestampObj = subsidyData.get("timestamp");
            if (timestampObj != null && tvDate != null) {
                try {
                    Date date;
                    if (timestampObj instanceof Long) {
                        date = new Date((Long) timestampObj);
                    } else if (timestampObj instanceof com.google.firebase.Timestamp) {
                        date = ((com.google.firebase.Timestamp) timestampObj).toDate();
                    } else {
                        date = new Date();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    tvDate.setText(sdf.format(date));
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting date", e);
                    tvDate.setText("Unknown Date");
                }
            }

            // Basic farmer details
            setTextViewSafely(tvFarmerId, (String) subsidyData.get("farmerId"));
            setTextViewSafely(tvFarmerName, (String) subsidyData.get("farmerName"));

            // Location
            String location = (String) subsidyData.get("location");
            if (location == null || location.trim().isEmpty()) {
                location = (String) subsidyData.get("barangay");
                if (location != null && !location.trim().isEmpty()) {
                    location = "Barangay " + location;
                }
            }
            setTextViewSafely(tvLocation, location);

            // Farm type
            String farmType = determineFarmType(subsidyData);
            setTextViewSafely(tvFarmType, farmType);

            // Support type
            String supportType = (String) subsidyData.get("supportType");
            setTextViewSafely(tvSubsidyType, supportType);

            // Show/hide fields based on farm type
            displayFieldsBasedOnFarmType(farmType, subsidyData);

            // Show/hide subsidy details based on support type
            displaySubsidyDetailsBasedOnType(supportType, subsidyData);

            // Update status background
            updateStatusBackground(currentStatus);

            Log.d(TAG, "populateSubsidyData() completed");

        } catch (Exception e) {
            Log.e(TAG, "Error populating subsidy data", e);
        }
    }

    private void updateButtonVisibility(String status) {
        try {
            Log.d(TAG, "updateButtonVisibility() called with status: '" + status + "'");

            if (status == null) {
                status = "Pending";
            }

            String normalizedStatus = status.trim().toLowerCase(Locale.US);
            boolean shouldShowButtons = "pending".equals(normalizedStatus);

            Log.d(TAG, "Normalized status: '" + normalizedStatus + "', Should show buttons: " + shouldShowButtons);

            if (buttonContainer != null) {
                buttonContainer.setVisibility(shouldShowButtons ? View.VISIBLE : View.GONE);
                Log.d(TAG, "Button container visibility set to: " + (shouldShowButtons ? "VISIBLE" : "GONE"));
            } else {
                Log.w(TAG, "Button container is null, using individual buttons");
                // Fallback to individual buttons
                if (btnApprove != null) {
                    btnApprove.setVisibility(shouldShowButtons ? View.VISIBLE : View.GONE);
                    Log.d(TAG, "Approve button visibility set to: " + (shouldShowButtons ? "VISIBLE" : "GONE"));
                } else {
                    Log.e(TAG, "btnApprove is null!");
                }
                if (btnReject != null) {
                    btnReject.setVisibility(shouldShowButtons ? View.VISIBLE : View.GONE);
                    Log.d(TAG, "Reject button visibility set to: " + (shouldShowButtons ? "VISIBLE" : "GONE"));
                } else {
                    Log.e(TAG, "btnReject is null!");
                }
            }

            // Also ensure buttons are enabled when visible
            if (shouldShowButtons) {
                setButtonsEnabled(true);
                Log.d(TAG, "Buttons enabled because status is pending");
            }

            // Force a test after visibility update
            if (shouldShowButtons && btnApprove != null) {
                btnApprove.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Post-visibility update button state:");
                        Log.d(TAG, "- Approve visibility: " + btnApprove.getVisibility());
                        Log.d(TAG, "- Approve enabled: " + btnApprove.isEnabled());
                        Log.d(TAG, "- Approve clickable: " + btnApprove.isClickable());
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating button visibility", e);
            showButtonContainer();
        }
    }

    private void showButtonContainer() {
        try {
            Log.d(TAG, "showButtonContainer() called");

            if (buttonContainer != null) {
                buttonContainer.setVisibility(View.VISIBLE);
                Log.d(TAG, "Button container set to VISIBLE");
            } else {
                Log.w(TAG, "Button container is null - falling back to individual button showing");
                // Fallback: show individual buttons
                if (btnApprove != null) {
                    btnApprove.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Approve button set to VISIBLE");
                }
                if (btnReject != null) {
                    btnReject.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Reject button set to VISIBLE");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing button container", e);
        }
    }

    private void hideButtonContainer() {
    }

    private void updateStatusBackground(String status) {
        try {
            if (status == null || tvStatus == null) return;

            String statusLower = status.trim().toLowerCase();

            switch (statusLower) {
                case "approved":
                    tvStatus.setBackgroundResource(R.drawable.status_approved_background);
                    break;
                case "rejected":
                    tvStatus.setBackgroundResource(R.drawable.status_rejected_background);
                    break;
                case "pending":
                default:
                    tvStatus.setBackgroundResource(R.drawable.status_pending_background);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating status background", e);
        }
    }

    private String determineFarmType(Map<String, Object> subsidyData) {
        String farmType = (String) subsidyData.get("farmType");
        if (farmType != null && !farmType.trim().isEmpty()) {
            return farmType;
        }

        boolean hasLivestock = subsidyData.containsKey("livestock") || subsidyData.containsKey("livestockCount");
        boolean hasCrops = subsidyData.containsKey("seedType") || subsidyData.containsKey("fertilizerType") ||
                subsidyData.containsKey("cropsGrown") || subsidyData.containsKey("lotSize") ||
                "Crop".equalsIgnoreCase((String) subsidyData.get("farmType"));

        if (hasLivestock && hasCrops) {
            return "Mixed (Livestock & Crops)";
        } else if (hasLivestock) {
            return "Livestock";
        } else if (hasCrops) {
            return "Crops";
        } else {
            return "Not specified";
        }
    }

    private void displayFieldsBasedOnFarmType(String farmType, Map<String, Object> subsidyData) {
        try {
            if (farmType == null) return;

            if (farmType.toLowerCase().contains("livestock")) {
                showLivestockFields(subsidyData);
            } else {
                hideLivestockFields();
            }

            if (farmType.toLowerCase().contains("crops") || farmType.toLowerCase().contains("mixed")) {
                showCropsFields(subsidyData);
            } else {
                hideCropsFields();
            }

            if (farmType.toLowerCase().contains("mixed")) {
                showLivestockFields(subsidyData);
                showCropsFields(subsidyData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying fields based on farm type", e);
        }
    }

    private void showLivestockFields(Map<String, Object> subsidyData) {
        try {
            if (tvLivestockTypeLabel != null && tvLivestockType != null) {
                tvLivestockTypeLabel.setVisibility(View.VISIBLE);
                tvLivestockType.setVisibility(View.VISIBLE);
                setTextViewSafely(tvLivestockType, (String) subsidyData.get("livestock"));
            }

            if (tvLivestockCountLabel != null && tvLivestockCount != null) {
                tvLivestockCountLabel.setVisibility(View.VISIBLE);
                tvLivestockCount.setVisibility(View.VISIBLE);
                Object countObj = subsidyData.get("livestockCount");
                if (countObj != null) {
                    tvLivestockCount.setText(String.valueOf(countObj));
                } else {
                    tvLivestockCount.setText("Not specified");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing livestock fields", e);
        }
    }

    private void showCropsFields(Map<String, Object> subsidyData) {
        try {
            if (tvCropsLabel != null && tvCrops != null) {
                tvCropsLabel.setVisibility(View.VISIBLE);
                tvCrops.setVisibility(View.VISIBLE);

                String crops = (String) subsidyData.get("cropsGrown");
                if (crops == null || crops.trim().isEmpty()) {
                    crops = (String) subsidyData.get("seedType");
                }
                setTextViewSafely(tvCrops, crops);
            }

            if (tvLotSizeLabel != null && tvLotSize != null) {
                tvLotSizeLabel.setVisibility(View.VISIBLE);
                tvLotSize.setVisibility(View.VISIBLE);

                String lotSize = (String) subsidyData.get("lotSize");
                Log.d(TAG, "Lot size from database: " + lotSize);

                if (lotSize != null && !lotSize.trim().isEmpty()) {
                    tvLotSize.setText(lotSize);
                } else {
                    tvLotSize.setText("Not specified");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing crops fields", e);
        }
    }

    private void hideLivestockFields() {
        try {
            if (tvLivestockTypeLabel != null) tvLivestockTypeLabel.setVisibility(View.GONE);
            if (tvLivestockType != null) tvLivestockType.setVisibility(View.GONE);
            if (tvLivestockCountLabel != null) tvLivestockCountLabel.setVisibility(View.GONE);
            if (tvLivestockCount != null) tvLivestockCount.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Error hiding livestock fields", e);
        }
    }

    private void hideCropsFields() {
        try {
            if (tvCropsLabel != null) tvCropsLabel.setVisibility(View.GONE);
            if (tvCrops != null) tvCrops.setVisibility(View.GONE);
            if (tvLotSizeLabel != null) tvLotSizeLabel.setVisibility(View.GONE);
            if (tvLotSize != null) tvLotSize.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Error hiding crops fields", e);
        }
    }

    private void displaySubsidyDetailsBasedOnType(String supportType, Map<String, Object> subsidyData) {
        try {
            hideAllSubsidyDetails();

            if ("Seeds and Fertilizers".equalsIgnoreCase(supportType)) {
                showSeedsAndFertilizersFields(subsidyData);
            } else if ("Monetary Support".equalsIgnoreCase(supportType)) {
                showMonetaryFields(subsidyData);
            } else if ("Livestock Support".equalsIgnoreCase(supportType)) {
                showLivestockSupportFields(subsidyData);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying subsidy details", e);
        }
    }

    private void showSeedsAndFertilizersFields(Map<String, Object> subsidyData) {
        try {
            if (tvSeedsRequestedLabel != null && tvSeedsRequested != null) {
                String seeds = (String) subsidyData.get("seedsRequested");
                if (seeds == null || seeds.trim().isEmpty()) {
                    seeds = (String) subsidyData.get("seedType");
                }

                if (seeds != null && !seeds.trim().isEmpty()) {
                    tvSeedsRequestedLabel.setVisibility(View.VISIBLE);
                    tvSeedsRequested.setVisibility(View.VISIBLE);
                    setTextViewSafely(tvSeedsRequested, seeds);
                }
            }

            if (tvFertilizersRequestedLabel != null && tvFertilizersRequested != null) {
                String fertilizers = (String) subsidyData.get("fertilizersRequested");
                if (fertilizers == null || fertilizers.trim().isEmpty()) {
                    fertilizers = (String) subsidyData.get("fertilizerType");
                }

                if (fertilizers != null && !fertilizers.trim().isEmpty()) {
                    tvFertilizersRequestedLabel.setVisibility(View.VISIBLE);
                    tvFertilizersRequested.setVisibility(View.VISIBLE);
                    setTextViewSafely(tvFertilizersRequested, fertilizers);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing seeds and fertilizers fields", e);
        }
    }

    private void showMonetaryFields(Map<String, Object> subsidyData) {
        try {
            if (tvMonetarySupportLabel != null && tvMonetarySupport != null) {
                tvMonetarySupportLabel.setVisibility(View.VISIBLE);
                tvMonetarySupport.setVisibility(View.VISIBLE);
                Object amountObj = subsidyData.get("monetaryAmount");
                if (amountObj != null) {
                    tvMonetarySupport.setText("₱" + String.valueOf(amountObj));
                } else {
                    tvMonetarySupport.setText("Amount not specified");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing monetary fields", e);
        }
    }

    private void showLivestockSupportFields(Map<String, Object> subsidyData) {
        try {
            if (tvMonetarySupportLabel != null && tvMonetarySupport != null) {
                tvMonetarySupportLabel.setText("Livestock Support Type");
                tvMonetarySupportLabel.setVisibility(View.VISIBLE);
                tvMonetarySupport.setVisibility(View.VISIBLE);
                setTextViewSafely(tvMonetarySupport, (String) subsidyData.get("livestockSupportType"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing livestock support fields", e);
        }
    }

    private void hideAllSubsidyDetails() {
        try {
            if (tvSeedsRequestedLabel != null) tvSeedsRequestedLabel.setVisibility(View.GONE);
            if (tvSeedsRequested != null) tvSeedsRequested.setVisibility(View.GONE);
            if (tvFertilizersRequestedLabel != null) tvFertilizersRequestedLabel.setVisibility(View.GONE);
            if (tvFertilizersRequested != null) tvFertilizersRequested.setVisibility(View.GONE);
            if (tvMonetarySupportLabel != null) tvMonetarySupportLabel.setVisibility(View.GONE);
            if (tvMonetarySupport != null) tvMonetarySupport.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Error hiding subsidy details", e);
        }
    }

    private void setTextViewSafely(TextView textView, String value) {
        try {
            if (textView != null) {
                if (value != null && !value.trim().isEmpty()) {
                    textView.setText(value);
                } else {
                    textView.setText("Not specified");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting text view", e);
        }
    }

    private void loadAttachedFiles(Map<String, Object> subsidyData) {
        try {
            // Check for supporting document URL (most common field)
            String documentUrl = (String) subsidyData.get("supportingDocumentUrl");
            String documentName = (String) subsidyData.get("supportingDocumentName");

            if (documentUrl != null && !documentUrl.trim().isEmpty()) {
                if (noFilesView != null) noFilesView.setVisibility(View.GONE);
                setupFileItem(documentName != null ? documentName : "Supporting Document", documentUrl);
                return;
            }

            // Fallback: check for files array/object
            if (subsidyData.containsKey("files")) {
                Object filesObj = subsidyData.get("files");

                if (filesObj instanceof List) {
                    List<Map<String, String>> files = (List<Map<String, String>>) filesObj;
                    if (!files.isEmpty()) {
                        if (noFilesView != null) noFilesView.setVisibility(View.GONE);
                        Map<String, String> firstFile = files.get(0);
                        setupFileItem(firstFile.get("name"), firstFile.get("url"));
                    } else {
                        showNoFiles();
                    }
                } else if (filesObj instanceof Map) {
                    Map<String, String> file = (Map<String, String>) filesObj;
                    if (noFilesView != null) noFilesView.setVisibility(View.GONE);
                    setupFileItem(file.get("name"), file.get("url"));
                } else {
                    showNoFiles();
                }
            } else {
                showNoFiles();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading attached files", e);
            showNoFiles();
        }
    }

    private void showNoFiles() {
        try {
            if (noFilesView != null) noFilesView.setVisibility(View.VISIBLE);
            if (fileItem != null) fileItem.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Error showing no files", e);
        }
    }

    private void setupFileItem(String fileName, String fileUrl) {
        try {
            if (fileName != null && fileUrl != null && fileItem != null && tvFileName != null) {
                fileItem.setVisibility(View.VISIBLE);
                tvFileName.setText(fileName);
                if (btnDownloadFile != null) {
                    btnDownloadFile.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            downloadFile(fileUrl);
                        }
                    });
                }
            } else {
                showNoFiles();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up file item", e);
            showNoFiles();
        }
    }

    private void downloadFile(String fileUrl) {
        try {
            // Try direct URL first (for Firebase Storage URLs)
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(fileUrl));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file directly, trying Firebase Storage", e);

            try {
                // Fallback: try Firebase Storage reference
                StorageReference fileRef = storage.getReferenceFromUrl(fileUrl);

                fileRef.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                try {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(uri);
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error opening file", e);
                                    Toast.makeText(MunicipalSubsidyDetails.this, "Cannot open file", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to download file", e);
                                Toast.makeText(MunicipalSubsidyDetails.this, "Failed to download file", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception ex) {
                Log.e(TAG, "Error with Firebase Storage download", ex);
                Toast.makeText(this, "Error downloading file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");
        if (currentStatus != null) {
            updateButtonVisibility(currentStatus);
        }

        // Test buttons on resume
        if (btnApprove != null && btnReject != null) {
            Log.d(TAG, "Testing buttons on resume:");
            Log.d(TAG, "Approve - Visible: " + (btnApprove.getVisibility() == View.VISIBLE) +
                    ", Enabled: " + btnApprove.isEnabled() +
                    ", Clickable: " + btnApprove.isClickable());
            Log.d(TAG, "Reject - Visible: " + (btnReject.getVisibility() == View.VISIBLE) +
                    ", Enabled: " + btnReject.isEnabled() +
                    ", Clickable: " + btnReject.isClickable());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
        Log.d(TAG, "Activity destroyed");
    }
}
