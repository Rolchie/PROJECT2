package com.maramagagriculturalaid.app.FarmersData;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.ActivityLogger;

import java.util.HashMap;
import java.util.Map;

public class EditFarmInformationActivity extends AppCompatActivity {

    private static final String TAG = "EditFarmInfo";

    // UI Components
    private ImageButton btnBack;
    private TextView tvFarmerName;
    private RadioGroup rgFarmType;
    private RadioButton rbCrop, rbLivestock, rbMixed;
    private ViewFlipper viewFlipper;
    private AppCompatButton btnSave;
    private View progressOverlay;

    // Crop Form Components
    private EditText etMunicipalCrop, etStreetCrop, etOtherCrop;
    private TextView tvBarangayCrop;
    private AppCompatSpinner spinnerCropsGrown;
    private LinearLayout layoutOtherCrop;
    private EditText etLotSizeValue;
    private Spinner spinnerUnit;

    // Livestock Form Components
    private EditText etMunicipalLivestock, etStreetLivestock, etOtherLivestock, etAnimalCount;
    private TextView tvBarangayLivestock;
    private AppCompatSpinner spinnerLivestockType;
    private LinearLayout layoutOtherLivestock;

    // Mixed Form Components
    private EditText etMunicipalMixed, etStreetMixed, etOtherCropMixed, etOtherLivestockMixed;
    private TextView tvBarangayMixed;
    private AppCompatSpinner spinnerCropsGrownMixed, spinnerLivestockTypeMixed;
    private LinearLayout layoutOtherCropMixed, layoutOtherLivestockMixed;
    private EditText etLotSizeValueMixed, etAnimalCountMixed;
    private Spinner spinnerUnitMixed;

    // Data
    private FirebaseFirestore db;
    private String farmerId, farmerName, barangay, farmerDocumentId;
    private String currentFarmType = "";

    // Basic information from EditFarmerActivity
    private String firstName, middleName, lastName, contactNumber;

    // Farm type data storage
    private Map<String, Object> cropData = new HashMap<>();
    private Map<String, Object> livestockData = new HashMap<>();

    // Shared data across farm types
    private String sharedStreet = "";
    private String sharedMunicipal = "";

    // Unsaved changes tracking
    private boolean hasUnsavedChanges = false;
    private Map<String, String> originalValues = new HashMap<>();
    private String originalFarmType = "";

    // Predefined barangays in Maramag Municipality
    private final String[] availableBarangays = {
            "Anahawon", "Base Camp", "Bayabason", "Camp 1", "Colambugon",
            "Dagumba-an", "Danggawan", "Dologon", "Kisanday", "Kuya",
            "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion",
            "San Miguel", "San Roque", "Tubigon", "Kiharong", "Bagongsilang"
    };

    // Spinner Data
    private String[] cropOptions = {"Select Crop", "Rice", "Corn", "Vegetables", "Fruits", "Root Crops", "Pineapple", "Other"};
    private String[] livestockOptions = {"Select Livestock", "Cattle", "Carabao", "Goat", "Goats", "Pig", "Chicken", "Duck", "Other"};
    private String[] unitOptions = {"Hectares(ha)", "Square Meters"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_farm_information);

        db = FirebaseFirestore.getInstance();

        // Get data from intent
        farmerId = getIntent().getStringExtra("farmerId");
        farmerName = getIntent().getStringExtra("farmerName");
        barangay = getIntent().getStringExtra("barangay");
        farmerDocumentId = getIntent().getStringExtra("farmerDocumentId");

        // Get basic information from EditFarmerActivity
        firstName = getIntent().getStringExtra("firstName");
        middleName = getIntent().getStringExtra("middleName");
        lastName = getIntent().getStringExtra("lastName");
        contactNumber = getIntent().getStringExtra("contactNumber");

        // Debug logging
        Log.d(TAG, "Received data - farmerId: " + farmerId + ", farmerName: " + farmerName +
                ", barangay: " + barangay + ", farmerDocumentId: " + farmerDocumentId);
        Log.d(TAG, "Basic info - firstName: " + firstName + ", lastName: " + lastName);

        initViews();
        setupSpinners();
        setupClickListeners();
        setupChangeListeners();

