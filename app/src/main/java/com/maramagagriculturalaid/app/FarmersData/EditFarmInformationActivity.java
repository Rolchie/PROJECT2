package com.maramagagriculturalaid.app.FarmersData;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SuccessActivities.SuccessedEditedFarmer;

public class EditFarmInformationActivity extends AppCompatActivity {
    // Firebase
    private FirebaseFirestore db;
    private String farmerDocumentId;
    private boolean isNewFarmer;
    private Farmer farmer;

    // UI Components - Common
    private ImageButton btnBack;
    private RadioGroup rgFarmType;
    private RadioButton rbCrop, rbLivestock, rbMixed;
    private ViewFlipper viewFlipper;
    private ProgressDialog progressDialog;
    private Button btnSave;
    private ProgressBar progressBar;

    // UI Components - Farm Location (all views)
    private TextView etMunicipalCrop, etMunicipalLivestock, etMunicipalMixed;
    private TextView tvBarangayCrop, tvBarangayLivestock, tvBarangayMixed;
    private TextView etStreetCrop, etStreetLivestock, etStreetMixed;

    // UI Components - Crop Farm
    private TextView tvCropsGrown, tvLotSize;

    // UI Components - Livestock Farm
    private TextView tvLivestockType, tvAnimalCount;

    // UI Components - Mixed Farm
    private TextView tvCropsGrownMixed, tvLotSizeMixed;
    private TextView tvLivestockTypeMixed, tvAnimalCountMixed;

