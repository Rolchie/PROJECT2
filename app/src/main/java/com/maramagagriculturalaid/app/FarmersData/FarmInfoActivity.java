package com.maramagagriculturalaid.app.FarmersData;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.util.Log;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SuccessActivities.SuccessedAddFarmer;

import java.util.HashMap;
import java.util.Map;

public class FarmInfoActivity extends AppCompatActivity {

    public static final int FARM_TYPE_CROP = 0;
    public static final int FARM_TYPE_LIVESTOCK = 1;
    public static final int FARM_TYPE_MIXED = 2;

    private ViewFlipper viewFlipper;
    private RadioGroup rgFarmType;
    private RadioButton rbCrop, rbLivestock, rbMixed;
    private AppCompatImageButton btnBack;
    private AppCompatButton btnSave;
    private AppCompatTextView tvTitle;
    private View progressOverlay;
    private ProgressBar stepProgress;

    // Crop form fields
    private AppCompatTextView tvMunicipalCrop;
    private EditText etStreetCrop, etLotSize, etOtherCrop;
    private Spinner spinnerBarangayCrop, spinnerUnit, spinnerCropsGrown;
    private LinearLayout layoutOtherCrop;

    // Livestock form fields
    private AppCompatTextView tvMunicipalLivestock;
    private EditText etStreetLivestock, etOtherLivestock, etLivestockCount;
    private Spinner spinnerBarangayLivestock, spinnerLivestockType;
    private LinearLayout layoutOtherLivestock;

    // Mixed form fields
    private AppCompatTextView tvMunicipalMixed;
    private EditText etStreetMixed, etOtherCropMixed, etLotSizeMixed;
    private EditText etOtherLivestockMixed, etLivestockCountMixed;
    private Spinner spinnerBarangayMixed, spinnerUnitMixed, spinnerCropsGrownMixed;
    private Spinner spinnerLivestockTypeMixed;
    private LinearLayout layoutOtherCropMixed, layoutOtherLivestockMixed;

    private FirebaseFirestore db;
    private Map<String, Object> farmerData;
    private Map<String, Object> cropFarmData;
    private Map<String, Object> livestockFarmData;
    private Map<String, Object> mixedFarmData;
    private int currentFarmType = FARM_TYPE_CROP;

    private String selectedCrop;
    private String selectedLivestock;
    private String selectedCropMixed;
    private String selectedLivestockMixed;

    private static final String TAG = "FarmInfoActivity";

    // Unit measurement options
    private final String[] unitOptions = {"Square Meter(sqm)", "Hectares(ha)", "Are", "Square Feet(Sq ft)"};

    // Crops options
    private final String[] cropsOptions = {
            "Sugarcane",
            "Corn (maize)",
            "Rice (palay)",
            "Pineapple",
            "Banana",
            "Coffee",
            "Papaya",
            "Watermelon",
            "Mango",
            "Tomato",
            "Eggplant",
            "String beans",
            "Cassava",
            "Sweet potato (kamote)",
            "Taro (gabi)",
            "Coconut",
            "Peanuts",
            "Soybeans",
            "Others (please specify)"
    };