        // Handle missing barangay information gracefully
        if (barangay == null || barangay.isEmpty()) {
            handleMissingBarangay();
        } else {
            // Validate barangay exists in our predefined list
            boolean isValidBarangay = false;
            for (String validBarangay : availableBarangays) {
                if (validBarangay.equalsIgnoreCase(barangay)) {
                    barangay = validBarangay; // Use correct case
                    isValidBarangay = true;
                    break;
                }
            }

            if (isValidBarangay) {
                loadFarmerData();
            } else {
                Log.w(TAG, "Invalid barangay: " + barangay);
                handleMissingBarangay();
            }
        }
    }

    private void handleMissingBarangay() {
        Log.w(TAG, "Missing or invalid barangay information");
        showBarangaySelectionDialog();
    }

    private void showBarangaySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Barangay");
        builder.setMessage("Please select the barangay where this farmer is located:");

        builder.setItems(availableBarangays, (dialog, which) -> {
            barangay = availableBarangays[which];
            Log.d(TAG, "User selected barangay: " + barangay);
            loadFarmerData();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "Cannot proceed without barangay information", Toast.LENGTH_LONG).show();
            finish();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvFarmerName = findViewById(R.id.tv_farmer_name);
        rgFarmType = findViewById(R.id.rg_farm_type);
        rbCrop = findViewById(R.id.rb_crop);
        rbLivestock = findViewById(R.id.rb_livestock);
        rbMixed = findViewById(R.id.rb_mixed);
        viewFlipper = findViewById(R.id.view_flipper);
        btnSave = findViewById(R.id.btn_save);
        progressOverlay = findViewById(R.id.progress_overlay);

        // Crop form components
        etMunicipalCrop = findViewById(R.id.et_municipal_crop);
        etStreetCrop = findViewById(R.id.et_street_crop);
        tvBarangayCrop = findViewById(R.id.tv_barangay_crop);
        spinnerCropsGrown = findViewById(R.id.spinner_crops_grown);
        layoutOtherCrop = findViewById(R.id.layout_other_crop);
        etOtherCrop = findViewById(R.id.et_other_crop);
        etLotSizeValue = findViewById(R.id.et_lot_size_value);
        spinnerUnit = findViewById(R.id.spinner_unit);

        // Livestock form components
        etMunicipalLivestock = findViewById(R.id.et_municipal_livestock);
        etStreetLivestock = findViewById(R.id.et_street_livestock);
        tvBarangayLivestock = findViewById(R.id.tv_barangay_livestock);
        spinnerLivestockType = findViewById(R.id.spinner_livestock_type);
        layoutOtherLivestock = findViewById(R.id.layout_other_livestock);
        etOtherLivestock = findViewById(R.id.et_other_livestock);
        etAnimalCount = findViewById(R.id.et_animal_count);

        // Mixed form components
        etMunicipalMixed = findViewById(R.id.et_municipal_mixed);
        etStreetMixed = findViewById(R.id.et_street_mixed);
        tvBarangayMixed = findViewById(R.id.tv_barangay_mixed);
        spinnerCropsGrownMixed = findViewById(R.id.spinner_crops_grown_mixed);
        layoutOtherCropMixed = findViewById(R.id.layout_other_crop_mixed);
        etOtherCropMixed = findViewById(R.id.et_other_crop_mixed);
        spinnerLivestockTypeMixed = findViewById(R.id.spinner_livestock_type_mixed);
        layoutOtherLivestockMixed = findViewById(R.id.layout_other_livestock_mixed);
        etOtherLivestockMixed = findViewById(R.id.et_other_livestock_mixed);
        etLotSizeValueMixed = findViewById(R.id.et_lot_size_value_mixed);
        etAnimalCountMixed = findViewById(R.id.et_animal_count_mixed);
        spinnerUnitMixed = findViewById(R.id.spinner_unit_mixed);

        // Set farmer name
        if (tvFarmerName != null && farmerName != null) {
            tvFarmerName.setText(farmerName);
        }

        // Hide progress overlay initially
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
    }

    private void setupSpinners() {
        // Setup crop spinners
        if (spinnerCropsGrown != null) {
            ArrayAdapter<String> cropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cropOptions);
            cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCropsGrown.setAdapter(cropAdapter);
        }

        if (spinnerCropsGrownMixed != null) {
            ArrayAdapter<String> cropMixedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cropOptions);
            cropMixedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCropsGrownMixed.setAdapter(cropMixedAdapter);
        }

        // Setup livestock spinners
        if (spinnerLivestockType != null) {
            ArrayAdapter<String> livestockAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, livestockOptions);
            livestockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLivestockType.setAdapter(livestockAdapter);
        }

        if (spinnerLivestockTypeMixed != null) {
            ArrayAdapter<String> livestockMixedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, livestockOptions);
            livestockMixedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerLivestockTypeMixed.setAdapter(livestockMixedAdapter);
        }

        // Setup unit spinners
        if (spinnerUnit != null) {
            ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitOptions);
            unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUnit.setAdapter(unitAdapter);
        }

        if (spinnerUnitMixed != null) {
            ArrayAdapter<String> unitMixedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitOptions);
            unitMixedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUnitMixed.setAdapter(unitMixedAdapter);
        }

        // Setup spinner listeners
        setupSpinnerListeners();
    }

    private void setupSpinnerListeners() {
        // Crop spinner listener
        if (spinnerCropsGrown != null) {
            spinnerCropsGrown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (layoutOtherCrop != null) {
                        layoutOtherCrop.setVisibility(position == cropOptions.length - 1 ? View.VISIBLE : View.GONE);
                    }
                    hasUnsavedChanges = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Mixed crop spinner listener
        if (spinnerCropsGrownMixed != null) {
            spinnerCropsGrownMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (layoutOtherCropMixed != null) {
                        layoutOtherCropMixed.setVisibility(position == cropOptions.length - 1 ? View.VISIBLE : View.GONE);
                    }
                    hasUnsavedChanges = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Livestock spinner listener
        if (spinnerLivestockType != null) {
            spinnerLivestockType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (layoutOtherLivestock != null) {
                        layoutOtherLivestock.setVisibility(position == livestockOptions.length - 1 ? View.VISIBLE : View.GONE);
                    }
                    hasUnsavedChanges = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        // Mixed livestock spinner listener
        if (spinnerLivestockTypeMixed != null) {
            spinnerLivestockTypeMixed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (layoutOtherLivestockMixed != null) {
                        layoutOtherLivestockMixed.setVisibility(position == livestockOptions.length - 1 ? View.VISIBLE : View.GONE);
                    }
                    hasUnsavedChanges = true;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackPress());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (validateForm()) {
                    saveAllFarmerInformation();
                }
            });
        }

        // Radio group listener
        if (rgFarmType != null) {
            rgFarmType.setOnCheckedChangeListener((group, checkedId) -> {
                hasUnsavedChanges = true;
                if (checkedId == R.id.rb_crop) {
                    currentFarmType = "Crop";
                    viewFlipper.setDisplayedChild(0);
                    autoFillCropForm();
                } else if (checkedId == R.id.rb_livestock) {
                    currentFarmType = "Livestock";
                    viewFlipper.setDisplayedChild(1);
                    autoFillLivestockForm();
                } else if (checkedId == R.id.rb_mixed) {
                    currentFarmType = "Mixed";
                    viewFlipper.setDisplayedChild(2);
                    autoFillMixedForm();
                }
            });
        }
    }

    private void setupChangeListeners() {
        TextWatcher changeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                hasUnsavedChanges = true;
            }
        };

        // Add change listeners to all EditText fields
        if (etMunicipalCrop != null) etMunicipalCrop.addTextChangedListener(changeWatcher);
        if (etStreetCrop != null) etStreetCrop.addTextChangedListener(changeWatcher);
        if (etOtherCrop != null) etOtherCrop.addTextChangedListener(changeWatcher);
        if (etLotSizeValue != null) etLotSizeValue.addTextChangedListener(changeWatcher);
        if (etMunicipalLivestock != null) etMunicipalLivestock.addTextChangedListener(changeWatcher);
        if (etStreetLivestock != null) etStreetLivestock.addTextChangedListener(changeWatcher);
        if (etOtherLivestock != null) etOtherLivestock.addTextChangedListener(changeWatcher);
        if (etAnimalCount != null) etAnimalCount.addTextChangedListener(changeWatcher);
        if (etMunicipalMixed != null) etMunicipalMixed.addTextChangedListener(changeWatcher);
        if (etStreetMixed != null) etStreetMixed.addTextChangedListener(changeWatcher);
        if (etOtherCropMixed != null) etOtherCropMixed.addTextChangedListener(changeWatcher);
        if (etOtherLivestockMixed != null) etOtherLivestockMixed.addTextChangedListener(changeWatcher);
        if (etLotSizeValueMixed != null) etLotSizeValueMixed.addTextChangedListener(changeWatcher);
        if (etAnimalCountMixed != null) etAnimalCountMixed.addTextChangedListener(changeWatcher);
    }

    private void loadFarmerData() {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }

        // Set barangay in all forms
        if (tvBarangayCrop != null) tvBarangayCrop.setText(barangay);
        if (tvBarangayLivestock != null) tvBarangayLivestock.setText(barangay);
        if (tvBarangayMixed != null) tvBarangayMixed.setText(barangay);

        // Load farm type data
        loadFarmTypeData(farmerDocumentId);
    }

    // FIXED: Enhanced to properly detect and handle mixed farm types
    private void loadFarmTypeData(String farmerDocId) {
        Log.d(TAG, "Loading farm type data for farmer: " + farmerDocId);

        db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocId)
                .collection("FarmType")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean hasCropData = false;
                        boolean hasLivestockData = false;

                        // Clear previous data
                        cropData.clear();
                        livestockData.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String docId = document.getId();
                            Map<String, Object> documentData = document.getData();

                            Log.d(TAG, "Found farm type document: " + docId);
                            Log.d(TAG, "Document data: " + documentData);

                            // Extract shared data (street and municipal)
                            String street = (String) documentData.get("street");
                            String municipal = (String) documentData.get("municipal");

                            if (!TextUtils.isEmpty(street)) {
                                sharedStreet = street;
                            }
                            if (!TextUtils.isEmpty(municipal)) {
                                sharedMunicipal = municipal;
                            }

                            // Store data based on document ID
                            if ("Crop".equals(docId)) {
                                cropData.putAll(documentData);
                                hasCropData = true;
                                Log.d(TAG, "Loaded crop data: " + cropData);
                            } else if ("Livestock".equals(docId)) {
                                livestockData.putAll(documentData);
                                hasLivestockData = true;
                                Log.d(TAG, "Loaded livestock data: " + livestockData);
                            }
                        }

                        // Determine farm type based on available data
                        if (hasCropData && hasLivestockData) {
                            // Mixed farm type - has both crop and livestock data
                            currentFarmType = "Mixed";
                            populateMixedFormFromData();
                            rbMixed.setChecked(true);
                            viewFlipper.setDisplayedChild(2);
                            Log.d(TAG, "Detected Mixed farm type with both crop and livestock data");
                        } else if (hasCropData) {
                            // Crop only
                            currentFarmType = "Crop";
                            populateCropFormFromData();
                            rbCrop.setChecked(true);
                            viewFlipper.setDisplayedChild(0);
                            Log.d(TAG, "Detected Crop farm type");
                        } else if (hasLivestockData) {
                            // Livestock only
                            currentFarmType = "Livestock";
                            populateLivestockFormFromData();
                            rbLivestock.setChecked(true);
                            viewFlipper.setDisplayedChild(1);
                            Log.d(TAG, "Detected Livestock farm type");
                        } else {
                            // No farm type data found, default to Crop
                            Log.d(TAG, "No farm type data found, defaulting to Crop");
                            currentFarmType = "Crop";
                            rbCrop.setChecked(true);
                            viewFlipper.setDisplayedChild(0);
                        }

                        // Auto-fill shared data to all forms
                        autoFillAllForms();

                        // Save original values after data is loaded
                        saveOriginalValues();

                        if (progressOverlay != null) {
                            progressOverlay.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "Error loading farm type data", task.getException());
                        if (progressOverlay != null) {
                            progressOverlay.setVisibility(View.GONE);
                        }
                        // Default to Crop form
                        currentFarmType = "Crop";
                        rbCrop.setChecked(true);
                        viewFlipper.setDisplayedChild(0);
                        saveOriginalValues();
                    }
                });
    }

    private void populateCropFormFromData() {
        Log.d(TAG, "Populating crop form from data");
        if (cropData != null && !cropData.isEmpty()) {
            String municipal = (String) cropData.get("municipal");
            String street = (String) cropData.get("street");
            String cropsGrown = (String) cropData.get("cropsGrown");
            String lotSizeValue = (String) cropData.get("lotSizeValue");
            String lotSizeUnit = (String) cropData.get("lotSizeUnit");

            if (municipal != null) etMunicipalCrop.setText(municipal);
            if (street != null) etStreetCrop.setText(street);
            if (lotSizeValue != null) etLotSizeValue.setText(lotSizeValue);

            // Set unit spinner
            if (lotSizeUnit != null) {
                for (int i = 0; i < unitOptions.length; i++) {
                    if (unitOptions[i].equals(lotSizeUnit)) {
                        spinnerUnit.setSelection(i);
                        break;
                    }
                }
            }

            // Set crop spinner
            if (cropsGrown != null && !cropsGrown.isEmpty()) {
                boolean found = false;
                for (int i = 1; i < cropOptions.length - 1; i++) { // Skip "Select Crop" and "Other"
                    if (cropOptions[i].equalsIgnoreCase(cropsGrown)) {
                        spinnerCropsGrown.setSelection(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Set to "Other" and show custom field
                    spinnerCropsGrown.setSelection(cropOptions.length - 1);
                    layoutOtherCrop.setVisibility(View.VISIBLE);
                    etOtherCrop.setText(cropsGrown);
                }
            }
        }
    }

    private void populateLivestockFormFromData() {
        Log.d(TAG, "Populating livestock form from data");
        if (livestockData != null && !livestockData.isEmpty()) {
            String municipal = (String) livestockData.get("municipal");
            String street = (String) livestockData.get("street");
            String livestockType = (String) livestockData.get("livestockType");
            Object livestockCountObj = livestockData.get("livestockCount");

            if (municipal != null) etMunicipalLivestock.setText(municipal);
            if (street != null) etStreetLivestock.setText(street);
            if (livestockCountObj != null) etAnimalCount.setText(String.valueOf(livestockCountObj));

            // Set livestock spinner
            if (livestockType != null && !livestockType.isEmpty()) {
                boolean found = false;
                for (int i = 1; i < livestockOptions.length - 1; i++) { // Skip "Select Livestock" and "Other"
                    if (livestockOptions[i].equalsIgnoreCase(livestockType)) {
                        spinnerLivestockType.setSelection(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Set to "Other" and show custom field
                    spinnerLivestockType.setSelection(livestockOptions.length - 1);
                    layoutOtherLivestock.setVisibility(View.VISIBLE);
                    etOtherLivestock.setText(livestockType);
                }
            }
        }
    }

    // FIXED: Enhanced mixed form population to handle both crop and livestock data properly
    private void populateMixedFormFromData() {
        Log.d(TAG, "Populating mixed form from data");
        rbMixed.setChecked(true);
        viewFlipper.setDisplayedChild(2);

        // Populate from crop data
        if (cropData != null && !cropData.isEmpty()) {
            Log.d(TAG, "Populating crop section of mixed form");
            String municipal = (String) cropData.get("municipal");
            String street = (String) cropData.get("street");
            String cropsGrown = (String) cropData.get("cropsGrown");
            String lotSizeValue = (String) cropData.get("lotSizeValue");
            String lotSizeUnit = (String) cropData.get("lotSizeUnit");

            if (municipal != null) etMunicipalMixed.setText(municipal);
            if (street != null) etStreetMixed.setText(street);
            if (lotSizeValue != null) etLotSizeValueMixed.setText(lotSizeValue);

            // Set unit spinner for mixed form
            if (lotSizeUnit != null) {
                for (int i = 0; i < unitOptions.length; i++) {
                    if (unitOptions[i].equals(lotSizeUnit)) {
                        spinnerUnitMixed.setSelection(i);
                        break;
                    }
                }
            }

            // Set crop spinner for mixed form
            if (cropsGrown != null && !cropsGrown.isEmpty()) {
                boolean found = false;
                for (int i = 1; i < cropOptions.length - 1; i++) { // Skip "Select Crop" and "Other"
                    if (cropOptions[i].equalsIgnoreCase(cropsGrown)) {
                        spinnerCropsGrownMixed.setSelection(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Set to "Other" and show custom field
                    spinnerCropsGrownMixed.setSelection(cropOptions.length - 1);
                    layoutOtherCropMixed.setVisibility(View.VISIBLE);
                    etOtherCropMixed.setText(cropsGrown);
                }
            }
        }

        // Populate from livestock data
        if (livestockData != null && !livestockData.isEmpty()) {
            Log.d(TAG, "Populating livestock section of mixed form");
            String municipal = (String) livestockData.get("municipal");
            String street = (String) livestockData.get("street");
            String livestockType = (String) livestockData.get("livestockType");
            Object livestockCountObj = livestockData.get("livestockCount");

            // Use livestock data for municipal and street if crop data doesn't have them
            if (municipal != null && TextUtils.isEmpty(etMunicipalMixed.getText())) {
                etMunicipalMixed.setText(municipal);
            }
            if (street != null && TextUtils.isEmpty(etStreetMixed.getText())) {
                etStreetMixed.setText(street);
            }

            if (livestockCountObj != null) {
                etAnimalCountMixed.setText(String.valueOf(livestockCountObj));
            }

            // Set livestock spinner for mixed form
            if (livestockType != null && !livestockType.isEmpty()) {
                boolean found = false;
                for (int i = 1; i < livestockOptions.length - 1; i++) { // Skip "Select Livestock" and "Other"
                    if (livestockOptions[i].equalsIgnoreCase(livestockType)) {
                        spinnerLivestockTypeMixed.setSelection(i);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Set to "Other" and show custom field
                    spinnerLivestockTypeMixed.setSelection(livestockOptions.length - 1);
                    layoutOtherLivestockMixed.setVisibility(View.VISIBLE);
                    etOtherLivestockMixed.setText(livestockType);
                }
            }
        }

        Log.d(TAG, "Mixed form populated with crop and livestock data");
    }

    private void autoFillAllForms() {
        autoFillCropForm();
        autoFillLivestockForm();
        autoFillMixedForm();
    }

    private void autoFillCropForm() {
        if (!TextUtils.isEmpty(sharedMunicipal) && etMunicipalCrop != null && TextUtils.isEmpty(etMunicipalCrop.getText())) {
            etMunicipalCrop.setText(sharedMunicipal);
        }
        if (!TextUtils.isEmpty(sharedStreet) && etStreetCrop != null && TextUtils.isEmpty(etStreetCrop.getText())) {
            etStreetCrop.setText(sharedStreet);
        }
    }

    private void autoFillLivestockForm() {
        if (!TextUtils.isEmpty(sharedMunicipal) && etMunicipalLivestock != null && TextUtils.isEmpty(etMunicipalLivestock.getText())) {
            etMunicipalLivestock.setText(sharedMunicipal);
        }
        if (!TextUtils.isEmpty(sharedStreet) && etStreetLivestock != null && TextUtils.isEmpty(etStreetLivestock.getText())) {
            etStreetLivestock.setText(sharedStreet);
        }
    }

    private void autoFillMixedForm() {
        if (!TextUtils.isEmpty(sharedMunicipal) && etMunicipalMixed != null && TextUtils.isEmpty(etMunicipalMixed.getText())) {
            etMunicipalMixed.setText(sharedMunicipal);
        }
        if (!TextUtils.isEmpty(sharedStreet) && etStreetMixed != null && TextUtils.isEmpty(etStreetMixed.getText())) {
            etStreetMixed.setText(sharedStreet);
        }
    }

    private void saveOriginalValues() {
        originalValues.clear();
        originalFarmType = currentFarmType;

        // Save current form values as original
        if ("Crop".equals(currentFarmType)) {
            saveOriginalCropValues();
        } else if ("Livestock".equals(currentFarmType)) {
            saveOriginalLivestockValues();
        } else if ("Mixed".equals(currentFarmType)) {
            saveOriginalMixedValues();
        }

        hasUnsavedChanges = false;
    }

    private void saveOriginalCropValues() {
        if (etMunicipalCrop != null) originalValues.put("municipal_crop", etMunicipalCrop.getText().toString());
        if (etStreetCrop != null) originalValues.put("street_crop", etStreetCrop.getText().toString());
        if (spinnerCropsGrown != null) originalValues.put("crops_grown", String.valueOf(spinnerCropsGrown.getSelectedItemPosition()));
        if (etOtherCrop != null) originalValues.put("other_crop", etOtherCrop.getText().toString());
        if (etLotSizeValue != null) originalValues.put("lot_size_value", etLotSizeValue.getText().toString());
        if (spinnerUnit != null) originalValues.put("unit", String.valueOf(spinnerUnit.getSelectedItemPosition()));
    }

    private void saveOriginalLivestockValues() {
        if (etMunicipalLivestock != null) originalValues.put("municipal_livestock", etMunicipalLivestock.getText().toString());
        if (etStreetLivestock != null) originalValues.put("street_livestock", etStreetLivestock.getText().toString());
        if (spinnerLivestockType != null) originalValues.put("livestock_type", String.valueOf(spinnerLivestockType.getSelectedItemPosition()));
        if (etOtherLivestock != null) originalValues.put("other_livestock", etOtherLivestock.getText().toString());
        if (etAnimalCount != null) originalValues.put("animal_count", etAnimalCount.getText().toString());
    }

    private void saveOriginalMixedValues() {
        if (etMunicipalMixed != null) originalValues.put("municipal_mixed", etMunicipalMixed.getText().toString());
        if (etStreetMixed != null) originalValues.put("street_mixed", etStreetMixed.getText().toString());
        if (spinnerCropsGrownMixed != null) originalValues.put("crops_grown_mixed", String.valueOf(spinnerCropsGrownMixed.getSelectedItemPosition()));
        if (etOtherCropMixed != null) originalValues.put("other_crop_mixed", etOtherCropMixed.getText().toString());
        if (spinnerLivestockTypeMixed != null) originalValues.put("livestock_type_mixed", String.valueOf(spinnerLivestockTypeMixed.getSelectedItemPosition()));
        if (etOtherLivestockMixed != null) originalValues.put("other_livestock_mixed", etOtherLivestockMixed.getText().toString());
        if (etLotSizeValueMixed != null) originalValues.put("lot_size_value_mixed", etLotSizeValueMixed.getText().toString());
        if (etAnimalCountMixed != null) originalValues.put("animal_count_mixed", etAnimalCountMixed.getText().toString());
        if (spinnerUnitMixed != null) originalValues.put("unit_mixed", String.valueOf(spinnerUnitMixed.getSelectedItemPosition()));
    }

    private boolean validateForm() {
        if ("Crop".equals(currentFarmType)) {
            return validateCropForm();
        } else if ("Livestock".equals(currentFarmType)) {
            return validateLivestockForm();
        } else if ("Mixed".equals(currentFarmType)) {
            return validateMixedForm();
        }
        return false;
    }

    private boolean validateCropForm() {
        boolean isValid = true;

        if (etMunicipalCrop != null && TextUtils.isEmpty(etMunicipalCrop.getText())) {
            etMunicipalCrop.setError("Municipal is required");
            isValid = false;
        }

        if (etStreetCrop != null && TextUtils.isEmpty(etStreetCrop.getText())) {
            etStreetCrop.setError("Street is required");
            isValid = false;
        }

        if (spinnerCropsGrown != null && spinnerCropsGrown.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a crop", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerCropsGrown != null && spinnerCropsGrown.getSelectedItemPosition() == cropOptions.length - 1) {
            if (etOtherCrop != null && TextUtils.isEmpty(etOtherCrop.getText())) {
                etOtherCrop.setError("Please specify the crop");
                isValid = false;
            }
        }

        if (etLotSizeValue != null && TextUtils.isEmpty(etLotSizeValue.getText())) {
            etLotSizeValue.setError("Lot size is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateLivestockForm() {
        boolean isValid = true;

        if (etMunicipalLivestock != null && TextUtils.isEmpty(etMunicipalLivestock.getText())) {
            etMunicipalLivestock.setError("Municipal is required");
            isValid = false;
        }

        if (etStreetLivestock != null && TextUtils.isEmpty(etStreetLivestock.getText())) {
            etStreetLivestock.setError("Street is required");
            isValid = false;
        }

        if (spinnerLivestockType != null && spinnerLivestockType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a livestock type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerLivestockType != null && spinnerLivestockType.getSelectedItemPosition() == livestockOptions.length - 1) {
            if (etOtherLivestock != null && TextUtils.isEmpty(etOtherLivestock.getText())) {
                etOtherLivestock.setError("Please specify the livestock type");
                isValid = false;
            }
        }

        if (etAnimalCount != null && TextUtils.isEmpty(etAnimalCount.getText())) {
            etAnimalCount.setError("Animal count is required");
            isValid = false;
        }

        return isValid;
    }

    private boolean validateMixedForm() {
        boolean isValid = true;

        if (etMunicipalMixed != null && TextUtils.isEmpty(etMunicipalMixed.getText())) {
            etMunicipalMixed.setError("Municipal is required");
            isValid = false;
        }

        if (etStreetMixed != null && TextUtils.isEmpty(etStreetMixed.getText())) {
            etStreetMixed.setError("Street is required");
            isValid = false;
        }

        if (spinnerCropsGrownMixed != null && spinnerCropsGrownMixed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a crop", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerCropsGrownMixed != null && spinnerCropsGrownMixed.getSelectedItemPosition() == cropOptions.length - 1) {
            if (etOtherCropMixed != null && TextUtils.isEmpty(etOtherCropMixed.getText())) {
                etOtherCropMixed.setError("Please specify the crop");
                isValid = false;
            }
        }

        if (spinnerLivestockTypeMixed != null && spinnerLivestockTypeMixed.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a livestock type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (spinnerLivestockTypeMixed != null && spinnerLivestockTypeMixed.getSelectedItemPosition() == livestockOptions.length - 1) {
            if (etOtherLivestockMixed != null && TextUtils.isEmpty(etOtherLivestockMixed.getText())) {
                etOtherLivestockMixed.setError("Please specify the livestock type");
                isValid = false;
            }
        }

        if (etLotSizeValueMixed != null && TextUtils.isEmpty(etLotSizeValueMixed.getText())) {
            etLotSizeValueMixed.setError("Lot size is required");
            isValid = false;
        }

        if (etAnimalCountMixed != null && TextUtils.isEmpty(etAnimalCountMixed.getText())) {
            etAnimalCountMixed.setError("Animal count is required");
            isValid = false;
        }

        return isValid;
    }

    private void saveAllFarmerInformation() {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(false);
        }

        // First update basic farmer information
        updateBasicFarmerInformation();
    }

    private void updateBasicFarmerInformation() {
        Map<String, Object> farmerUpdates = new HashMap<>();

        // Update basic information if provided
        if (firstName != null && !firstName.isEmpty()) {
            farmerUpdates.put("firstName", firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            farmerUpdates.put("middleInitial", middleName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            farmerUpdates.put("lastName", lastName);
        }
        if (contactNumber != null && !contactNumber.isEmpty()) {
            farmerUpdates.put("contactNumber", contactNumber);
        }

        // Add timestamp
        farmerUpdates.put("lastUpdated", Timestamp.now());

        DocumentReference farmerRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId);

        farmerRef.update(farmerUpdates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Basic farmer information updated successfully");
                    // Now save farm type information
                    saveFarmTypeInformation();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating basic farmer information", e);
                    onSaveError(e);
                });
    }

    private void saveFarmTypeInformation() {
        if ("Crop".equals(currentFarmType)) {
            saveCropInformation();
        } else if ("Livestock".equals(currentFarmType)) {
            saveLivestockInformation();
        } else if ("Mixed".equals(currentFarmType)) {
            saveMixedInformation();
        }
    }

    private void saveCropInformation() {
        Map<String, Object> cropInfo = new HashMap<>();
        cropInfo.put("municipal", etMunicipalCrop.getText().toString().trim());
        cropInfo.put("street", etStreetCrop.getText().toString().trim());
        cropInfo.put("barangay", barangay);

        // Get crop type
        String cropType;
        if (spinnerCropsGrown.getSelectedItemPosition() == cropOptions.length - 1) {
            cropType = etOtherCrop.getText().toString().trim();
        } else {
            cropType = cropOptions[spinnerCropsGrown.getSelectedItemPosition()];
        }
        cropInfo.put("cropsGrown", cropType);

        cropInfo.put("lotSizeValue", etLotSizeValue.getText().toString().trim());
        cropInfo.put("lotSizeUnit", unitOptions[spinnerUnit.getSelectedItemPosition()]);

        // Create combined lot size
        String lotSize = etLotSizeValue.getText().toString().trim() + " " + unitOptions[spinnerUnit.getSelectedItemPosition()];
        cropInfo.put("lotSize", lotSize);

        cropInfo.put("lastUpdated", Timestamp.now());

        DocumentReference cropRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Crop");

        cropRef.set(cropInfo, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Crop information saved successfully");
                    // Delete other farm type documents if they exist
                    deleteOtherFarmTypes("Crop");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving crop information", e);
                    onSaveError(e);
                });
    }

    private void saveLivestockInformation() {
        Map<String, Object> livestockInfo = new HashMap<>();
        livestockInfo.put("municipal", etMunicipalLivestock.getText().toString().trim());
        livestockInfo.put("street", etStreetLivestock.getText().toString().trim());
        livestockInfo.put("barangay", barangay);

        // Get livestock type
        String livestockType;
        if (spinnerLivestockType.getSelectedItemPosition() == livestockOptions.length - 1) {
            livestockType = etOtherLivestock.getText().toString().trim();
        } else {
            livestockType = livestockOptions[spinnerLivestockType.getSelectedItemPosition()];
        }
        livestockInfo.put("livestockType", livestockType);

        String animalCountStr = etAnimalCount.getText().toString().trim();
        try {
            int animalCount = Integer.parseInt(animalCountStr);
            livestockInfo.put("livestockCount", animalCount);
        } catch (NumberFormatException e) {
            livestockInfo.put("livestockCount", animalCountStr);
        }

        livestockInfo.put("lastUpdated", Timestamp.now());

        DocumentReference livestockRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Livestock");

        livestockRef.set(livestockInfo, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Livestock information saved successfully");
                    // Delete other farm type documents if they exist
                    deleteOtherFarmTypes("Livestock");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving livestock information", e);
                    onSaveError(e);
                });
    }

    private void saveMixedInformation() {
        // Save crop information
        Map<String, Object> cropInfo = new HashMap<>();
        cropInfo.put("municipal", etMunicipalMixed.getText().toString().trim());
        cropInfo.put("street", etStreetMixed.getText().toString().trim());
        cropInfo.put("barangay", barangay);

        // Get crop type
        String cropType;
        if (spinnerCropsGrownMixed.getSelectedItemPosition() == cropOptions.length - 1) {
            cropType = etOtherCropMixed.getText().toString().trim();
        } else {
            cropType = cropOptions[spinnerCropsGrownMixed.getSelectedItemPosition()];
        }
        cropInfo.put("cropsGrown", cropType);

        cropInfo.put("lotSizeValue", etLotSizeValueMixed.getText().toString().trim());
        cropInfo.put("lotSizeUnit", unitOptions[spinnerUnitMixed.getSelectedItemPosition()]);

        // Create combined lot size
        String lotSize = etLotSizeValueMixed.getText().toString().trim() + " " + unitOptions[spinnerUnitMixed.getSelectedItemPosition()];
        cropInfo.put("lotSize", lotSize);

        cropInfo.put("lastUpdated", Timestamp.now());

        // Save livestock information
        Map<String, Object> livestockInfo = new HashMap<>();
        livestockInfo.put("municipal", etMunicipalMixed.getText().toString().trim());
        livestockInfo.put("street", etStreetMixed.getText().toString().trim());
        livestockInfo.put("barangay", barangay);

        // Get livestock type
        String livestockType;
        if (spinnerLivestockTypeMixed.getSelectedItemPosition() == livestockOptions.length - 1) {
            livestockType = etOtherLivestockMixed.getText().toString().trim();
        } else {
            livestockType = livestockOptions[spinnerLivestockTypeMixed.getSelectedItemPosition()];
        }
        livestockInfo.put("livestockType", livestockType);

        String animalCountStr = etAnimalCountMixed.getText().toString().trim();
        try {
            int animalCount = Integer.parseInt(animalCountStr);
            livestockInfo.put("livestockCount", animalCount);
        } catch (NumberFormatException e) {
            livestockInfo.put("livestockCount", animalCountStr);
        }

        livestockInfo.put("lastUpdated", Timestamp.now());

        // Save both documents
        DocumentReference cropRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Crop");

        DocumentReference livestockRef = db.collection("Barangays")
                .document(barangay)
                .collection("Farmers")
                .document(farmerDocumentId)
                .collection("FarmType")
                .document("Livestock");

        // Save crop data first
        cropRef.set(cropInfo, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Mixed crop information saved successfully");
                    // Save livestock data
                    livestockRef.set(livestockInfo, SetOptions.merge())
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Mixed livestock information saved successfully");
                                onSaveSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error saving mixed livestock information", e);
                                onSaveError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving mixed crop information", e);
                    onSaveError(e);
                });
    }

    private void deleteOtherFarmTypes(String keepType) {
        String[] farmTypes = {"Crop", "Livestock"};

        for (String farmType : farmTypes) {
            if (!farmType.equals(keepType)) {
                db.collection("Barangays")
                        .document(barangay)
                        .collection("Farmers")
                        .document(farmerDocumentId)
                        .collection("FarmType")
                        .document(farmType)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Deleted " + farmType + " farm type document");
                        })
                        .addOnFailureListener(e -> {
                            Log.w(TAG, "Could not delete " + farmType + " farm type document", e);
                        });
            }
        }

        // After cleanup, call success
        onSaveSuccess();
    }

    // FIXED: Return result instead of navigating to new activity
    private void onSaveSuccess() {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(true);
        }

        // Reset unsaved changes flag after successful save
        hasUnsavedChanges = false;

        // Log the farmer edit activity using ActivityLogger
        if (barangay != null && farmerName != null) {
            Log.d(TAG, "Logging farmer edit activity for: " + farmerName + " in " + barangay);
            ActivityLogger.logFarmerEdited(barangay, farmerName);
        } else {
            Log.w(TAG, "Cannot log activity - missing barangay or farmer name");
        }

        Toast.makeText(this, "Farm information updated successfully", Toast.LENGTH_SHORT).show();

        // FIXED: Return result instead of creating new intent
        Intent resultIntent = new Intent();
        resultIntent.putExtra("updated", true);
        resultIntent.putExtra("farmerId", farmerId);
        resultIntent.putExtra("farmerName", farmerName);
        resultIntent.putExtra("barangay", barangay);
        resultIntent.putExtra("farmerDocumentId", farmerDocumentId);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void onSaveError(Exception e) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
        if (btnSave != null) {
            btnSave.setEnabled(true);
        }

        Log.e(TAG, "Error saving farmer information", e);
        Toast.makeText(this, "Error saving information: " + e.getMessage(), Toast.LENGTH_LONG).show();

        new AlertDialog.Builder(this)
                .setTitle("Save Failed")
                .setMessage("Failed to save farmer information. Would you like to try again?")
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (validateForm()) {
                        saveAllFarmerInformation();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // FIXED: Handle back press properly
    private void handleBackPress() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    // FIXED: Updated unsaved changes dialog
    private void showUnsavedChangesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save them before leaving?")
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (validateForm()) {
                            saveAllFarmerInformation();
                        }
                    }
                })
                .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hasUnsavedChanges = false;
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handleBackPress();
    }
}