    private String selectedFarmType = "Crop"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_farm_information);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get farmer document ID from intent
        farmerDocumentId = getIntent().getStringExtra("FARMER_DOCUMENT_ID");
        isNewFarmer = getIntent().getBooleanExtra("IS_NEW_FARMER", false);

        if (farmerDocumentId == null) {
            Toast.makeText(this, "Error: Farmer ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initViews();

        // Setup RadioGroup listener for farm type selection
        setupFarmTypeListener();

        // Setup Save button listener
        setupSaveButtonListener();

        // Load farmer data
        loadFarmerData();
    }

    private void initViews() {
        // Common elements
        btnBack = findViewById(R.id.btnBack);
        rgFarmType = findViewById(R.id.rg_farm_type);
        rbCrop = findViewById(R.id.rb_crop);
        rbLivestock = findViewById(R.id.rb_livestock);
        rbMixed = findViewById(R.id.rb_mixed);
        viewFlipper = findViewById(R.id.view_flipper);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);

        // Crop view elements
        etMunicipalCrop = findViewById(R.id.et_municipal_crop);
        tvBarangayCrop = findViewById(R.id.tv_barangay_crop);
        etStreetCrop = findViewById(R.id.et_street_crop);
        tvCropsGrown = findViewById(R.id.tv_crops_grown);
        tvLotSize = findViewById(R.id.tv_lot_size);

        // Livestock view elements
        etMunicipalLivestock = findViewById(R.id.et_municipal_livestock);
        tvBarangayLivestock = findViewById(R.id.tv_barangay_livestock);
        etStreetLivestock = findViewById(R.id.et_street_livestock);
        tvLivestockType = findViewById(R.id.tv_livestock_type);
        tvAnimalCount = findViewById(R.id.tv_animal_count);

        // Mixed view elements
        etMunicipalMixed = findViewById(R.id.et_municipal_mixed);
        tvBarangayMixed = findViewById(R.id.tv_barangay_mixed);
        etStreetMixed = findViewById(R.id.et_street_mixed);
        tvCropsGrownMixed = findViewById(R.id.tv_crops_grown_mixed);
        tvLotSizeMixed = findViewById(R.id.tv_lot_size_mixed);
        tvLivestockTypeMixed = findViewById(R.id.tv_livestock_type_mixed);
        tvAnimalCountMixed = findViewById(R.id.tv_animal_count_mixed);

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Progress dialog setup
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
    }

    private void setupFarmTypeListener() {
        rgFarmType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_crop) {
                selectedFarmType = "Crop";
                viewFlipper.setDisplayedChild(0); // Show crop view
            } else if (checkedId == R.id.rb_livestock) {
                selectedFarmType = "Livestock";
                viewFlipper.setDisplayedChild(1); // Show livestock view
            } else if (checkedId == R.id.rb_mixed) {
                selectedFarmType = "Mixed";
                viewFlipper.setDisplayedChild(2); // Show mixed view
            }
        });
    }

    private void setupSaveButtonListener() {
        btnSave.setOnClickListener(v -> {
            saveFarmerData();
        });
    }

    private void saveFarmerData() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        // Get updated data based on the selected farm type
        String location = "";
        String barangay = "";
        String exactLocation = "";

        // Get the appropriate data based on selected farm type
        switch (selectedFarmType) {
            case "Crop":
                location = etMunicipalCrop.getText().toString().trim();
                barangay = tvBarangayCrop.getText().toString().trim();
                exactLocation = etStreetCrop.getText().toString().trim();
                break;
            case "Livestock":
                location = etMunicipalLivestock.getText().toString().trim();
                barangay = tvBarangayLivestock.getText().toString().trim();
                exactLocation = etStreetLivestock.getText().toString().trim();
                break;
            case "Mixed":
                location = etMunicipalMixed.getText().toString().trim();
                barangay = tvBarangayMixed.getText().toString().trim();
                exactLocation = etStreetMixed.getText().toString().trim();
                break;
        }

        // Since this is read-only activity, just navigate to success screen
        navigateToSuccessScreen();
    }

    private void navigateToSuccessScreen() {
        progressBar.setVisibility(View.GONE);

        // Navigate to SuccessedEditedFarmer activity
        Intent intent = new Intent(EditFarmInformationActivity.this, SuccessedEditedFarmer.class);
        intent.putExtra("FARMER_ID", farmerDocumentId);
        startActivity(intent);
        finish(); // Close this activity
    }

    private void loadFarmerData() {
        progressDialog.show();

        DocumentReference docRef = db.collection("Farmers").document(farmerDocumentId);
        docRef.get().addOnCompleteListener(task -> {
            progressDialog.dismiss();

            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    farmer = task.getResult().toObject(Farmer.class);
                    displayFarmData();
                } else {
                    Toast.makeText(this, "Error: Farmer not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Error loading farmer data: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayFarmData() {
        if (farmer != null) {
            // Set farm type and display appropriate view
            if (farmer.getFarmType() != null) {
                selectedFarmType = farmer.getFarmType();

                switch (selectedFarmType) {
                    case "Crop":
                        rbCrop.setChecked(true);
                        viewFlipper.setDisplayedChild(0);
                        break;
                    case "Livestock":
                        rbLivestock.setChecked(true);
                        viewFlipper.setDisplayedChild(1);
                        break;
                    case "Mixed":
                        rbMixed.setChecked(true);
                        viewFlipper.setDisplayedChild(2);
                        break;
                }
            }

            // Set location data for all views
            String location = farmer.getLocation() != null ? farmer.getLocation() : "Maramag";
            etMunicipalCrop.setText(location);
            etMunicipalLivestock.setText(location);
            etMunicipalMixed.setText(location);

            String barangay = farmer.getBarangay() != null ? farmer.getBarangay() : "";
            tvBarangayCrop.setText(barangay);
            tvBarangayLivestock.setText(barangay);
            tvBarangayMixed.setText(barangay);

            String exactLocation = farmer.getExactLocation() != null ? farmer.getExactLocation() : "";
            etStreetCrop.setText(exactLocation);
            etStreetLivestock.setText(exactLocation);
            etStreetMixed.setText(exactLocation);

            // Set crop data
            String cropsGrown = farmer.getCropsGrown() != null ? farmer.getCropsGrown() : "";
            tvCropsGrown.setText(cropsGrown);
            tvCropsGrownMixed.setText(cropsGrown);

            String lotSize = farmer.getLotSize() > 0 ? String.valueOf(farmer.getLotSize()) : "";
            tvLotSize.setText(lotSize);
            tvLotSizeMixed.setText(lotSize);

            // Set livestock data
            String livestock = farmer.getLivestock() != null ? farmer.getLivestock() : "";
            tvLivestockType.setText(livestock);
            tvLivestockTypeMixed.setText(livestock);

            String livestockCount = farmer.getLivestockCount() > 0 ? String.valueOf(farmer.getLivestockCount()) : "";
            tvAnimalCount.setText(livestockCount);
            tvAnimalCountMixed.setText(livestockCount);
        }
    }
}