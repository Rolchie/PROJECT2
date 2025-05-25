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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.maramagagriculturalaid.app.R;
import com.maramagagriculturalaid.app.SuccessActivities.SuccessedAddFarmer;
import com.maramagagriculturalaid.app.ActivityLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.List;

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
    private TextView tvMunicipalCrop, tvBarangayCrop;
    private EditText etStreetCrop, etLotSize, etOtherCrop;
    private Spinner spinnerUnit, spinnerCropsGrown;
    private LinearLayout layoutOtherCrop;

    // Livestock form fields
    private TextView tvMunicipalLivestock, tvBarangayLivestock;
    private EditText etStreetLivestock, etOtherLivestock, etLivestockCount;
    private Spinner spinnerLivestockType;
    private LinearLayout layoutOtherLivestock;

    // Mixed form fields
    private TextView tvMunicipalMixed, tvBarangayMixed;
    private EditText etStreetMixed, etOtherCropMixed, etLotSizeMixed;
    private EditText etOtherLivestockMixed, etLivestockCountMixed;
    private Spinner spinnerUnitMixed, spinnerCropsGrownMixed;
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

    // User's assigned barangay
    private String userBarangay;

    private static final String TAG = "FarmInfoActivity";

    // Predefined barangays in Maramag Municipality
    private final String[] availableBarangays = {
            "Anahawon", "Base Camp", "Bayabason", "Camp 1", "Colambugon",
            "Dagumba-an", "Danggawan", "Dologon", "Kisanday", "Kuya",
            "La Roxas", "Panadtalan", "Panalsalan", "North Poblacion", "South Poblacion",
            "San Miguel", "San Roque", "Tubigon", "Kiharong", "Bagongsilang"
    };

    // Unit measurement options
    private final String[] unitOptions = {"Square Meter(sqm)", "Hectares(ha)", "Are", "Square Feet(Sq ft)"};

    // Crops options
    private final String[] cropsOptions = {
            "Select Crop Type",
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

        // Initialize predefined barangays in database first
        initializePredefinedBarangays();

        // Get user's barangay first, then setup the rest
        getCurrentUserBarangay();
    }

    private void initializePredefinedBarangays() {
        Log.d(TAG, "Initializing predefined barangays in database");

        Timestamp now = Timestamp.now();
        WriteBatch batch = db.batch();

        for (String barangayName : availableBarangays) {
            DocumentReference barangayRef = db.collection("Barangays").document(barangayName);
            Map<String, Object> barangayData = new HashMap<>();
            barangayData.put("name", barangayName);
            barangayData.put("municipal", "Maramag");
            barangayData.put("province", "Bukidnon");
            barangayData.put("region", "Northern Mindanao");
            barangayData.put("isActive", true);
            barangayData.put("createdAt", now);
            barangayData.put("lastUpdated", now);

            // Use merge to avoid overwriting existing data
            batch.set(barangayRef, barangayData, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully initialized predefined barangays");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error initializing predefined barangays", e);
                    // Continue anyway, this is not critical for the main functionality
                });
    }

    private void getCurrentUserBarangay() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            showLoading(true);
            Log.d(TAG, "Getting barangay for user: " + currentUser.getUid());

            db.collection("Users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(userDocument -> {
                        showLoading(false);
                        if (userDocument.exists()) {
                            Log.d(TAG, "User document exists. Data: " + userDocument.getData());

                            // Try different field names that might contain barangay info
                            userBarangay = userDocument.getString("Barangay");
                            if (userBarangay == null || userBarangay.isEmpty()) {
                                userBarangay = userDocument.getString("barangay");
                            }
                            if (userBarangay == null || userBarangay.isEmpty()) {
                                userBarangay = userDocument.getString("assignedBarangay");
                            }
                            if (userBarangay == null || userBarangay.isEmpty()) {
                                userBarangay = userDocument.getString("location");
                            }

                            if (userBarangay != null && !userBarangay.isEmpty()) {
                                Log.d(TAG, "User's assigned barangay: " + userBarangay);

                                // Validate if the barangay exists in our predefined list
                                boolean isValidBarangay = false;
                                for (String barangay : availableBarangays) {
                                    if (barangay.equalsIgnoreCase(userBarangay)) {
                                        userBarangay = barangay; // Use the correct case
                                        isValidBarangay = true;
                                        break;
                                    }
                                }

                                if (isValidBarangay) {
                                    // Set the barangay in all forms
                                    setUserBarangayInForms();

                                    // Now setup the rest of the UI
                                    setupFarmTypeSelection();
                                    setupClickListeners();
                                    setupUnitSpinner();
                                    setupCropsSpinner();
                                    setupLivestockSpinner();
                                    setDefaultMunicipalValues();
                                } else {
                                    Log.w(TAG, "Invalid barangay: " + userBarangay);
                                    showBarangaySelectionDialog();
                                }
                            } else {
                                Log.w(TAG, "No barangay found in user document");
                                showBarangaySelectionDialog();
                            }
                        } else {
                            Log.e(TAG, "User document does not exist");
                            Toast.makeText(this, "Error: User data not found. Please contact support.", Toast.LENGTH_LONG).show();
                            showBarangaySelectionDialog(); // Still allow user to select barangay
                        }
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error getting user barangay", e);
                        Toast.makeText(this, "Error: Failed to get user information. Please check your connection.", Toast.LENGTH_LONG).show();
                        showBarangaySelectionDialog(); // Allow user to select barangay as fallback
                    });
        } else {
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showBarangaySelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Your Barangay");
        builder.setMessage("Please select your assigned barangay to continue:");

        builder.setItems(availableBarangays, (dialog, which) -> {
            userBarangay = availableBarangays[which];
            Log.d(TAG, "User selected barangay: " + userBarangay);

            // Update user's barangay in Firestore
            updateUserBarangay(userBarangay);

            // Set the barangay in all forms
            setUserBarangayInForms();

            // Setup the rest of the UI
            setupFarmTypeSelection();
            setupClickListeners();
            setupUnitSpinner();
            setupCropsSpinner();
            setupLivestockSpinner();
            setDefaultMunicipalValues();

            dialog.dismiss();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void updateUserBarangay(String barangay) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("Barangay", barangay);
            updates.put("barangay", barangay); // Add both field names for compatibility
            updates.put("municipal", "Maramag");
            updates.put("province", "Bukidnon");
            updates.put("lastUpdated", Timestamp.now());

            db.collection("Users")
                    .document(currentUser.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User barangay updated successfully");
                        Toast.makeText(this, "Barangay updated: " + barangay, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating user barangay", e);
                        Toast.makeText(this, "Warning: Could not save barangay preference", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setUserBarangayInForms() {
        // Set the user's barangay in all form TextViews
        if (tvBarangayCrop != null) {
            tvBarangayCrop.setText(userBarangay);
            Log.d(TAG, "Set crop form barangay to: " + userBarangay);
        }
        if (tvBarangayLivestock != null) {
            tvBarangayLivestock.setText(userBarangay);
            Log.d(TAG, "Set livestock form barangay to: " + userBarangay);
        }
        if (tvBarangayMixed != null) {
            tvBarangayMixed.setText(userBarangay);
            Log.d(TAG, "Set mixed form barangay to: " + userBarangay);
        }
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
        tvBarangayCrop = findViewById(R.id.tv_barangay_crop);
        etStreetCrop = findViewById(R.id.et_street_crop);
        etLotSize = findViewById(R.id.et_lot_size);
        spinnerUnit = findViewById(R.id.spinner_unit);
        spinnerCropsGrown = findViewById(R.id.spinner_crops_grown);
        etOtherCrop = findViewById(R.id.et_other_crop);
        layoutOtherCrop = findViewById(R.id.layout_other_crop);

        // Initialize Livestock form views
        tvMunicipalLivestock = findViewById(R.id.tv_municipal_livestock);
        tvBarangayLivestock = findViewById(R.id.tv_barangay_livestock);
        etStreetLivestock = findViewById(R.id.et_street_livestock);
        spinnerLivestockType = findViewById(R.id.spinner_livestock_type);
        etOtherLivestock = findViewById(R.id.et_other_livestock);
        layoutOtherLivestock = findViewById(R.id.layout_other_livestock);
        etLivestockCount = findViewById(R.id.et_animal_count);

        // Initialize Mixed form views
        tvMunicipalMixed = findViewById(R.id.tv_municipal_mixed);
        tvBarangayMixed = findViewById(R.id.tv_barangay_mixed);
        etStreetMixed = findViewById(R.id.et_street_mixed);
        spinnerCropsGrownMixed = findViewById(R.id.spinner_crops_grown_mixed);
        etOtherCropMixed = findViewById(R.id.et_other_crop_mixed);
        layoutOtherCropMixed = findViewById(R.id.layout_other_crop_mixed);
        etLotSizeMixed = findViewById(R.id.et_lot_size_mixed);
        spinnerUnitMixed = findViewById(R.id.spinner_unit_mixed);
        spinnerLivestockTypeMixed = findViewById(R.id.spinner_livestock_type_mixed);
        etOtherLivestockMixed = findViewById(R.id.et_other_livestock_mixed);
        layoutOtherLivestockMixed = findViewById(R.id.layout_other_livestock_mixed);
        etLivestockCountMixed = findViewById(R.id.et_animal_count_mixed);

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

    private void setupUnitSpinner() {
        // Create adapter for unit measurement spinner
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                unitOptions
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);
        spinnerUnit.setSelection(0);

        // Setup for Mixed form
        ArrayAdapter<String> unitAdapterMixed = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                unitOptions
        );
        unitAdapterMixed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnitMixed.setAdapter(unitAdapterMixed);
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
        cropFarmData.clear();

        cropFarmData.put("farmType", "Crop");
        cropFarmData.put("municipal", "Maramag");
        cropFarmData.put("barangay", userBarangay);
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
        livestockFarmData.clear();

        livestockFarmData.put("farmType", "Livestock");
        livestockFarmData.put("municipal", "Maramag");
        livestockFarmData.put("barangay", userBarangay);
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
        mixedFarmData.clear();

        mixedFarmData.put("farmType", "Mixed");
        mixedFarmData.put("municipal", "Maramag");
        mixedFarmData.put("barangay", userBarangay);
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

        if (userBarangay == null || userBarangay.isEmpty()) {
            Toast.makeText(this, "Error: User barangay not available", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Save failed: User barangay is empty");
            return;
        }

        try {
            Log.d(TAG, "Saving data for farmer ID: " + farmerId);
            Log.d(TAG, "User's barangay: " + userBarangay);

            showLoading(true);
            Timestamp now = Timestamp.now();
            farmerData.put("createdAt", now);
            farmerData.put("lastUpdated", now);
            farmerData.put("barangay", userBarangay);
            farmerData.put("municipal", "Maramag");

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

            // 1. Ensure barangay document exists with complete information
            DocumentReference barangayRef = db.collection("Barangays").document(userBarangay);
            Map<String, Object> barangayData = new HashMap<>();
            barangayData.put("name", userBarangay);
            barangayData.put("municipal", "Maramag");
            barangayData.put("province", "Bukidnon");
            barangayData.put("region", "Northern Mindanao");
            barangayData.put("isActive", true);
            barangayData.put("lastUpdated", now);
            batch.set(barangayRef, barangayData, SetOptions.merge());

            // 2. Save farmer under user's Barangay's Farmers subcollection
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
            farmData.put("farmerName", farmerDocId);

            DocumentReference farmTypeRef = barangayFarmerRef.collection("FarmType").document(farmTypeDocName);
            batch.set(farmTypeRef, farmData);

            // 4. Update notifications
            DocumentReference notificationRef = db.collection("Notifications").document("new_farmers");
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("lastUpdated", now);
            notificationData.put("message", "New farmer added to " + userBarangay + ": " + farmerDocId);
            batch.set(notificationRef, notificationData, SetOptions.merge());

            // Commit the batch
            batch.commit()
                    .addOnCompleteListener(task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Batch commit succeeded!");

                            String farmerFullName = (String) farmerData.get("fullName");
                            Log.d(TAG, "Logging farmer addition: " + farmerFullName + " to " + userBarangay);
                            ActivityLogger.logFarmerAdded(userBarangay, farmerFullName);

                            Toast.makeText(FarmInfoActivity.this, "Farmer saved successfully to " + userBarangay + "!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(FarmInfoActivity.this, SuccessedAddFarmer.class);
                            intent.putExtra("success_message", "Farmer's information saved successfully to " + userBarangay + "!");
                            intent.putExtra("farmer_name", farmerFullName);
                            intent.putExtra("barangay", userBarangay);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e(TAG, "Batch commit failed", task.getException());
                            new AlertDialog.Builder(FarmInfoActivity.this)
                                    .setTitle("Database Error")
                                    .setMessage("Error saving data to " + userBarangay + ": " + (task.getException() != null ?
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