    // Livestock options
    private final String[] livestockOptions = {
            "Select livestock type",
            "Cattle (Beef and Dairy)",
            "Carabao (Water Buffalo)",
            "Goats",
            "Pigs (Swine)",
            "Chicken (Broilers and Layers)",
            "Ducks",
            "Turkeys",
            "Native Chickens",
            "Others (please specify)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farm_info);

        db = FirebaseFirestore.getInstance();
        farmerData = new HashMap<>();
        cropFarmData = new HashMap<>();
        livestockFarmData = new HashMap<>();
        mixedFarmData = new HashMap<>();

        extractFarmerData();
        initViews();
        setupFarmTypeSelection();
        setupClickListeners();
        setupSpinners();
        setupUnitSpinner();
        setupCropsSpinner();
        setupLivestockSpinner();
        setDefaultMunicipalValues();
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

            // Add crop data if it was passed from previous activity
            if (extras.containsKey("crop")) {
                farmerData.put("crop", extras.getString("crop", ""));
            }

            String fullName = extras.getString("firstName", "") + " ";
            if (!TextUtils.isEmpty(extras.getString("middleInitial", ""))) {
                fullName += extras.getString("middleInitial", "") + ". ";
            }
            fullName += extras.getString("lastName", "");
            farmerData.put("fullName", fullName);

            Log.d(TAG, "Extracted farmer data: " + farmerData.toString());
        }
    }

    private void initViews() {
        progressOverlay = findViewById(R.id.progress_overlay);
        viewFlipper = findViewById(R.id.view_flipper);
        rgFarmType = findViewById(R.id.rg_farm_type);
        rbCrop = findViewById(R.id.rb_crop);
        rbLivestock = findViewById(R.id.rb_livestock);
        rbMixed = findViewById(R.id.rb_mixed);
        btnBack = findViewById(R.id.btn_back);
        btnSave = findViewById(R.id.btn_save);
        tvTitle = findViewById(R.id.tv_title);
        stepProgress = findViewById(R.id.step_progress);

        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }

        // Initialize Crop form views
        tvMunicipalCrop = findViewById(R.id.tv_municipal_crop);
        etStreetCrop = findViewById(R.id.et_street_crop);
        etLotSize = findViewById(R.id.et_lot_size);
        spinnerBarangayCrop = findViewById(R.id.spinner_barangay_crop);
        spinnerUnit = findViewById(R.id.spinner_unit);
        spinnerCropsGrown = findViewById(R.id.spinner_crops_grown);
        etOtherCrop = findViewById(R.id.et_other_crop);
        layoutOtherCrop = findViewById(R.id.layout_other_crop);

        // Initialize Livestock form views
        tvMunicipalLivestock = findViewById(R.id.tv_municipal_livestock);
        etStreetLivestock = findViewById(R.id.et_street_livestock);
        spinnerBarangayLivestock = findViewById(R.id.spinner_barangay_livestock);
        spinnerLivestockType = findViewById(R.id.spinner_livestock_type);
        etOtherLivestock = findViewById(R.id.et_other_livestock);
        layoutOtherLivestock = findViewById(R.id.layout_other_livestock);
        etLivestockCount = findViewById(R.id.et_animal_count);

        // Initialize Mixed form views
        tvMunicipalMixed = findViewById(R.id.tv_municipal_mixed);
        etStreetMixed = findViewById(R.id.et_street_mixed);
        spinnerBarangayMixed = findViewById(R.id.spinner_barangay_mixed);
        spinnerCropsGrownMixed = findViewById(R.id.spinner_crops_grown_mixed);
        etOtherCropMixed = findViewById(R.id.et_other_crop_mixed);
        layoutOtherCropMixed = findViewById(R.id.layout_other_crop_mixed);
        etLotSizeMixed = findViewById(R.id.et_lot_size_mixed);
        spinnerUnitMixed = findViewById(R.id.spinner_unit_mixed);
        spinnerLivestockTypeMixed = findViewById(R.id.spinner_livestock_type_mixed);
        etOtherLivestockMixed = findViewById(R.id.et_other_livestock_mixed);
        layoutOtherLivestockMixed = findViewById(R.id.layout_other_livestock_mixed);
        etLivestockCountMixed = findViewById(R.id.et_animal_count_mixed);
        viewFlipper = findViewById(R.id.view_flipper);

        // Initially hide the "Other" fields
        if (layoutOtherCrop != null) {
            layoutOtherCrop.setVisibility(View.GONE);
        }
        if (layoutOtherLivestock != null) {
            layoutOtherLivestock.setVisibility(View.GONE);
        }
        if (layoutOtherCropMixed != null) {
            layoutOtherCropMixed.setVisibility(View.GONE);
        }
        if (layoutOtherLivestockMixed != null) {
            layoutOtherLivestockMixed.setVisibility(View.GONE);
        }
    }

    private void setDefaultMunicipalValues() {
        if (tvMunicipalCrop != null) {
            tvMunicipalCrop.setText("Maramag");
        }
        if (tvMunicipalLivestock != null) {
            tvMunicipalLivestock.setText("Maramag");
        }
        if (tvMunicipalMixed != null) {
            tvMunicipalMixed.setText("Maramag");
        }
    }

    private void setupFarmTypeSelection() {
        rbCrop.setChecked(true);
        viewFlipper.setDisplayedChild(FARM_TYPE_CROP);

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
        String[] barangays = {"Anahawon", "Base Camp", "Bayabason", "Bagongsilang", "Camp 1", "Colambugon", "Dagumba-an", "Danggawan", "Dologon", "Kisanday", "Kuya", "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion", "San Miguel", "San Roque", "Tubigon", "Kiharong"};

        // Setup for Crop form
        ArrayAdapter<String> adapterCrop = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barangays);
        adapterCrop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangayCrop.setAdapter(adapterCrop);

        // Setup for Livestock form
        ArrayAdapter<String> adapterLivestock = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barangays);
        adapterLivestock.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangayLivestock.setAdapter(adapterLivestock);

        // Setup for Mixed form
        ArrayAdapter<String> adapterMixed = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barangays);
        adapterMixed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangayMixed.setAdapter(adapterMixed);
    }

    private void setupUnitSpinner() {
        // Create adapter for unit measurement spinner
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                unitOptions
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        // Set default selection to "sqm"
        spinnerUnit.setSelection(0);

        // Setup for Mixed form
        ArrayAdapter<String> unitAdapterMixed = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                unitOptions
        );
        unitAdapterMixed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnitMixed.setAdapter(unitAdapterMixed);

        // Set default selection to "sqm"
        spinnerUnitMixed.setSelection(0);
    }

    private void setupCropsSpinner() {
        // Create adapter for crops spinner
        ArrayAdapter<String> cropsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cropsOptions
        );
        cropsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCropsGrown.setAdapter(cropsAdapter);

        // Set listener to handle selection
        spinnerCropsGrown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCrop = parent.getItemAtPosition(position).toString();

                // Show the "Other" field if "Others (please specify)" is selected
                if ("Others (please specify)".equals(selectedCrop)) {
                    layoutOtherCrop.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherCrop.setVisibility(View.GONE);
                    etOtherCrop.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Setup for Mixed form
        ArrayAdapter<String> cropsAdapterMixed = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cropsOptions
        );
        cropsAdapterMixed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCropsGrownMixed.setAdapter(cropsAdapterMixed);

        // Set listener to handle selection
        spinnerCropsGrownMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCropMixed = parent.getItemAtPosition(position).toString();

                // Show the "Other" field if "Others (please specify)" is selected
                if ("Others (please specify)".equals(selectedCropMixed)) {
                    layoutOtherCropMixed.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherCropMixed.setVisibility(View.GONE);
                    etOtherCropMixed.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Check if we have a crop from previous activity
        if (farmerData.containsKey("crop")) {
            String previousCrop = (String) farmerData.get("crop");
            if (previousCrop != null && !previousCrop.isEmpty()) {
                // Try to find the crop in our list
                for (int i = 0; i < cropsOptions.length; i++) {
                    if (cropsOptions[i].equals(previousCrop)) {
                        spinnerCropsGrown.setSelection(i);
                        spinnerCropsGrownMixed.setSelection(i);
                        return;
                    }
                }

                // If not found, select "Others" and set the text
                for (int i = 0; i < cropsOptions.length; i++) {
                    if (cropsOptions[i].equals("Others (please specify)")) {
                        spinnerCropsGrown.setSelection(i);
                        spinnerCropsGrownMixed.setSelection(i);
                        etOtherCrop.setText(previousCrop);
                        etOtherCropMixed.setText(previousCrop);
                        layoutOtherCrop.setVisibility(View.VISIBLE);
                        layoutOtherCropMixed.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        }
    }

    private void setupLivestockSpinner() {
        // Create adapter for livestock spinner
        ArrayAdapter<String> livestockAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                livestockOptions
        );
        livestockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLivestockType.setAdapter(livestockAdapter);

        // Set listener to handle selection
        spinnerLivestockType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLivestock = parent.getItemAtPosition(position).toString();

                // Show the "Other" field if "Others (please specify)" is selected
                if ("Others (please specify)".equals(selectedLivestock)) {
                    layoutOtherLivestock.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherLivestock.setVisibility(View.GONE);
                    etOtherLivestock.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Setup for Mixed form
        ArrayAdapter<String> livestockAdapterMixed = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                livestockOptions
        );
        livestockAdapterMixed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLivestockTypeMixed.setAdapter(livestockAdapterMixed);

        // Set listener to handle selection
        spinnerLivestockTypeMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLivestockMixed = parent.getItemAtPosition(position).toString();

                // Show the "Other" field if "Others (please specify)" is selected
                if ("Others (please specify)".equals(selectedLivestockMixed)) {
                    layoutOtherLivestockMixed.setVisibility(View.VISIBLE);
                } else {
                    layoutOtherLivestockMixed.setVisibility(View.GONE);
                    etOtherLivestockMixed.setText("");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
            if (validateCurrentForm()) {
                Log.d(TAG, "Form validation passed");
                collectCurrentFormData();
                Log.d(TAG, "Form data collected");
                saveDataToFirestore();
            } else {
                Log.d(TAG, "Form validation failed");
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

        if (TextUtils.isEmpty(etStreetCrop.getText())) {
            etStreetCrop.setError("Street address is required");
            isValid = false;
        }

        // Validate crop selection
        if (spinnerCropsGrown.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a crop", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if ("Others (please specify)".equals(selectedCrop) &&
                TextUtils.isEmpty(etOtherCrop.getText())) {
            etOtherCrop.setError("Please specify the crop");
            isValid = false;
        }

        if (TextUtils.isEmpty(etLotSize.getText())) {
            etLotSize.setError("Lot size is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateLivestockForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(etStreetLivestock.getText())) {
            etStreetLivestock.setError("Street address is required");
            isValid = false;
        }

        // Validate livestock selection
        if (spinnerLivestockType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a livestock type", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if ("Others (please specify)".equals(selectedLivestock) &&
                TextUtils.isEmpty(etOtherLivestock.getText())) {
            etOtherLivestock.setError("Please specify the livestock type");
            isValid = false;
        }

        if (TextUtils.isEmpty(etLivestockCount.getText())) {
            etLivestockCount.setError("Number of animals is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateMixedForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(etStreetMixed.getText())) {
            etStreetMixed.setError("Street address is required");
            isValid = false;
        }

        // Validate crop selection
        if (spinnerCropsGrownMixed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a crop", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if ("Others (please specify)".equals(selectedCropMixed) &&
                TextUtils.isEmpty(etOtherCropMixed.getText())) {
            etOtherCropMixed.setError("Please specify the crop");
            isValid = false;
        }

        if (TextUtils.isEmpty(etLotSizeMixed.getText())) {
            etLotSizeMixed.setError("Lot size is required");
            isValid = false;
        }

        // Validate livestock selection
        if (spinnerLivestockTypeMixed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a livestock type", Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if ("Others (please specify)".equals(selectedLivestockMixed) &&
                TextUtils.isEmpty(etOtherLivestockMixed.getText())) {
            etOtherLivestockMixed.setError("Please specify the livestock type");
            isValid = false;
        }

        if (TextUtils.isEmpty(etLivestockCountMixed.getText())) {
            etLivestockCountMixed.setError("Number of animals is required");
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
        cropFarmData.clear(); // Clear previous data to avoid issues

        cropFarmData.put("farmType", "Crop");
        cropFarmData.put("municipal", tvMunicipalCrop.getText().toString().trim());
        cropFarmData.put("barangay", spinnerBarangayCrop.getSelectedItem().toString());
        cropFarmData.put("street", etStreetCrop.getText().toString().trim());

        // Get the selected crop
        String cropValue;
        if ("Others (please specify)".equals(selectedCrop)) {
            cropValue = etOtherCrop.getText().toString().trim();
        } else {
            cropValue = selectedCrop;
        }
        cropFarmData.put("cropsGrown", cropValue);

        // Combine lot size value with selected unit
        String lotSizeValue = etLotSize.getText().toString().trim();
        String lotSizeUnit = spinnerUnit.getSelectedItem().toString();
        String lotSizeComplete = lotSizeValue + " " + lotSizeUnit;

        cropFarmData.put("lotSize", lotSizeComplete);
        cropFarmData.put("lotSizeValue", lotSizeValue);
        cropFarmData.put("lotSizeUnit", lotSizeUnit);

        cropFarmData.put("farmerId", farmerData.get("farmerId"));
        cropFarmData.put("farmerName", farmerData.get("fullName"));

        Log.d(TAG, "Collected crop farm data: " + cropFarmData.toString());
    }

    private void collectLivestockFormData() {
        livestockFarmData.clear(); // Clear previous data to avoid issues

        livestockFarmData.put("farmType", "Livestock");
        livestockFarmData.put("municipal", tvMunicipalLivestock.getText().toString().trim());
        livestockFarmData.put("barangay", spinnerBarangayLivestock.getSelectedItem().toString());
        livestockFarmData.put("street", etStreetLivestock.getText().toString().trim());

        // Get the selected livestock type
        String livestockValue;
        if ("Others (please specify)".equals(selectedLivestock)) {
            livestockValue = etOtherLivestock.getText().toString().trim();
        } else {
            livestockValue = selectedLivestock;
        }
        livestockFarmData.put("livestockType", livestockValue);

        // Get livestock count
        String livestockCount = etLivestockCount.getText().toString().trim();
        livestockFarmData.put("livestockCount", livestockCount);

        livestockFarmData.put("farmerId", farmerData.get("farmerId"));
        livestockFarmData.put("farmerName", farmerData.get("fullName"));

        Log.d(TAG, "Collected livestock farm data: " + livestockFarmData.toString());
    }

    private void collectMixedFormData() {
        mixedFarmData.clear(); // Clear previous data to avoid issues

        mixedFarmData.put("farmType", "Mixed");
        mixedFarmData.put("municipal", tvMunicipalMixed.getText().toString().trim());
        mixedFarmData.put("barangay", spinnerBarangayMixed.getSelectedItem().toString());
        mixedFarmData.put("street", etStreetMixed.getText().toString().trim());

        // Get the selected crop
        String cropValue;
        if ("Others (please specify)".equals(selectedCropMixed)) {
            cropValue = etOtherCropMixed.getText().toString().trim();
        } else {
            cropValue = selectedCropMixed;
        }
        mixedFarmData.put("cropsGrown", cropValue);

        // Combine lot size value with selected unit
        String lotSizeValue = etLotSizeMixed.getText().toString().trim();
        String lotSizeUnit = spinnerUnitMixed.getSelectedItem().toString();
        String lotSizeComplete = lotSizeValue + " " + lotSizeUnit;

        mixedFarmData.put("lotSize", lotSizeComplete);
        mixedFarmData.put("lotSizeValue", lotSizeValue);
        mixedFarmData.put("lotSizeUnit", lotSizeUnit);

        // Get the selected livestock type
        String livestockValue;
        if ("Others (please specify)".equals(selectedLivestockMixed)) {
            livestockValue = etOtherLivestockMixed.getText().toString().trim();
        } else {
            livestockValue = selectedLivestockMixed;
        }
        mixedFarmData.put("livestockType", livestockValue);

        // Get livestock count
        String livestockCount = etLivestockCountMixed.getText().toString().trim();
        mixedFarmData.put("livestockCount", livestockCount);

        mixedFarmData.put("farmerId", farmerData.get("farmerId"));
        mixedFarmData.put("farmerName", farmerData.get("fullName"));

        Log.d(TAG, "Collected mixed farm data: " + mixedFarmData.toString());
    }

    private void saveDataToFirestore() {
        Log.d(TAG, "Starting saveDataToFirestore method");

        String farmerId = (String) farmerData.get("farmerId");
        if (farmerId == null || farmerId.isEmpty()) {
            Toast.makeText(this, "Farmer ID is required", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save failed: Farmer ID is empty");
            return;
        }

        try {
            String selectedBarangay;
            String municipal;

            switch (currentFarmType) {
                case FARM_TYPE_CROP:
                    selectedBarangay = spinnerBarangayCrop.getSelectedItem().toString();
                    municipal = tvMunicipalCrop.getText().toString().trim();
                    break;
                case FARM_TYPE_LIVESTOCK:
                    selectedBarangay = spinnerBarangayLivestock.getSelectedItem().toString();
                    municipal = tvMunicipalLivestock.getText().toString().trim();
                    break;
                case FARM_TYPE_MIXED:
                    selectedBarangay = spinnerBarangayMixed.getSelectedItem().toString();
                    municipal = tvMunicipalMixed.getText().toString().trim();
                    break;
                default:
                    selectedBarangay = spinnerBarangayCrop.getSelectedItem().toString();
                    municipal = tvMunicipalCrop.getText().toString().trim();
            }

            Log.d(TAG, "Saving data for farmer ID: " + farmerId);
            Log.d(TAG, "Selected barangay: " + selectedBarangay);
            Log.d(TAG, "Municipal: " + municipal);

            showLoading(true);
            Timestamp now = Timestamp.now();
            farmerData.put("createdAt", now);
            farmerData.put("lastUpdated", now);
            farmerData.put("barangay", selectedBarangay);
            farmerData.put("municipal", municipal);

            WriteBatch batch = db.batch();

            String lastName = (String) farmerData.get("lastName");
            String firstName = (String) farmerData.get("firstName");
            String middleInitial = (String) farmerData.get("middleInitial");

            if (lastName == null || firstName == null) {
                Toast.makeText(this, "Name information is incomplete", Toast.LENGTH_SHORT).show();
                showLoading(false);
                return;
            }

            // Format the document ID to match the UI pattern: "Last, First M."
            String farmerDocId = lastName + ", " + firstName;
            if (middleInitial != null && !middleInitial.isEmpty()) {
                farmerDocId += " " + middleInitial + ".";
            }

            Log.d(TAG, "Generated farmer document ID: " + farmerDocId);

            // 1. Save to Barangays collection
            DocumentReference barangayRef = db.collection("Barangays").document(selectedBarangay);
            Map<String, Object> barangayData = new HashMap<>();
            barangayData.put("name", selectedBarangay);
            barangayData.put("lastUpdated", now);
            batch.set(barangayRef, barangayData, SetOptions.merge());

            // 2. Save farmer under Barangay's Farmers subcollection
            DocumentReference barangayFarmerRef = barangayRef.collection("Farmers").document(farmerDocId);
            batch.set(barangayFarmerRef, farmerData);

            // 3. Save farm type data under the farmer
            Map<String, Object> farmData;
            String farmTypeDocName;

            switch (currentFarmType) {
                case FARM_TYPE_CROP:
                    farmData = new HashMap<>(cropFarmData);
                    farmTypeDocName = "Crop";
                    break;
                case FARM_TYPE_LIVESTOCK:
                    farmData = new HashMap<>(livestockFarmData);
                    farmTypeDocName = "Livestock";
                    break;
                case FARM_TYPE_MIXED:
                    farmData = new HashMap<>(mixedFarmData);
                    farmTypeDocName = "Mixed";
                    break;
                default:
                    farmData = new HashMap<>(cropFarmData);
                    farmTypeDocName = "Crop";
            }

            farmData.put("createdAt", now);
            farmData.put("lastUpdated", now);
            farmData.put("farmerName", farmerDocId); // Use the formatted name

            DocumentReference farmTypeRef = barangayFarmerRef.collection("FarmType").document(farmTypeDocName);
            batch.set(farmTypeRef, farmData);

            // 4. Update notifications (if needed)
            DocumentReference notificationRef = db.collection("Notifications").document("new_farmers");
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("lastUpdated", now);
            notificationData.put("message", "New farmer added: " + farmerDocId);
            batch.set(notificationRef, notificationData, SetOptions.merge());

            // Commit the batch
            batch.commit()
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Batch commit succeeded!");
                            Toast.makeText(FarmInfoActivity.this, "Farmer saved successfully!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(FarmInfoActivity.this, SuccessedAddFarmer.class);
                            intent.putExtra("success_message", "Farmer's information saved successfully!");
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e(TAG, "Batch commit failed", task.getException());
                            new AlertDialog.Builder(FarmInfoActivity.this)
                                    .setTitle("Database Error")
                                    .setMessage("Error saving data: " + (task.getException() != null ?
                                            task.getException().getMessage() : "Unknown error"))
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in saveDataToFirestore", e);
            showLoading(false);
            new AlertDialog.Builder(FarmInfoActivity.this)
                    .setTitle("Error")
                    .setMessage("An error occurred: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void showLoading(boolean isLoading) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        btnSave.setEnabled(!isLoading);
        btnBack.setEnabled(!isLoading);
        rgFarmType.setEnabled(!isLoading);
    }
}
