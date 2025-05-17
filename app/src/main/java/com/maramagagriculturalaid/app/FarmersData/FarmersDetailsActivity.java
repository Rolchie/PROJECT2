package com.maramagagriculturalaid.app.FarmersData;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class FarmersDetailsActivity extends AppCompatActivity {

    private static final String TAG = "FarmersDetailsActivity";

    // UI Components
    private ImageButton btnBack;
    private TextView tvTitle;
    private TextView tvFarmerId;
    private TextView tvPhoneNumber;
    private TextView tvFullName;
    private TextView tvBirthday;
    private TextView tvAddress;
    private TextView tvFarmType;
    private LinearLayout cropDetails;
    private LinearLayout livestockDetails;
    private LinearLayout mixedDetails;
    private TextView tvCropsGrown;
    private TextView tvLotSize;
    private TextView tvLivestock;
    private TextView tvLivestockCount;
    private TextView tvMixedCrops;
    private TextView tvMixedLotSize;
    private TextView tvMixedLivestock;
    private TextView tvMixedLivestockCount;
    private TextView tvLastUpdated;
    private AppCompatButton btnEdit;
    private AppCompatButton btnDelete;
    private ProgressBar progressBar;

    // Data
    private String farmerId;
    private String barangayId;
    private FirebaseFirestore db;
    private Farmer currentFarmer;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmers_details);

        // Initialize date formatter
        dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

        // Get farmer ID and barangay ID from intent
        farmerId = getIntent().getStringExtra("farmerId");
        barangayId = getIntent().getStringExtra("barangayId");

        if (farmerId == null || barangayId == null) {
            Toast.makeText(this, "Error: Farmer ID or Barangay ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeViews();

        // Setup listeners
        setupListeners();

        // Load farmer data
        loadFarmerData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvFarmerId = findViewById(R.id.tv_farmer_id);
        tvPhoneNumber = findViewById(R.id.tv_phone_number);
        tvFullName = findViewById(R.id.tv_full_name);
        tvBirthday = findViewById(R.id.tv_birthday);
        tvAddress = findViewById(R.id.tv_address);
        tvFarmType = findViewById(R.id.tv_farm_type);

        // Farm details sections
        cropDetails = findViewById(R.id.crop_details);
        livestockDetails = findViewById(R.id.livestock_details);
        mixedDetails = findViewById(R.id.mixed_details);

        // Crop farm details
        tvCropsGrown = findViewById(R.id.tv_crops_grown);
        tvLotSize = findViewById(R.id.tv_lot_size);

        // Livestock farm details
        tvLivestock = findViewById(R.id.tv_livestock);
        tvLivestockCount = findViewById(R.id.tv_livestock_count);

        // Mixed farm details
        tvMixedCrops = findViewById(R.id.tv_mixed_crops);
        tvMixedLotSize = findViewById(R.id.tv_mixed_lot_size);
        tvMixedLivestock = findViewById(R.id.tv_mixed_livestock);
        tvMixedLivestockCount = findViewById(R.id.tv_mixed_livestock_count);

        tvLastUpdated = findViewById(R.id.tv_last_updated);
        btnEdit = findViewById(R.id.btn_edit);
        btnDelete = findViewById(R.id.btn_delete);
        progressBar = findViewById(R.id.progress_bar);

        // Hide last updated since it's not in the model
        tvLastUpdated.setVisibility(View.GONE);
    }

    private void setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Edit button click listener
        btnEdit.setOnClickListener(v -> {
            if (currentFarmer != null) {
                Intent intent = new Intent(FarmersDetailsActivity.this, EditFarmerActivity.class);
                intent.putExtra("farmerId", farmerId);
                intent.putExtra("barangayId", barangayId);
                startActivity(intent);
            }
        });

        // Delete button click listener
        btnDelete.setOnClickListener(v -> {
            if (currentFarmer != null) {
                showDeleteConfirmation();
            }
        });
    }

    private void loadFarmerData() {
        showLoading(true);

        DocumentReference docRef = db.collection("Barangays").document(barangayId)
                .collection("Farmers").document(farmerId);

        docRef.get().addOnCompleteListener(task -> {
            showLoading(false);

            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                try {
                    currentFarmer = task.getResult().toObject(Farmer.class);
                    if (currentFarmer != null) {
                        if (currentFarmer.getId() == null) {
                            currentFarmer.setId(farmerId);
                        }
                        displayFarmerData();
                    } else {
                        Toast.makeText(this, "Farmer data is null", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing farmer data", e);
                    Toast.makeText(this, "Error loading farmer details", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.e(TAG, "Error getting farmer details: ", task.getException());
                Toast.makeText(this, "Failed to load farmer details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private void displayFarmerData() {
        try {
            // Set basic farmer information
            tvFarmerId.setText(currentFarmer.getId());
            tvPhoneNumber.setText(currentFarmer.getPhoneNumber());
            tvFullName.setText(currentFarmer.getFullName());

            // Format birthday if available
            if (currentFarmer.getBirthday() != null) {
                tvBirthday.setText(dateFormat.format(currentFarmer.getBirthday()));
            } else {
                tvBirthday.setText("Not provided");
            }

            // Set address - combine location and exact location if available
            StringBuilder addressBuilder = new StringBuilder();
            if (currentFarmer.getExactLocation() != null && !currentFarmer.getExactLocation().isEmpty()) {
                addressBuilder.append(currentFarmer.getExactLocation());
            }
            if (currentFarmer.getLocation() != null && !currentFarmer.getLocation().isEmpty()) {
                if (addressBuilder.length() > 0) {
                    addressBuilder.append(", ");
                }
                addressBuilder.append(currentFarmer.getLocation());
            }
            if (currentFarmer.getBarangay() != null && !currentFarmer.getBarangay().isEmpty()) {
                if (addressBuilder.length() > 0) {
                    addressBuilder.append(", ");
                }
                addressBuilder.append("Barangay ").append(currentFarmer.getBarangay());
            }

            if (addressBuilder.length() > 0) {
                tvAddress.setText(addressBuilder.toString());
            } else if (currentFarmer.getAddress() != null && !currentFarmer.getAddress().isEmpty()) {
                // Fallback to address field if location fields are empty
                tvAddress.setText(currentFarmer.getAddress());
            } else {
                tvAddress.setText("Not provided");
            }

            // Set farm type
            tvFarmType.setText(currentFarmer.getFarmType());

            // Show appropriate farm details section based on farm type
            String farmType = currentFarmer.getFarmType();
            if (farmType != null) {
                if (farmType.equalsIgnoreCase("Crop")) {
                    cropDetails.setVisibility(View.VISIBLE);
                    livestockDetails.setVisibility(View.GONE);
                    mixedDetails.setVisibility(View.GONE);

                    tvCropsGrown.setText(currentFarmer.getCropsGrown());
                    tvLotSize.setText(String.format(Locale.getDefault(), "%.2f hectares", currentFarmer.getLotSize()));
                } else if (farmType.equalsIgnoreCase("Livestock")) {
                    cropDetails.setVisibility(View.GONE);
                    livestockDetails.setVisibility(View.VISIBLE);
                    mixedDetails.setVisibility(View.GONE);

                    tvLivestock.setText(currentFarmer.getLivestock());
                    tvLivestockCount.setText(String.valueOf(currentFarmer.getLivestockCount()));
                } else if (farmType.equalsIgnoreCase("Mixed")) {
                    cropDetails.setVisibility(View.GONE);
                    livestockDetails.setVisibility(View.GONE);
                    mixedDetails.setVisibility(View.VISIBLE);

                    tvMixedCrops.setText(currentFarmer.getCropsGrown());
                    tvMixedLotSize.setText(String.format(Locale.getDefault(), "%.2f hectares", currentFarmer.getLotSize()));
                    tvMixedLivestock.setText(currentFarmer.getLivestock());
                    tvMixedLivestockCount.setText(String.valueOf(currentFarmer.getLivestockCount()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying farmer data", e);
            Toast.makeText(this, "Error displaying farmer data", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Farmer")
                .setMessage("Are you sure you want to delete " + currentFarmer.getFullName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteFarmer())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteFarmer() {
        showLoading(true);

        db.collection("Barangays").document(barangayId)
                .collection("Farmers").document(farmerId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(FarmersDetailsActivity.this,
                            "Farmer deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error deleting farmer", e);
                    Toast.makeText(FarmersDetailsActivity.this,
                            "Failed to delete farmer", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning from edit screen
        if (farmerId != null && barangayId != null) {
            loadFarmerData();
        }
    }
}