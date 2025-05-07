package com.maramagagriculturalaid.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FarmInfoActivity extends AppCompatActivity {

    // Constants for farm types
    public static final int FARM_TYPE_CROP = 0;
    public static final int FARM_TYPE_LIVESTOCK = 1;
    public static final int FARM_TYPE_MIXED = 2;

    // UI Components
    private ViewFlipper viewFlipper;
    private RadioGroup rgFarmType;
    private RadioButton rbCrop, rbLivestock, rbMixed;
    private ImageButton btnBack;
    private Button btnSave;
    private TextView tvTitle;

    // Crop Farm Form Fields
    private EditText etMunicipalCrop, etStreetCrop, etCropsGrown, etLotSize;
    private Spinner spinnerBarangayCrop;

    // Livestock Farm Form Fields
    private EditText etMunicipalLivestock, etStreetLivestock, etLivestock, etNoLivestock;
    private Spinner spinnerBarangayLivestock;

    // Mixed Farm Form Fields
    private EditText etMunicipalMixed, etStreetMixed, etCropsGrownMixed, etLotSizeMixed,
            etLivestockMixed, etNoLivestockMixed;
    private Spinner spinnerBarangayMixed;

    // Data
    private FirebaseFirestore db;
    private Map<String, Object> farmerData;
    private Map<String, Object> cropFarmData;
    private Map<String, Object> livestockFarmData;
    private Map<String, Object> mixedFarmData;
    private int currentFarmType = FARM_TYPE_CROP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_info);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize data maps
        farmerData = new HashMap<>();
        cropFarmData = new HashMap<>();
        livestockFarmData = new HashMap<>();
        mixedFarmData = new HashMap<>();

        // Get farmer data from intent
        extractFarmerData();

        // Initialize views
        initViews();

        // Set up farm type selection
        setupFarmTypeSelection();

        // Set up click listeners
        setupClickListeners();

        // Set up spinners
        setupSpinners();
    }

    private void extractFarmerData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            farmerData.put("farmerId", extras.getString("farmerId", ""));
            farmerData.put("phoneNumber", extras.getString("phoneNumber", ""));
            farmerData.put("firstName", extras.getString("firstName", ""));
            farmerData.put("lastName", extras.getString("lastName", ""));
            farmerData.put("middleInitial", extras.getString("middleInitial", ""));
            farmerData.put("birthday", extras.getString("birthday", ""));

            // Add full name for easier querying
            String fullName = extras.getString("firstName", "") + " ";
            if (!TextUtils.isEmpty(extras.getString("middleInitial", ""))) {
                fullName += extras.getString("middleInitial", "") + ". ";
            }
            fullName += extras.getString("lastName", "");
            farmerData.put("fullName", fullName);
        }
    }

    private void initViews() {
        // Main UI components
        viewFlipper = findViewById(R.id.view_flipper);
        rgFarmType = findViewById(R.id.rg_farm_type);
        rbCrop = findViewById(R.id.rb_crop);
        rbLivestock = findViewById(R.id.rb_livestock);
        rbMixed = findViewById(R.id.rb_mixed);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        tvTitle = findViewById(R.id.tv_title);

        // Initialize Crop Farm form fields
        etMunicipalCrop = findViewById(R.id.et_municipal_crop);
        etStreetCrop = findViewById(R.id.et_street_crop);
        etCropsGrown = findViewById(R.id.et_crops_grown);
        etLotSize = findViewById(R.id.et_lot_size);
        spinnerBarangayCrop = findViewById(R.id.spinner_barangay_crop);

        // Initialize Livestock Farm form fields
        etMunicipalLivestock = findViewById(R.id.et_municipal_livestock);
        etStreetLivestock = findViewById(R.id.et_street_livestock);
        etLivestock = findViewById(R.id.et_livestock);
        etNoLivestock = findViewById(R.id.et_no_livestock);
        spinnerBarangayLivestock = findViewById(R.id.spinner_barangay_livestock);

        // Initialize Mixed Farm form fields
        etMunicipalMixed = findViewById(R.id.et_municipal_mixed);
        etStreetMixed = findViewById(R.id.et_street_mixed);
        etCropsGrownMixed = findViewById(R.id.et_crops_grown_mixed);
        etLotSizeMixed = findViewById(R.id.et_lot_size_mixed);
        etLivestockMixed = findViewById(R.id.et_livestock_mixed);
        etNoLivestockMixed = findViewById(R.id.et_no_livestock_mixed);
        spinnerBarangayMixed = findViewById(R.id.spinner_barangay_mixed);
    }

    private void setupFarmTypeSelection() {
        // Set default selection
        rbCrop.setChecked(true);
        viewFlipper.setDisplayedChild(FARM_TYPE_CROP);

        // Set listener for farm type selection
        rgFarmType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_crop) {
                currentFarmType = FARM_TYPE_CROP;
                viewFlipper.setDisplayedChild(FARM_TYPE_CROP);
            } else if (checkedId == R.id.rb_livestock) {
                currentFarmType = FARM_TYPE_LIVESTOCK;
                viewFlipper.setDisplayedChild(FARM_TYPE_LIVESTOCK);
            } else if (checkedId == R.id.rb_mixed) {
                currentFarmType = FARM_TYPE_MIXED;
                viewFlipper.setDisplayedChild(FARM_TYPE_MIXED);
            }
        });
    }

    private void setupSpinners() {
        // Sample data - replace with your actual data source
        String[] barangays = {"Anahawon", "Base Camp", "Bayabason", "Bagongsilang", "Camp 1", "Colambugon", "Dagumba-an","Danggawan", "Dologon", "Kisanday", "Kuya", "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion", "San Miguel", "San Roque", "Tubigon", "Kiharong"};

        // Set up Crop Farm spinner
        ArrayAdapter<String> adapterCrop = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                barangays
        );
        adapterCrop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangayCrop.setAdapter(adapterCrop);

        // Set up Livestock Farm spinner
        ArrayAdapter<String> adapterLivestock = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                barangays
        );
        adapterLivestock.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangayLivestock.setAdapter(adapterLivestock);

        // Set up Mixed Farm spinner
        ArrayAdapter<String> adapterMixed = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                barangays
        );
        adapterMixed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangayMixed.setAdapter(adapterMixed);
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Save button
        btnSave.setOnClickListener(v -> {
            if (validateCurrentForm()) {
                collectCurrentFormData();
                saveFarmerToFirestore();
            }
        });
    }

    private boolean validateCurrentForm() {
        switch (currentFarmType) {
            case FARM_TYPE_CROP:
                return validateCropForm();
            case FARM_TYPE_LIVESTOCK:
                return validateLivestockForm();
            case FARM_TYPE_MIXED:
                return validateMixedForm();
            default:
                return false;
        }
    }

    private boolean validateCropForm() {
        boolean isValid = true;

        // Validate Municipal
        if (TextUtils.isEmpty(etMunicipalCrop.getText())) {
            etMunicipalCrop.setError("Municipal is required");
            isValid = false;
        }

        // Validate Street
        if (TextUtils.isEmpty(etStreetCrop.getText())) {
            etStreetCrop.setError("Street address is required");
            isValid = false;
        }

        // Validate Crops Grown
        if (TextUtils.isEmpty(etCropsGrown.getText())) {
            etCropsGrown.setError("Crops grown is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateLivestockForm() {
        boolean isValid = true;

        // Validate Municipal
        if (TextUtils.isEmpty(etMunicipalLivestock.getText())) {
            etMunicipalLivestock.setError("Municipal is required");
            isValid = false;
        }

        // Validate Street
        if (TextUtils.isEmpty(etStreetLivestock.getText())) {
            etStreetLivestock.setError("Street address is required");
            isValid = false;
        }

        // Validate Livestock
        if (TextUtils.isEmpty(etLivestock.getText())) {
            etLivestock.setError("Livestock type is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateMixedForm() {
        boolean isValid = true;

        // Validate Municipal
        if (TextUtils.isEmpty(etMunicipalMixed.getText())) {
            etMunicipalMixed.setError("Municipal is required");
            isValid = false;
        }

        // Validate Street
        if (TextUtils.isEmpty(etStreetMixed.getText())) {
            etStreetMixed.setError("Street address is required");
            isValid = false;
        }

        // Validate Crops Grown
        if (TextUtils.isEmpty(etCropsGrownMixed.getText())) {
            etCropsGrownMixed.setError("Crops grown is required");
            isValid = false;
        }

        // Validate Livestock
        if (TextUtils.isEmpty(etLivestockMixed.getText())) {
            etLivestockMixed.setError("Livestock type is required");
            isValid = false;
        }

        return isValid;
    }

    private void collectCurrentFormData() {
        switch (currentFarmType) {
            case FARM_TYPE_CROP:
                collectCropFormData();
                break;
            case FARM_TYPE_LIVESTOCK:
                collectLivestockFormData();
                break;
            case FARM_TYPE_MIXED:
                collectMixedFormData();
                break;
        }
    }

    private void collectCropFormData() {
        cropFarmData.put("farmType", "Crop");
        cropFarmData.put("municipal", etMunicipalCrop.getText().toString().trim());
        cropFarmData.put("barangay", spinnerBarangayCrop.getSelectedItem().toString());
        cropFarmData.put("street", etStreetCrop.getText().toString().trim());
        cropFarmData.put("cropsGrown", etCropsGrown.getText().toString().trim());
        cropFarmData.put("lotSize", etLotSize.getText().toString().trim());
    }

    private void collectLivestockFormData() {
        livestockFarmData.put("farmType", "Livestock");
        livestockFarmData.put("municipal", etMunicipalLivestock.getText().toString().trim());
        livestockFarmData.put("barangay", spinnerBarangayLivestock.getSelectedItem().toString());
        livestockFarmData.put("street", etStreetLivestock.getText().toString().trim());
        livestockFarmData.put("livestock", etLivestock.getText().toString().trim());
        livestockFarmData.put("numberOfLivestock", etNoLivestock.getText().toString().trim());
    }

    private void collectMixedFormData() {
        mixedFarmData.put("farmType", "Mixed");
        mixedFarmData.put("municipal", etMunicipalMixed.getText().toString().trim());
        mixedFarmData.put("barangay", spinnerBarangayMixed.getSelectedItem().toString());
        mixedFarmData.put("street", etStreetMixed.getText().toString().trim());
        mixedFarmData.put("cropsGrown", etCropsGrownMixed.getText().toString().trim());
        mixedFarmData.put("lotSize", etLotSizeMixed.getText().toString().trim());
        mixedFarmData.put("livestock", etLivestockMixed.getText().toString().trim());
        mixedFarmData.put("numberOfLivestock", etNoLivestockMixed.getText().toString().trim());
    }

    private void saveFarmerToFirestore() {
        String farmerId = (String) farmerData.get("farmerId");
        if (farmerId == null || farmerId.isEmpty()) {
            Toast.makeText(this, "Farmer ID is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        showLoading(true);

        // Add timestamp for when the record was created
        farmerData.put("createdAt", com.google.firebase.Timestamp.now());
        farmerData.put("lastUpdated", com.google.firebase.Timestamp.now());

        // First check if the farmer ID already exists
        db.collection("farmers")
                .document(farmerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            showLoading(false);
                            Toast.makeText(this,
                                    "A farmer with this ID already exists", Toast.LENGTH_LONG).show();
                        } else {
                            // ID doesn't exist, proceed with save
                            performSave(farmerId);
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this,
                                "Error checking farmer ID: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performSave(String farmerId) {
        // Save farmer data
        db.collection("farmers")
                .document(farmerId)
                .set(farmerData)
                .addOnSuccessListener(aVoid -> {
                    // Save farm data based on selected type
                    switch (currentFarmType) {
                        case FARM_TYPE_CROP:
                            saveCropFarmData(farmerId);
                            break;
                        case FARM_TYPE_LIVESTOCK:
                            saveLivestockFarmData(farmerId);
                            break;
                        case FARM_TYPE_MIXED:
                            saveMixedFarmData(farmerId);
                            break;
                        default:
                            // If no farm type selected, just finish
                            showLoading(false);
                            Toast.makeText(this,
                                    "Farmer information saved successfully", Toast.LENGTH_SHORT).show();
                            finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Error saving farmer data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCropFarmData(String farmerId) {
        // Add timestamp
        cropFarmData.put("lastUpdated", com.google.firebase.Timestamp.now());

        db.collection("Farmers")
                .document(farmerId)
                .collection("FarmType")
                .document("Crop")
                .set(cropFarmData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Farmer and crop farm information saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Error saving crop farm data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }


    private void saveLivestockFarmData(String farmerId) {
        // Add timestamp
        livestockFarmData.put("lastUpdated", com.google.firebase.Timestamp.now());

        db.collection("Farmers").document(farmerId)
                .collection("FarmType").document("Livestock")
                .set(livestockFarmData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Farmer and farm information saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Error saving livestock farm data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveMixedFarmData(String farmerId) {
        // Add timestamp
        mixedFarmData.put("lastUpdated", com.google.firebase.Timestamp.now());

        db.collection("Farmers").document(farmerId)
                .collection("FarmType").document("Mixed")
                .set(mixedFarmData)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Farmer and farm information saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "Error saving mixed farm data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        // Implement loading indicator logic here
        // For example, show/hide a ProgressBar
        View progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        // Disable UI interaction during loading
        btnSave.setEnabled(!isLoading);
        btnBack.setEnabled(!isLoading);
        rgFarmType.setEnabled(!isLoading);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}